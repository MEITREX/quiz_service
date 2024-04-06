package de.unistuttgart.iste.gits.quiz_service.persistence.entity;

import de.unistuttgart.iste.meitrex.generated.dto.ClozeElementType;
import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClozeElementEmbeddable {

    @OrderColumn(nullable = false)
    private int position;

    @Enumerated(EnumType.ORDINAL)
    private ClozeElementType type;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String text;

    @Column(nullable = true)
    private String correctAnswer;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String feedback;
}
