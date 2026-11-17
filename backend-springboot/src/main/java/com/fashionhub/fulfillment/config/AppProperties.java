package com.fashionhub.fulfillment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

public final class AppProperties {
    private AppProperties() {}

    @ConfigurationProperties(prefix = "app.camunda-api")
    public record CamundaApi(String baseUrl, String username, String password, String operateUrl) {}

    @ConfigurationProperties(prefix = "app.courier")
    public record Courier(String apiKey, String publicBaseUrl) {}

    @ConfigurationProperties(prefix = "app.fulfillment")
    public record Fulfillment(String substitutionTimeout) {}

    @ConfigurationProperties(prefix = "app.cors")
    public record Cors(String allowedOrigins) {}
}
