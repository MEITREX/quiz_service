package de.unistuttgart.iste.meitrex.quiz_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

@Configuration
public class HttpClientBeans {

    @Bean
    HttpClient defaultHttpClient() {
        return HttpClient.newHttpClient();
    }
}
