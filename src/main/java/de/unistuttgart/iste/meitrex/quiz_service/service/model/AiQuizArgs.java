package de.unistuttgart.iste.meitrex.quiz_service.service.model;

import java.util.List;

public class AiQuizArgs {
    
    private String description = "";
    private List<String> resources = new java.util.ArrayList<>();
    private List<String> limitations = new java.util.ArrayList<>();

    
    public AiQuizArgs(){

    }

    /**
     * sets the description of the quiz
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    /**
     * returns the description of the quiz
     * @return
     */
    public String getDescription() {
        return description;
    }
    /**
     * sets the resources of the quiz
     * @param resources
     */
    public void setResources(List<String> resources) {
        this.resources = resources;
    }

    /**
     * returns the resources of the quiz
     * @return
     */
    public List<String> getResources() {
        return resources;
    }

    /**
     * sets the limitations of the quiz
     * @param limitations
     */
    public void setLimitations(List<String> limitations) {
        this.limitations = limitations;
    }

    /**
     * returns the limitations of the quiz
     * @return
     */
    public List<String> getLimitations() {
        return limitations;
    }

}
