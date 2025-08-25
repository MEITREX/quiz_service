package de.unistuttgart.iste.meitrex.quiz_service.event;

import de.unistuttgart.iste.meitrex.quiz_service.event.type.publish.UpdateQuizEvent;
import de.unistuttgart.iste.meitrex.quiz_service.persistence.entity.QuizEntity;
import io.dapr.client.DaprClient;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * This class is responsible for publishing events that a controlled by the quiz service.
 */
public class EventPublisher {



    @Generated
    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);
    private static final String PUBSUB_NAME = "meitrex";
    private final DaprClient client;


    @Generated
    public EventPublisher(final DaprClient client) {
        this.client = client;
    }


    /**
     * Publishes an event that a quiz was updated.
     *
     * @param quizEvent the event containing the updated quiz data
     */
    public  Mono<Void> publishUpdateQuizEvent(final UpdateQuizEvent quizEvent) {
       return this.publishEvent(quizEvent, "quiz_updated");
    }

    /**
     * converts a quiz entity to an event an publish it
     * @param quizEntity the entity
     */
    public  Mono<Void> publishUpdateQuizEvent(final QuizEntity quizEntity) {
        return publishUpdateQuizEvent(UpdateQuizEvent.fromEntity(quizEntity));
    }




    protected Mono<Void> publishEvent(final Object dto, final String daprTopic) {
        return this.client.publishEvent(PUBSUB_NAME, daprTopic, dto)
                .doOnSuccess((response) -> log.debug("Published message to daprTopic {}: {}", daprTopic, response))
                .doOnError((error) -> log.error("Error while publishing message to daprTopic {}: {}", daprTopic, error.getMessage()));

    }

}
