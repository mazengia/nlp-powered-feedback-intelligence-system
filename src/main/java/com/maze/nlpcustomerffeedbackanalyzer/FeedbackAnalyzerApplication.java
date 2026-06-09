package com.maze.nlpcustomerffeedbackanalyzer.feedback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.Contact;

@SpringBootApplication
@OpenAPIDefinition(
    info = @Info(
        title       = "Customer Feedback Analyzer API",
        version     = "1.0.0",
        description = "REST API for analyzing customer feedback using NLP/LLM (powered by Google Colab). " +
                      "Supports sentiment analysis, topic classification, key phrase extraction, " +
                      "summarization, and Amharic language support.",
        contact     = @Contact(name = "Feedback Analyzer Team")
    )
)
public class FeedbackAnalyzerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FeedbackAnalyzerApplication.class, args);
    }
}
