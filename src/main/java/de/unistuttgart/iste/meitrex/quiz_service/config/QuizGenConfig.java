package de.unistuttgart.iste.meitrex.quiz_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration()
@ConfigurationProperties(prefix = "quizgen")
@Setter
@Getter
public class QuizGenConfig {

    private String model;





}
