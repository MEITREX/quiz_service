package de.unistuttgart.iste.meitrex.quiz_service.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.iste.meitrex.generated.dto.Quiz;
import de.unistuttgart.iste.meitrex.quiz_service.config.QuizGenConfig;
import de.unistuttgart.iste.meitrex.quiz_service.event.EventPublisher;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.QuestionEntity;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.mapper.AiQuestionMapper;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.mapper.QuizMapper;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.repository.QuizRepository;
import de.unistuttgart.iste.meitrex.quiz_service.service.model.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.QuizEntity;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AiQuizGenerationService {

    // used to keep the quiz generation prompt template in memory to reduce the number of disk accesses
    // and to speed up the quiz generation process
    private String promptTemplateCache = "";


    private final OllamaService ollamaService;

    private final DocProcAiService docProcAiService;

    private final QuizMapper quizMapper;

    private final AiQuestionMapper questionMapper;

    private final QuizRepository quizRepository;

    private final QuizGenConfig quizGenConfig;

    private final EventPublisher eventPublisher;
    
    private final ObjectMapper jsonMapper = new ObjectMapper();

    private String getTemplatePath() {
        return this.getClass().getResource("/prompt_templates/quiz_gen_template.txt").getPath();
    }

    private String getTemplate() {
        if (promptTemplateCache.isEmpty()) {
            try {
                // read the template from the file
                reloadTemplate();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return promptTemplateCache;
    }

    /**
     * Reloads the quiz generation prompt template from the file.
     *
     * @return the quiz generation prompt template
     * @throws IOException 
     */
    public String reloadTemplate() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(getTemplatePath()));
        StringBuilder template = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            template.append(line).append("\n");
        }
        // write result to cache
        promptTemplateCache = template.toString();
        reader.close();
        return promptTemplateCache;
    }


    /**
     * this methods will generate questions for a quiz and adds them to the quiz entity.
     * @param quizEntity the quiz entity to fill with questions
     * @param limits the limits for the quiz generation
     * @param description the description of the quiz to fine tune the quiz generation
     * @param mediaRecordIds the media record ids to use as resources for the quiz generation
     * @return
     */
    public Quiz fillQuizWithQuestions(QuizEntity quizEntity, AiQuizGenLimits limits, String description, List<String> mediaRecordIds) {
        final QuizEntity fillQuiz = fillQuiz(quizEntity, limits, description, mediaRecordIds);
        return quizMapper.entityToDto(fillQuiz);
    }

    /**
     * fills a quiz entity
     *
     * usably for internal or chained operations on the quiz entity
     * @param quizEntity
     * @param limits
     * @param description
     * @param mediaRecordIds
     * @return
     */
    protected QuizEntity fillQuiz(QuizEntity quizEntity, AiQuizGenLimits limits, String description, List<String> mediaRecordIds) {
        List<QuestionEntity> questions = generateQuizQuestions(limits, description, mediaRecordIds);
        // refresh quiz entity to ensure it is up to date
        QuizEntity qe = quizRepository.findById(quizEntity.getAssessmentId()).orElse(null);
        if (qe == null) {
            throw new IllegalArgumentException("Quiz entity not found with id: " + quizEntity.getAssessmentId());
        }
        quizEntity = qe;

        if (questions.isEmpty()) {
            return quizEntity; // or throw an exception, depending on your error handling strategy
        }
        // get the first question number to set the quiz number
        int lastQuestionNumber = quizEntity.getQuestionPool().stream().map(QuestionEntity::getNumber).max(Integer::compareTo).orElse(0);
        int nextQuestionNumber = lastQuestionNumber + 1;
        for (QuestionEntity question : questions) {
            question.setNumber(nextQuestionNumber++);
            question.setItemId(UUID.randomUUID()); // requires ID to set manually, as it is not generated by the database
            question.setAiGenerated(true);
        }
        quizEntity.getQuestionPool().addAll(questions);

        QuizEntity savedEntity = quizRepository.save(quizEntity);
        // publish the quiz change event, block to ensure that the event is actually send,
        // to ensure that the event is actually send, since it is system critical
        eventPublisher.publishUpdateQuizEvent(savedEntity).block();
        return savedEntity;
    }


    private Map<String, Object> buildJsonSchema(AiQuizGenLimits limits){
        JsonSchema jsonSchema = new JsonSchema();
        JsonSchema.SchemaArgs schemaArgs = new JsonSchema.SchemaArgs();
        schemaArgs.setMinExactAnswerQuestions(Optional.of(limits.getMinExactQuestions()));
        schemaArgs.setMaxExactAnswerQuestions(Optional.of(limits.getMaxExactQuestions()));
        schemaArgs.setMinFreeTextQuestions(Optional.of(limits.getMinFreeTextQuestions()));
        schemaArgs.setMaxFreeTextQuestions(Optional.of(limits.getMaxFreeTextQuestions()));
        schemaArgs.setMinMultipleChoicesQuestions(Optional.of(limits.getMinMultipleChoiceQuestions()));
        schemaArgs.setMaxMultipleChoicesQuestions(Optional.of(limits.getMaxMultipleChoiceQuestions()));
        schemaArgs.setMinNumericQuestions(Optional.of(limits.getMinNumericQuestions()));
        schemaArgs.setMaxNumericQuestions(Optional.of(limits.getMaxNumericQuestions()));
        JsonNode json = jsonSchema.buildSchema(schemaArgs);
        return jsonMapper.convertValue(json, Map.class);
    }

    /**
     * generates quiz questions based on the given limits, topic, description and media record ids. It will not add the questions to a quiz entity.
     * @param limits
     * @param description
     * @param mediaRecordIds
     * @return
     */
    public List<QuestionEntity> generateQuizQuestions(AiQuizGenLimits limits, String description, List<String> mediaRecordIds){
        String prompt = buildPrompt(limits, description, mediaRecordIds);
        Map<String, Object> jsonSchema = buildJsonSchema(limits);
        OllamaRequest request = new OllamaRequest(quizGenConfig.getModel(), prompt, false, jsonSchema);
        try {
            OllamaResponse response = ollamaService.queryLLM(request);
            return generateQuestionsFromAiResponse(response).orElse(List.of());
        } catch (IOException | InterruptedException e) {
            return List.of();
        }
    }


    protected Optional<List<QuestionEntity>> generateQuestionsFromAiResponse(OllamaResponse aiResponse) {
        Optional<PromptJson> promptJson = ollamaService.parseResponse(aiResponse, PromptJson.class);
        if (promptJson.isEmpty()) {
            return Optional.empty();
        }
        List<QuestionEntity> questions = questionMapper.map(promptJson.get().getQuiz().getQuestions());
        return Optional.of(questions);
    }

    protected List<String> loadResources(final List<String> mediaRecordIds) {
        return mediaRecordIds
                .stream()
                .map(UUID::fromString)
                .map(docProcAiService::getSummaryByDocId)
                .map(r -> r.blockOptional(Duration.of(10, ChronoUnit.SECONDS)))
                .map(r -> r.orElse(""))
                .toList();

    }

    /**
     *  builds the limits for the quiz generation as prompt strings
     * @param limits
     * @return
     */
    protected List<String> buildLimits(AiQuizGenLimits limits) {
        List<String> limitsList = new LinkedList<>();
        limitsList.add("the maximum number of questions is: " + limits.getMaxQuestions());
        limitsList.add("the minimum number of questions is: " + limits.getMinQuestions());
        limitsList.add("the maximum number of answers per question is: " + limits.getMaxAnswersPerQuestion());
        limitsList.add("the maximum number of multiple choice questions is: " + limits.getMaxMultipleChoiceQuestions());
        limitsList.add("the maximum number of free text questions is: " + limits.getMaxFreeTextQuestions());
        limitsList.add("the maximum number of numeric questions is: " + limits.getMaxNumericQuestions());
        limitsList.add("the maximum number of exact answer questions is: " + limits.getMaxExactQuestions());
        limitsList.add("are multiple choice questions with multiple correct answers allowed: " + limits.isAllowMultipleCorrectAnswers());
        return limitsList;
    }

    /**
     * builds the arguments for the quiz generation
     * @param limits
     * @param description
     * @param mediaRecordTags
     * @return
     */
    protected AiQuizArgs buildArgs(AiQuizGenLimits limits, String description, List<String> mediaRecordTags) {
        AiQuizArgs args = new AiQuizArgs();
        args.setDescription(description);
        args.setResources(loadResources(mediaRecordTags));
        args.setLimitations(buildLimits(limits));
        return args;
    }


    /**
     * builds the prompt for the quiz generation
     * @param limits
     * @param description
     * @param mediaRecordTags
     * @return
     */
    protected String buildPrompt(AiQuizGenLimits limits, String description, List<String> mediaRecordTags) {
        String systemPrompt = getTemplate();
        return fillTemplate(systemPrompt, buildArgs(limits, description, mediaRecordTags));
    }

    /**
     * populates the template with the given arguments
     * @param template
     * @param args
     * @return
     */
    protected String fillTemplate(final String template, final AiQuizArgs args){
        String filledTemplate = template;
        filledTemplate = filledTemplate.replace("{{description}}", args.getDescription());
        filledTemplate = filledTemplate.replace("{{resources}}", String.join(" \n", args.getResources()));
        filledTemplate = filledTemplate.replace("{{limitations}}", String.join(" \n", args.getLimitations()));
        return filledTemplate;
    }

    

    

}
