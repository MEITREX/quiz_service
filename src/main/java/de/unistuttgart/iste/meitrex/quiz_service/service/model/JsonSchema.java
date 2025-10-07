package de.unistuttgart.iste.meitrex.quiz_service.service.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationModule;
import com.github.victools.jsonschema.module.jakarta.validation.JakartaValidationOption;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.util.Optional;

public class JsonSchema {


    public JsonNode buildSchema(){
        return buildSchema(new SchemaArgs());
    }
    public JsonNode buildSchema(SchemaArgs args){
        JakartaValidationModule module = new JakartaValidationModule(JakartaValidationOption.INCLUDE_PATTERN_EXPRESSIONS, JakartaValidationOption.NOT_NULLABLE_FIELD_IS_REQUIRED);
        SchemaGeneratorConfigBuilder configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON).with(module)
                .with(Option.EXTRA_OPEN_API_FORMAT_VALUES);
        // add jackson module to allow jackson annotations
        configBuilder.with(new JacksonModule());
        SchemaGenerator generator = new SchemaGenerator(configBuilder.build());
        ObjectNode jsonSchema = (ObjectNode) generator.generateSchema(PromptJson.class);
        ObjectNode questions = (ObjectNode) jsonSchema.get("properties").get("quiz").get("properties").get("questions").get("properties");
        addArraySizes(questions, args);
        return jsonSchema;
    }

    private void addArraySizes(ObjectNode questions, SchemaArgs args){
        ObjectNode multipleChoice = (ObjectNode) questions.get("multiple_choice");
        addArraySize(multipleChoice, args.minMultipleChoicesQuestions, args.maxMultipleChoicesQuestions);

        // free_text,numeric, exact_answer
        ObjectNode freeText = (ObjectNode) questions.get("free_text");
        addArraySize(freeText, args.minFreeTextQuestions, args.maxFreeTextQuestions);
        ObjectNode numeric = (ObjectNode) questions.get("numeric");
        addArraySize(numeric, args.minNumericQuestions, args.maxNumericQuestions);
        ObjectNode exactAnswer = (ObjectNode) questions.get("exact_answer");
        addArraySize(exactAnswer, args.minExactAnswerQuestions, args.maxExactAnswerQuestions);
    }

    private void addArraySize(ObjectNode questions, Optional<Integer> min, Optional<Integer> max){
        Optional<JsonNode> node = min.map( v ->  new BigIntegerNode(BigInteger.valueOf(v)));
        node.ifPresent(value -> questions.set("minItems", value));
        Optional<JsonNode> maxNode = max.map( v ->  new BigIntegerNode(BigInteger.valueOf(v)));
        maxNode.ifPresent(value -> questions.set("maxItems", value));
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class SchemaArgs{

        private Optional<Integer> minMultipleChoicesQuestions = Optional.empty();
        private Optional<Integer> maxMultipleChoicesQuestions = Optional.empty();

        private Optional<Integer> minNumericQuestions = Optional.empty();
        private Optional<Integer> maxNumericQuestions = Optional.empty();

        private Optional<Integer> minFreeTextQuestions = Optional.empty();
        private Optional<Integer> maxFreeTextQuestions = Optional.empty();

        private Optional<Integer> minExactAnswerQuestions = Optional.empty();
        private Optional<Integer> maxExactAnswerQuestions = Optional.empty();



    }

}
