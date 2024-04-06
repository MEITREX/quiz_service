package de.unistuttgart.iste.gits.quiz_service.config;

import de.unistuttgart.iste.meitrex.common.dapr.MockTopicPublisher;
import de.unistuttgart.iste.meitrex.common.dapr.TopicPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.*;

/**
 * This is a dev-config for the TopicPublisher. It is intended to be used for development only.
 * It will log all messages instead of sending them to the dapr topic.
 * <p>
 * The purpose of this is to allow developers to work on the media-service without having to run the dapr runtime.
 * <p>
 * To use this config, set the spring profile to "dev" (or any other profile that is not "prod").
 */
@Configuration
@Profile("!prod")
@Slf4j
public class DevTopicPublisherConfiguration {

    @Bean
    public TopicPublisher getTopicPublisher() {
        log.warn("TopicPublisher is mocked. This is intended for development use only.");
        return new MockTopicPublisher();
    }
}
