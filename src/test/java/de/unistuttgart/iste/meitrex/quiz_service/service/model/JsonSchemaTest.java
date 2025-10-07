package de.unistuttgart.iste.meitrex.quiz_service.service.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

public class JsonSchemaTest {



    @Test
    public void testGenerateSchema(){
        final JsonSchema.SchemaArgs args = new JsonSchema.SchemaArgs();
        args.setMinFreeTextQuestions(Optional.of(3));
        args.setMaxFreeTextQuestions(Optional.of(5));
        args.setMinMultipleChoicesQuestions(Optional.of(2));
        args.setMaxMultipleChoicesQuestions(Optional.of(4));
        args.setMinNumericQuestions(Optional.of(1));
        args.setMaxNumericQuestions(Optional.of(2));
        args.setMinExactAnswerQuestions(Optional.of(7));
        args.setMaxExactAnswerQuestions(Optional.of(10));
        final JsonSchema js = new JsonSchema();
        final JsonNode res = js.buildSchema(args);

        // find exact answer node
        JsonNode exactAnswerNode = res.at("/properties/quiz/properties/questions/properties/exact_answer");
        // find free text node
        JsonNode freeTextNode = res.at("/properties/quiz/properties/questions/properties/free_text");
        // find multiple choice node
        JsonNode multipleChoiceNode = res.at("/properties/quiz/properties/questions/properties/multiple_choice");
        // find numeric node
        JsonNode numericNode = res.at("/properties/quiz/properties/questions/properties/numeric");

        // check if the minItems and maxItems are set correctly
        assert exactAnswerNode.isObject() && exactAnswerNode.has("minItems") && exactAnswerNode.has("maxItems");
        assert freeTextNode.isObject() && freeTextNode.has("minItems") && freeTextNode.has("maxItems");
        assert multipleChoiceNode.isObject() && multipleChoiceNode.has("minItems") && multipleChoiceNode.has("maxItems");
        assert numericNode.isObject() && numericNode.has("minItems") && numericNode.has("maxItems");

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> resMap = mapper.convertValue(res, Map.class);
        int size = resMap.size();
        assert size == 4;
        assert resMap.containsKey("$schema");
        assert resMap.containsKey("type");
        assert resMap.containsKey("properties");
        assert resMap.containsKey("required");

    }


}
