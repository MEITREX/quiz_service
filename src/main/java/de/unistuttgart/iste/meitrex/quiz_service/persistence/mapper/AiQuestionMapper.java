package de.unistuttgart.iste.meitrex.quiz_service.persistence.mapper;

import de.unistuttgart.iste.meitrex.generated.dto.MultipleChoiceAnswer;
import de.unistuttgart.iste.meitrex.generated.dto.QuestionType;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.*;
import de.unistuttgart.iste.meitrex.quiz_service.service.model.PromptJson;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class AiQuestionMapper {


    public List<QuestionEntity> map(PromptJson.Question[] questions) {
        return Stream.of(questions)
                .map(this::mapQuestion)
                .filter(Objects::nonNull)
                .toList();
    }

    private QuestionEntity mapQuestion(PromptJson.Question question) {
        if(question.isNumeric()){
            return question.getAsNumeric().map(numeric -> mapNumericQuestion(question, numeric)).orElse(null);
        }
        if(question.isFreeText()){
            return question.getAsFreeText().map(freeText -> mapFreeText(question, freeText)).orElse(null);
        }
        if(question.isMultipleChoice()){
            return question.getAsMultipleChoice().map(multipleChoice -> mapMultipleChoice(question, multipleChoice)).orElse(null);
        }
        if(question.isExactAnswer()){
            return question.getAsExactAnswer().map(exactAnswer -> mapExactAnswer(question, exactAnswer)).orElse(null);
        }
        return null;
    }

    private QuestionEntity mapNumericQuestion(PromptJson.Question question, PromptJson.Numeric numeric) {
        NumericQuestionEntity eq = new NumericQuestionEntity();
        eq.setText(question.getQuestion());
        eq.setType(QuestionType.NUMERIC);
        eq.setTolerance(numeric.getMaxDifference());
        eq.setCorrectAnswer(numeric.getAnswer());
        return eq;
    }

    private QuestionEntity mapFreeText(PromptJson.Question question, PromptJson.FreeText freeText) {
        ExactAnswerQuestionEntity eq = new ExactAnswerQuestionEntity();
        eq.setText(question.getQuestion());
        eq.setType(QuestionType.EXACT_ANSWER);
        eq.setCaseSensitive(false);
        eq.setCorrectAnswers(List.of(freeText.getAnswer()));
        return eq;
    }

    private QuestionEntity mapMultipleChoice(PromptJson.Question question, PromptJson.MultipleChoice multipleChoice) {
        MultipleChoiceQuestionEntity multipleChoiceQuestionEntity = new MultipleChoiceQuestionEntity();
        multipleChoiceQuestionEntity.setText(question.getQuestion());
        List<MultipleChoiceAnswerEmbeddable> answers = Arrays.stream(multipleChoice.getOptions()).map(this::createMultipleChoiceAnswer).toList();
        multipleChoiceQuestionEntity.setAnswers(answers);
        multipleChoiceQuestionEntity.setType(QuestionType.MULTIPLE_CHOICE);
        return multipleChoiceQuestionEntity;
    }

    private MultipleChoiceAnswerEmbeddable createMultipleChoiceAnswer(PromptJson.MultipleChoice.Option option){
        MultipleChoiceAnswerEmbeddable answer = new MultipleChoiceAnswerEmbeddable();
        answer.setAnswerText(option.getText());
        answer.setCorrect(option.isCorrect());
        return answer;
    }

    private QuestionEntity mapExactAnswer(PromptJson.Question question, PromptJson.ExactAnswer exactAnswer) {
        ExactAnswerQuestionEntity eq = new ExactAnswerQuestionEntity();
        eq.setText(question.getQuestion());
        eq.setCaseSensitive(exactAnswer.isCaseSensitive());
        eq.setType(QuestionType.EXACT_ANSWER);
        eq.setCorrectAnswers(List.of(exactAnswer.getAnswer()));
        return eq;
    }

}
