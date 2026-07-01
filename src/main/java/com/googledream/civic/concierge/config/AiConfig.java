package com.googledream.civic.concierge.config;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public GenerativeModel generativeModel() {
        VertexAI vertexAI = new VertexAI("civic-concierge-hackathon", "us-central1");
        return new GenerativeModel("gemini-2.5-flash", vertexAI);    }
}