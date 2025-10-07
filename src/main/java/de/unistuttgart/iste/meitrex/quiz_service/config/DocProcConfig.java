package de.unistuttgart.iste.meitrex.quiz_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration()
@ConfigurationProperties(prefix = "docproc")
@Setter
@Getter
public class DocProcConfig {

    private String url;





}
