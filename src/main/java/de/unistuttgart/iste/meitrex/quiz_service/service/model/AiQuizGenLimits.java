package de.unistuttgart.iste.meitrex.quiz_service.service.model;

import lombok.Getter;
import lombok.Setter;

public class AiQuizGenLimits {


    @Setter
    @Getter
    private int maxQuestions = 10;
    @Setter
    @Getter
    private int minQuestions = 1;
    @Setter
    @Getter
    private int maxAnswersPerQuestion = 4;
    @Setter
    @Getter
    boolean allowMultipleCorrectAnswers = false;
    @Setter
    @Getter
    private int maxMultipleChoiceQuestions = 5;
    @Setter
    @Getter
    private int maxFreeTextQuestions = 5;
    @Setter
    @Getter
    private int maxExactQuestions = 5;
    @Setter
    @Getter
    private int maxNumericQuestions = 5;

    public AiQuizGenLimits() {
    }



}
