package com.maze.nlpcustomerffeedbackanalyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.maze.nlpcustomerffeedbackanalyzer.feedback.Feedback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * HTTP client that communicates with the NLP Flask server running on Google Colab.
 * The server is exposed to the internet via an ngrok tunnel.
 */
@Service
@Slf4j
public class NlpServiceClient {

    @Value("${nlp.service.base-url}")
    private String baseUrl;

    @Value("${nlp.service.timeout-seconds:60}")
    private int timeoutSeconds;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    // ─── Full Analysis ────────────────────────────────────────────────────

    public JsonNode analyzeOne(String text, Feedback.FeedbackType feedbackType, String source) {
        ObjectNode body = mapper.createObjectNode();
        body.put("text", text);
        body.put("feedback_type", feedbackType.name().toLowerCase());
        body.put("source", source != null ? source : "unknown");
        return post("/api/analyze", body);
    }

    // ─── Batch Analysis ───────────────────────────────────────────────────

    public JsonNode analyzeBatch(List<Map<String, String>> feedbacks) {
        ObjectNode body = mapper.createObjectNode();
        ArrayNode arr = mapper.createArrayNode();
        for (Map<String, String> fb : feedbacks) {
            ObjectNode node = mapper.createObjectNode();
            node.put("text",          fb.getOrDefault("text", ""));
            node.put("feedback_type", fb.getOrDefault("feedbackType", "general").toLowerCase());
            node.put("source",        fb.getOrDefault("source", "unknown"));
            arr.add(node);
        }
        body.set("feedbacks", arr);
        return post("/api/analyze/batch", body);
    }

    // ─── Individual Features ──────────────────────────────────────────────

    public JsonNode getSentiment(String text) {
        ObjectNode body = mapper.createObjectNode();
        body.put("text", text);
        return post("/api/sentiment", body);
    }

    public JsonNode getTopics(String text, String feedbackType) {
        ObjectNode body = mapper.createObjectNode();
        body.put("text", text);
        body.put("feedback_type", feedbackType);
        return post("/api/topics", body);
    }

    public JsonNode getKeyphrases(String text, int topN) {
        ObjectNode body = mapper.createObjectNode();
        body.put("text", text);
        body.put("top_n", topN);
        return post("/api/keyphrases", body);
    }

    public JsonNode getSummary(String text) {
        ObjectNode body = mapper.createObjectNode();
        body.put("text", text);
        return post("/api/summarize", body);
    }

    public boolean isHealthy() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/health"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            log.warn("NLP service health check failed: {}", e.getMessage());
            return false;
        }
    }

    // ─── Internal HTTP helper ─────────────────────────────────────────────

    private JsonNode post(String path, ObjectNode body) {
        try {
            String requestBody = mapper.writeValueAsString(body);
            log.debug("NLP POST {} | body: {}", path, requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("NLP service returned HTTP {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("NLP service error: HTTP " + response.statusCode());
            }

            return mapper.readTree(response.body());

        } catch (Exception e) {
            log.error("Failed to call NLP service at {}: {}", path, e.getMessage());
            throw new RuntimeException("NLP service unavailable: " + e.getMessage(), e);
        }
    }
}
