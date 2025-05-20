package de.unistuttgart.iste.meitrex.quiz_service.service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

import java.util.Map;
import java.util.Optional;

/**
 *
 * Example JSON:
 *
 * {
 *     "quiz": {
 *         "title": "Quiz Title",
 *         "questions": [
 *             {
 *                 "question": "question text",
 *                 "type": "multiple_choice",
 *                 "type_options": {
 *                     "options": [
 *                         {
 *                             "text": "option 1",
 *                             "is_correct": true
 *                         },
 *                         {
 *                             "text": "option 2",
 *                             "is_correct": false
 *                         },
 *                     ]
 *                 },
 *             },
 *             {
 *                 "question": "question text",
 *                 "type": "multiple_choice",
 *                 "type_options": {
 *                     "options":
 *                     {
 *                         answer: ""
 *                     }
 *                 },
 *             },
 *             {
 *  *                 "question": "question text",
 *  *                 "type": "numeric",
 *  *                 "type_options": {
 *  *                     "options":
 *  *                     {
 *  *                         answer: 0.0,
 *  *                         maxDifference: 10.0
 *  *                     }
 *  *                 },
 *  *             },
 *  *             {
 *  *                 "question": "question text",
 *  *                 "type": "exact_answer",
 *  *                 "type_options": {
 *         ]
 *     }
 * }
 */
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED) // required for deserialization
public class PromptJson {

    @Getter
    @Setter
    private Quiz quiz;

    @NoArgsConstructor(access = AccessLevel.PROTECTED) // required for deserialization
    public static class Quiz {

        @Getter
        @Setter
        private String title;

        @Getter
        @Setter
        private Question[] questions;

        public Quiz(String title, Question[] questions) {
            this.title = title;
            this.questions = questions;
        }

    }

    @NoArgsConstructor(access = AccessLevel.PROTECTED) // required for deserialization
    public static class Question {

        @Setter
        @Getter
        private String question;

        @Setter
        @Getter
        private String type;


        @JsonProperty("type_options")
        @Getter
        private Map<String, Object> typeOptions;


        @Getter
        @JsonIgnore
        private String typeOptionsJson;

        @JsonIgnore
        private ObjectMapper objectMapper = new ObjectMapper();

        public Question(String question, String type, Map<String, Object> typeOptions) {
            this.question = question;
            this.type = type;
            this.typeOptions = typeOptions;
            this.setTypeOptions(typeOptions);
        }

        public void setTypeOptions(Map<String, Object> typeOptions) {
            this.typeOptions = typeOptions;
            try {
                this.typeOptionsJson = objectMapper.writeValueAsString(typeOptions);
            } catch (Exception e) {
                this.typeOptionsJson = "{}";
            }
        }


        /**
         * @return true if the type is free_text, false otherwise
         */
        @JsonIgnore
        public boolean isFreeText(){
            return type.equals("free_text");
        }

        /**
         * @return true if the type is multiple_choice, false otherwise
         */
        @JsonIgnore
        public boolean isMultipleChoice(){
            return type.equals("multiple_choice");
        }

        /**
         * @return true if the type is numeric, false otherwise
         */
        @JsonIgnore
        public boolean isNumeric(){
            return type.equals("numeric");
        }

        /**
         * @return true if the type is exact_answer, false otherwise
         */
        @JsonIgnore
        public boolean isExactAnswer(){
            return type.equals("exact_answer");
        }

        /**
         * @return the typeOptions as FreeText if the type is free_text, otherwise an empty Optional
         */
        @JsonIgnore
        public Optional<FreeText> getAsFreeText(){
            if(isFreeText()){
                try {
                    FreeText freeText = objectMapper.readValue(typeOptionsJson, FreeText.class);
                    return Optional.of(freeText);
                } catch (Exception e) {
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        }

        /**
         * @return the typeOptions as MultipleChoice if the type is multiple_choice, otherwise an empty Optional
         */
        @JsonIgnore
        public Optional<MultipleChoice> getAsMultipleChoice(){
            if(isMultipleChoice()){
                try {
                    MultipleChoice multipleChoice = objectMapper.readValue(typeOptionsJson, MultipleChoice.class);
                    return Optional.of(multipleChoice);
                } catch (Exception e) {
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        }

        /**
         * @return the typeOptions as Numeric if the type is numeric, otherwise an empty Optional
         */
        public Optional<Numeric> getAsNumeric(){
            if(isNumeric()){
                try {
                    Numeric numeric = objectMapper.readValue(typeOptionsJson, Numeric.class);
                    return Optional.of(numeric);
                } catch (Exception e) {
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        }

        public Optional<ExactAnswer> getAsExactAnswer(){
            if(isExactAnswer()){
                try {
                    ExactAnswer exactAnswer = objectMapper.readValue(typeOptionsJson, ExactAnswer.class);
                    return Optional.of(exactAnswer);
                } catch (Exception e) {
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        }
    }


    /**
     * the answer of the free text question wrapped in a class
     */
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FreeText{
        @Getter
        @Setter
        private String answer;


    }

    /**
     * the answer of the numeric question wrapped in a class
     */
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // required for deserialization
    @AllArgsConstructor
    public static class Numeric{

        @Getter
        @Setter
        private double answer;
        @Getter
        @Setter
        @JsonProperty("max_difference")
        private double maxDifference;

    }

    /**
     * the options of the multiple choice question wrapped in a class
     */
    @NoArgsConstructor(access = AccessLevel.PROTECTED) // required for deserialization
    public static class MultipleChoice{

        @Getter
        @Setter
        private Option[] options;

        public MultipleChoice(Option[] options) {
            this.options = options;
        }



        /**
         * the options of the multiple choice question
         */
        @NoArgsConstructor(access = AccessLevel.PROTECTED) // required for deserialization
        @AllArgsConstructor
        public static class Option{

            @Getter
            @Setter
            private String text;

            @Getter
            @Setter
            @JsonProperty("is_correct")
            private boolean isCorrect;

        }

    }

    @NoArgsConstructor(access = AccessLevel.PROTECTED) // required for deserialization
    @AllArgsConstructor
    public static class ExactAnswer {
        @Getter
        @Setter
        private String answer;

        @Getter
        @Setter
        @JsonProperty("case_sensitive")
        private boolean caseSensitive = false;

    }

}
