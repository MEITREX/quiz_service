package de.unistuttgart.iste.meitrex.quiz_service.config;

import de.unistuttgart.iste.meitrex.common.dapr.TopicPublisher;
import de.unistuttgart.iste.meitrex.quiz_service.event.EventPublisher;
import io.dapr.client.DaprClientBuilder;
import io.dapr.config.Properties;
import io.dapr.config.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.HashMap;
import java.util.Map;


/**
 * This is a prod-config for the TopicPublisher. It uses the dapr client to send messages to the dapr topic.
 */
@Configuration
@Profile("prod")
public class TopicPublisherConfiguration {

    @Value("${dapr.client.grpcEndpoint}")
    private String grpcEndpoint;

    @Value("${dapr.client.httpEndpoint}")
    private String httpEndpoint;

    @Value("${dapr.client.httpPort}")
    private String httpPort;

    @Value("${dapr.client.grpcPort}")
    private String gPort;

    private Logger logger = LoggerFactory.getLogger(TopicPublisherConfiguration.class);

    @Bean
    public TopicPublisher getTopicPublisher() {
        return new TopicPublisher(new DaprClientBuilder().build());
    }

    @Bean
    public EventPublisher getEventPublisher() {

        final Map<Property<?>, String> propertyOverrides = new HashMap<>();


        propertyOverrides.put(Properties.GRPC_ENDPOINT, grpcEndpoint);
        propertyOverrides.put(Properties.HTTP_ENDPOINT, httpEndpoint);
        propertyOverrides.put(Properties.HTTP_PORT, httpPort);
        propertyOverrides.put(Properties.GRPC_PORT, gPort);

        logger.debug("Using property overrides: {}", propertyOverrides);

        return new EventPublisher(
                new DaprClientBuilder()
                    //    .withPropertyOverrides(propertyOverrides)
                        .build()
                );
    }

}
