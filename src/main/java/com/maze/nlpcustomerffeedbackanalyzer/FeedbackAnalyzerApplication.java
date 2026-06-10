package com.maze.nlpcustomerffeedbackanalyzer;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(
                title       = "Customer Feedback Analyzer API",
                version     = "1.0.0",
                description = "REST API for analyzing customer feedback using NLP/LLM " +
                        "(powered by Google Colab + Flask). " +
                        "Supports sentiment analysis, topic classification, " +
                        "key phrase extraction, summarization, and Amharic.",
                contact     = @Contact(name = "Mdsf", email = "mz.tesfa@gmail.com")
        )
)
public class FeedbackAnalyzerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FeedbackAnalyzerApplication.class, args);
    }
}