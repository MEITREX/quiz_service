package de.unistuttgart.iste.meitrex.quiz_service.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AssociationEmbeddable {

    @Column(nullable = false, length = 255, name = "left_side")
    private String left;

    @Column(nullable = false, length = 255, name = "right_side")
    private String right;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String feedback;

}
