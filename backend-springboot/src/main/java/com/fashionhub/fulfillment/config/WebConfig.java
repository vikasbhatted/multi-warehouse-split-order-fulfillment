package com.fashionhub.fulfillment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties.Cors cors;

    public WebConfig(AppProperties.Cors cors) {
        this.cors = cors;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(cors.allowedOrigins().split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
