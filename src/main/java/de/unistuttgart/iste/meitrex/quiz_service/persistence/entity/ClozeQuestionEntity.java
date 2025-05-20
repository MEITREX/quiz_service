package de.unistuttgart.iste.meitrex.quiz_service.persistence.entity;


import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity(name = "ClozeQuestion")
@ToString(callSuper = true)
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ClozeQuestionEntity extends QuestionEntity {

    @ElementCollection
    @Builder.Default
    private List<ClozeElementEmbeddable> clozeElements = new ArrayList<>();

    @ElementCollection
    @Builder.Default
    private List<String> additionalWrongAnswers = new ArrayList<>();

    @Column(nullable = false)
    private boolean showBlanksList;
}
