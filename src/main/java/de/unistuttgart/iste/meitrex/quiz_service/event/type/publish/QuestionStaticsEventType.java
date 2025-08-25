package de.unistuttgart.iste.meitrex.quiz_service.event.type.publish;

import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.QuestionStatisticEntity;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * <p>
 * This class is used by different events and other services depends on it.
 * Only make changes that are compatible. Adding fields only is allowed.
 * </p>
 */
@Builder
@Getter
public class QuestionStaticsEventType {

    private UUID id;

    private UUID questionId;

    private UUID userId;

    private boolean answeredCorrectly;

    /**
     * converts from a database entity to an event type
     * @param entity the entity
     * @return the event type
     */
    public static QuestionStaticsEventType fromEntity(QuestionStatisticEntity entity){
        return builder()
                .id(entity.getId())
                .questionId(entity.getQuestionId())
                .userId(entity.getUserId())
                .answeredCorrectly(entity.isAnsweredCorrectly())
                .build();
    }
}
