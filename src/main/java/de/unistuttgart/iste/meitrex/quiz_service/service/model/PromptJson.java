package de.unistuttgart.iste.meitrex.quiz_service.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;


@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED) // required for deserialization
public class PromptJson {

    @Getter
    @Setter
    @NotNull
    private Quiz quiz;

    @NoArgsConstructor(access = AccessLevel.PROTECTED) // required for deserialization
    public static class Quiz {

        @Getter
        @Setter
        @NotNull
        private String title;

        @Getter
        @Setter
        @NotNull
        private Questions questions;

        public Quiz(String title, Questions questions) {
            this.title = title;
            this.questions = questions;
        }

    }

    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Setter
    @Getter
    public static class Questions{

        @JsonProperty(value = "multiple_choice", required = true)
        @NotNull
        private List<MultipleChoice> multipleChoiceQuestions;

        @JsonProperty(value = "free_text", required = true)
        @NotNull
        private List<FreeText> freeTextQuestions;

        @JsonProperty(value = "numeric", required = true)
        @NotNull
        private List<Numeric> numericQuestions;

        @JsonProperty(value = "exact_answer", required = true)
        @NotNull
        private List<ExactAnswer> exactAnswerQuestions;

    }




    /**
     * the answer of the free text question wrapped in a class
     */
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class FreeText{

        @NotNull
        private String question;

        @NotNull
        private String answer;



    }

    /**
     * the answer of the numeric question wrapped in a class
     */
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // required for deserialization
    @AllArgsConstructor
    @Getter
    @Setter
    public static class Numeric{

        @NotNull
        private String question;

        @NotNull
        private double answer;

        @JsonProperty(value = "max_difference", required = true)
        @NotNull
        private double maxDifference;



    }

    /**
     * the options of the multiple choice question wrapped in a class
     */
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // required for deserialization
    @Getter
    @Setter
    public static class MultipleChoice{

        @NotNull
        private String question;

        @Size(min = 4, max = 4, message = "There must be exactly 4 options for a multiple choice question")
        @NotNull
        private List<Option> options;


        /**
         * the options of the multiple choice question
         */
        @NoArgsConstructor(access = AccessLevel.PROTECTED) // required for deserialization
        @AllArgsConstructor
        @Getter
        @Setter
        public static class Option{

            @NotNull
            private String text;

            @JsonProperty(value = "is_correct", required = true)
            @NotNull
            private boolean isCorrect;

        }

    }

    @NoArgsConstructor(access = AccessLevel.PROTECTED) // required for deserialization
    @AllArgsConstructor
    @Getter
    @Setter
    public static class ExactAnswer {

        @NotNull
        private String question;

        @NotNull
        private String answer;


        @JsonProperty(value = "case_sensitive", required = true)
        @NotNull
        private boolean caseSensitive = false;

    }

}
