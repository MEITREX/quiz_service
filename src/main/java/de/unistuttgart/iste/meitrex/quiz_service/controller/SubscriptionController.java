package de.unistuttgart.iste.meitrex.quiz_service.controller;

import de.unistuttgart.iste.meitrex.common.event.ContentChangeEvent;
import de.unistuttgart.iste.meitrex.quiz_service.service.QuizService;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST Controller Class listening to a dapr Topic.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SubscriptionController {

    private final QuizService quizService;

    @Topic(name = "content-changed", pubsubName = "gits")
    @PostMapping(path = "/quiz-service/content-changed-pubsub")
    public Mono<Void> updateAssociation(@RequestBody final CloudEvent<ContentChangeEvent> cloudEvent) {

        return Mono.fromRunnable(() -> {
            try {
                quizService.deleteQuizzesWhenQuizContentIsDeleted(cloudEvent.getData());
            } catch (final Exception e) {
                log.error("Error while processing content-changes event. {}", e.getMessage());
            }
        });
    }

}
