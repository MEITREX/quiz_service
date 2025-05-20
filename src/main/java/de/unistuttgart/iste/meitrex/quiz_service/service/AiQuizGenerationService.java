package de.unistuttgart.iste.meitrex.quiz_service.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import de.unistuttgart.iste.meitrex.generated.dto.Quiz;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.QuestionEntity;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.mapper.AiQuestionMapper;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.mapper.QuizMapper;
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

    // TODO replace with an application properties entry
    private final String model = "mistral-nemo";


    private final OllamaService ollamaService;

    private final DocProcAiService docProcAiService;

    private final QuizMapper quizMapper;

    private final AiQuestionMapper questionMapper;


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
     * Generates a quiz based on the given topic and media record tags.
     *
     * @param topic the topic for the quiz
     * @param description the description of the quiz, it is used to fine tune the quiz generation
     * @param mediaRecordIds the ids of the media records to be used in the quiz generation
     * @return a QuizEntity object containing the generated quiz or empty optional if the generation failed
     */
    public Optional<Quiz> generateQuiz(AiQuizGenLimits limits, String topic, String description, List<String> mediaRecordIds) {
        String prompt = buildPrompt(limits, topic, description, mediaRecordIds);
        OllamaRequest request = new OllamaRequest(model, prompt, false);
        try {
            OllamaResponse response = ollamaService.queryLLM(request);
            return createFromAiResponse(response);
        } catch (IOException | InterruptedException e) {
            return Optional.empty();
        }
    }

    /**
     * creates a quiz from an AI response, to generate a quiz from parameters use the generateQuiz method instead.
     * @param aiResponse the AI response to generate the quiz from
     * @return an optional quiz object
     */
    public Optional<Quiz> createFromAiResponse(OllamaResponse aiResponse){
        Optional<PromptJson> promptJson = ollamaService.parseResponse(aiResponse, PromptJson.class);
        if (promptJson.isEmpty()) {
            return Optional.empty();
        }
        QuizEntity quizEntity = new QuizEntity();
        List<QuestionEntity> questions = questionMapper.map(promptJson.get().getQuiz().getQuestions());
        quizEntity.setQuestionPool(questions);
        return Optional.of(quizEntity).map(quizMapper::entityToDto);
    }



    protected List<String> loadResources(final String topic, final List<String> mediaRecordIds) {
        return mediaRecordIds
                .stream()
                .map(UUID::fromString)
                .map(docProcAiService::getSummaryByDocId)
                        .collect(Collectors.toUnmodifiableList());

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
        return limitsList;
    }

    /**
     * builds the arguments for the quiz generation
     * @param limits
     * @param topic
     * @param description
     * @param mediaRecordTags
     * @return
     */
    protected AiQuizArgs buildArgs(AiQuizGenLimits limits, String topic, String description, List<String> mediaRecordTags) {
        AiQuizArgs args = new AiQuizArgs();
        args.setDescription(description);
        args.setResources(loadResources(topic, mediaRecordTags));
        args.setLimitations(buildLimits(limits));
        return args;
    }


    /**
     * builds the prompt for the quiz generation
     * @param limits
     * @param topic
     * @param description
     * @param mediaRecordTags
     * @return
     */
    protected String buildPrompt(AiQuizGenLimits limits, String topic, String description, List<String> mediaRecordTags) {
        String systemPrompt = getTemplate();
        return fillTemplate(systemPrompt, buildArgs(limits, topic, description, mediaRecordTags));        
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
