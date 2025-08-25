package de.unistuttgart.iste.meitrex.quiz_service.event.type.publish;

import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.QuizEntity;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

/**
 * Event to update a quiz.
 * This event is fired when a quiz is updated.
 * <p>
 * When updating this class the event structure changes and other services depends on it.
 * Only make changes that are compatible. Adding fields only is allowed.
 * </p>
 */
@Getter
@Builder
public class UpdateQuizEvent {


    private UUID assessmentId;

    private UUID courseId;

    private List<QuestionEventType> questionPool;


    private int requiredCorrectAnswers;

    private String questionPoolingMode;

    private Integer numberOfRandomlySelectedQuestions = null;


    public static UpdateQuizEvent fromEntity(QuizEntity entity) {
        return builder()
                .assessmentId(entity.getAssessmentId())
                .courseId(entity.getCourseId())
                .numberOfRandomlySelectedQuestions(entity.getNumberOfRandomlySelectedQuestions())
                .questionPoolingMode(entity.getQuestionPoolingMode().toString())
                .questionPool(entity.getQuestionPool().stream().map(QuestionEventType::fromEntity).toList())
                .build();
    }

}
