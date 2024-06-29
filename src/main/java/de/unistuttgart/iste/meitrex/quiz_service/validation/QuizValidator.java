package de.unistuttgart.iste.meitrex.quiz_service.validation;

import de.unistuttgart.iste.meitrex.generated.dto.*;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.QuizEntity;
import jakarta.validation.ValidationException;
import org.springframework.stereotype.Component;

import java.util.List;

import static de.unistuttgart.iste.meitrex.generated.dto.ClozeElementType.BLANK;
import static de.unistuttgart.iste.meitrex.generated.dto.ClozeElementType.TEXT;

@Component
public class QuizValidator {
    public void validateCreateQuizInput(final CreateQuizInput input) {
        // no validation needed
    }

    public void validateCreateMultipleChoiceQuestionInput(final CreateMultipleChoiceQuestionInput input) {
        validateAtLeastOneAnswerCorrect(input.getAnswers());
    }

    public void validateUpdateMultipleChoiceQuestionInput(final UpdateMultipleChoiceQuestionInput input) {
        validateAtLeastOneAnswerCorrect(input.getAnswers());
    }

    private void validateAtLeastOneAnswerCorrect(final List<MultipleChoiceAnswerInput> answers) {
        if (answers.stream().noneMatch(MultipleChoiceAnswerInput::getCorrect)) {
            throw new ValidationException("At least one answer must be correct");
        }
    }

    public void validateCreateClozeQuestionInput(final CreateClozeQuestionInput input) {
        validateClozeElements(input.getClozeElements());
    }

    public void validateUpdateClozeQuestionInput(final UpdateClozeQuestionInput input) {
        validateClozeElements(input.getClozeElements());
    }

    private void validateClozeElements(final List<ClozeElementInput> clozeElements) {
        if (clozeElements.stream().map(ClozeElementInput::getType).noneMatch(BLANK::equals)) {
            throw new ValidationException("Cloze quiz most contain at least one blank");
        }
        clozeElements.forEach(this::validateClozeElement);
    }

    private void validateClozeElement(final ClozeElementInput clozeElement) {
        if (clozeElement.getType() == BLANK) {
            if (clozeElement.getCorrectAnswer() == null) {
                throw new ValidationException("correct answer is required for cloze blank elements");
            }
            if (clozeElement.getText() != null) {
                throw new ValidationException("text is not allowed for cloze blank elements");
            }
        }
        if (clozeElement.getType() == TEXT) {
            if (clozeElement.getCorrectAnswer() != null) {
                throw new ValidationException("correct answer is not allowed for text elements");
            }
            if (clozeElement.getText() == null) {
                throw new ValidationException("text is required for cloze text elements");
            }
            if (clozeElement.getFeedback() != null) {
                throw new ValidationException("text elements cannot have feedback");
            }
        }
    }

    public void validateCreateAssociationQuestionInput(final CreateAssociationQuestionInput input) {
        validateSidesUnique(input.getCorrectAssociations());
    }

    public void validateUpdateAssociationQuestionInput(final UpdateAssociationQuestionInput input) {
        validateSidesUnique(input.getCorrectAssociations());
    }

    private void validateSidesUnique(final List<AssociationInput> associationInputs) {
        final List<String> leftSide = associationInputs.stream().map(AssociationInput::getLeft).toList();
        final List<String> rightSide = associationInputs.stream().map(AssociationInput::getRight).toList();

        final long leftSideDistinctCount = leftSide.stream().distinct().count();
        final long rightSideDistinctCount = rightSide.stream().distinct().count();

        if (leftSide.size() != leftSideDistinctCount
            || rightSide.size() != rightSideDistinctCount) {
            throw new ValidationException("Each side of the associations must only contain unique values");
        }
    }

    /**
     * Checks if the question number is unique in the quiz
     *
     * @param quizEntity     the quiz entity
     * @param questionNumber the question number to check
     * @throws ValidationException if the question number is not unique
     */
    public void checkNumberIsUnique(final QuizEntity quizEntity, final int questionNumber) {
        if (quizEntity.getQuestionPool().stream().anyMatch(q -> q.getNumber() == questionNumber)) {
            throw new ValidationException("Question number must be unique, but the number "
                                          + questionNumber
                                          + " is already used.");
        }
    }
}
