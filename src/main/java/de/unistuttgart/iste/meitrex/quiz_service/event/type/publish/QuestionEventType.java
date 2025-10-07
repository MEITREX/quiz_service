package de.unistuttgart.iste.meitrex.quiz_service.event.type.publish;

import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.QuestionEntity;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * <p>
 * This class is used by different events and other services depends on it.
 * Only make changes that are compatible. Adding fields only is allowed.
 * </p>
 */
@Builder
@Getter
public class QuestionEventType {

    private UUID itemId;

    private int number;

    private String type;

    private String hint;

    private boolean aiGenerated;


    private List<QuestionStaticsEventType> questionStatistics = new ArrayList<>();

    public static QuestionEventType fromEntity(QuestionEntity entity){
        return builder()
                .aiGenerated(entity.isAiGenerated())
                .hint(entity.getHint())
                .number(entity.getNumber())
                .type(entity.getType().toString())
                .itemId(entity.getItemId())
                .questionStatistics(entity.getQuestionStatistics().stream().map(QuestionStaticsEventType::fromEntity).toList())
                .build();
    }
}
