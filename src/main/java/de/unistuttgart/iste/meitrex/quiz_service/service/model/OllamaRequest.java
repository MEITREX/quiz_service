package de.unistuttgart.iste.meitrex.quiz_service.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class OllamaRequest {

    @JsonProperty("model")
    final String model;
    @JsonProperty("prompt")
    final String prompt;
    @JsonProperty("stream")
    final boolean stream;
    @JsonProperty("format")
    final Map<String, Object> format;

    public OllamaRequest(String model, String prompt, boolean stream, Map<String, Object> format) {
        this.model = model;
        this.prompt = prompt;
        this.stream = stream;
        this.format = format;
    }

    public OllamaRequest(String model, String prompt, Map<String, Object> format) {
        this(model, prompt, false, format);
    }

    public OllamaRequest(String model, String prompt, boolean stream) {
        this(model, prompt, stream, null);
    }

    public OllamaRequest(String model, String prompt) {
        this(model, prompt, false);
    }

    /**
     * Getters for the fields
     */
    public String getModel() {
        return model;
    }

    /**
     * Getters for the fields
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * Getters for the fields
     */
    public boolean isStream() {
        return stream;
    }
}
