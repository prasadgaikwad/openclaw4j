package dev.prasadgaikwad.openclaw4j.tool.search;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Spring configuration class for the Search tool.
 */
@Configuration
@EnableConfigurationProperties(SearchProperties.class)
public class SearchConfig {

    private static final Logger log = LoggerFactory.getLogger(SearchConfig.class);

    @Bean("searchRestClient")
    public RestClient searchRestClient(RestClient.Builder builder, SearchProperties properties) {
        log.info("Configuring Tavily Search client (enabled: {}, baseUrl: {})", properties.enabled(),
                properties.baseUrl());

        return builder
                .baseUrl(Objects.requireNonNull(properties.baseUrl(), "Tavily base URL must not be null"))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
