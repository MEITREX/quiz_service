package de.unistuttgart.iste.meitrex.quiz_service.persistence.mapper;

import de.unistuttgart.iste.meitrex.generated.dto.MultipleChoiceAnswer;
import de.unistuttgart.iste.meitrex.generated.dto.QuestionType;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.*;
import de.unistuttgart.iste.meitrex.quiz_service.service.model.PromptJson;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class AiQuestionMapper {


    /**
     * maps the questions from the prompt json to question entities
     * @param questions
     * @return
     */
    public List<QuestionEntity> map(PromptJson.Questions questions) {

        List<QuestionEntity> res = new LinkedList<>();
        if (questions == null) {
            return res;
        }

        if (questions.getNumericQuestions() != null) {
            questions.getNumericQuestions().forEach(numeric -> {
                QuestionEntity question = mapNumericQuestion(numeric);
                    res.add(question);
            });
        }

        if (questions.getFreeTextQuestions() != null) {
            questions.getFreeTextQuestions().forEach(freeText -> {
                QuestionEntity question = mapFreeText(freeText);
                res.add(question);
            });
        }

        if (questions.getMultipleChoiceQuestions() != null) {
            questions.getMultipleChoiceQuestions().forEach(multipleChoice -> {
                QuestionEntity question = mapMultipleChoice(multipleChoice);
                res.add(question);
            });
        }

        if (questions.getExactAnswerQuestions() != null) {
            questions.getExactAnswerQuestions().forEach(exactAnswer -> {
                QuestionEntity question = mapExactAnswer(exactAnswer);
                res.add(question);
            });
        }

        return res;
    }


    private QuestionEntity mapNumericQuestion(PromptJson.Numeric numeric) {
        NumericQuestionEntity eq = new NumericQuestionEntity();
        final String textJson = toJsonObjectString(numeric.getQuestion());
        eq.setText(textJson);
        eq.setType(QuestionType.NUMERIC);
        eq.setTolerance(numeric.getMaxDifference());
        eq.setCorrectAnswer(numeric.getAnswer());
        return eq;
    }

    /**
     * required for the frontend to display the question properly
     * @param text the question text
     * @return the question text as a json object string
     */
    private String toJsonObjectString(String text) {
        return "[{\"type\":\"paragraph\",\"children\":[{\"text\":\"" + text + "\"}]}]";
    }

    private QuestionEntity mapFreeText(PromptJson.FreeText freeText) {
        ExactAnswerQuestionEntity eq = new ExactAnswerQuestionEntity();
        String textJson = toJsonObjectString(freeText.getQuestion());
        eq.setText(textJson);
        eq.setType(QuestionType.EXACT_ANSWER);
        eq.setCaseSensitive(false);
        eq.setCorrectAnswers(List.of(freeText.getAnswer()));
        return eq;
    }

    private MultipleChoiceAnswerEmbeddable mapChoiceAnswer(PromptJson.MultipleChoice.Option option) {
        MultipleChoiceAnswerEmbeddable answer = new MultipleChoiceAnswerEmbeddable();
        String answerJson = toJsonObjectString(option.getText());
        answer.setAnswerText(answerJson);
        answer.setCorrect(option.isCorrect());
        return answer;
    }

    private QuestionEntity mapMultipleChoice(PromptJson.MultipleChoice multipleChoice) {
        MultipleChoiceQuestionEntity multipleChoiceQuestionEntity = new MultipleChoiceQuestionEntity();
        String textJson = toJsonObjectString(multipleChoice.getQuestion());
        multipleChoiceQuestionEntity.setText(textJson);
        List<MultipleChoiceAnswerEmbeddable> answers = multipleChoice.getOptions().stream().map(this::mapChoiceAnswer).toList();
        multipleChoiceQuestionEntity.setAnswers(answers);
        multipleChoiceQuestionEntity.setType(QuestionType.MULTIPLE_CHOICE);
        return multipleChoiceQuestionEntity;
    }



    private QuestionEntity mapExactAnswer(PromptJson.ExactAnswer exactAnswer) {
        ExactAnswerQuestionEntity eq = new ExactAnswerQuestionEntity();
        final String textJson = toJsonObjectString(exactAnswer.getQuestion());
        eq.setText(textJson);
        eq.setCaseSensitive(exactAnswer.isCaseSensitive());
        eq.setType(QuestionType.EXACT_ANSWER);
        eq.setCorrectAnswers(List.of(exactAnswer.getAnswer()));
        return eq;
    }

}
