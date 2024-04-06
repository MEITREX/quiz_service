package de.unistuttgart.iste.gits.quiz_service.persistence.entity;

import de.unistuttgart.iste.meitrex.generated.dto.QuestionPoolingMode;
import jakarta.persistence.*;
import lombok.*;

import java.util.*;

@Entity(name = "Quiz")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuizEntity {

    @Id
    private UUID assessmentId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("number ASC")
    @Builder.Default
    private List<QuestionEntity> questionPool = new ArrayList<>();

    @Column(nullable = false)
    private int requiredCorrectAnswers;

    @Column(nullable = false)
    @Enumerated(EnumType.ORDINAL)
    @Builder.Default
    private QuestionPoolingMode questionPoolingMode = QuestionPoolingMode.ORDERED;

    @Column(nullable = true)
    @Builder.Default
    private Integer numberOfRandomlySelectedQuestions = null;
}
