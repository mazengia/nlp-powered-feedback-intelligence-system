package com.maze.nlpcustomerffeedbackanalyzer.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.maze.nlpcustomerffeedbackanalyzer.exception.NlpServiceException;
import com.maze.nlpcustomerffeedbackanalyzer.feedback.Feedback;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP client for the NLP Flask server running on Google Colab,
 * exposed via an ngrok tunnel.
 *
 * <p>All calls are synchronous. Errors throw {@link NlpServiceException}
 * so callers can handle them without catching raw {@link RuntimeException}.
 */
@Service
@Slf4j
public class NlpServiceClient {

    @Value("${nlp.service.base-url}")
    private String baseUrl;

    @Value("${nlp.service.timeout-seconds:60}")
    private int timeoutSeconds;

    @Value("${nlp.service.max-batch-size:50}")
    private int maxBatchSize;

    /** Shared, thread-safe HTTP/1.1 + HTTP/2 client — do NOT recreate per request. */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_2)
            .build();

    private final ObjectMapper mapper;

    public NlpServiceClient(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    // ── Public API ────────────────────────────────────────────────────────

    @CircuitBreaker(name = "nlpService", fallbackMethod = "analyzeOneFallback")
    @Retry(name = "nlpService")
    @RateLimiter(name = "api")
    public JsonNode analyzeOne(String text, Feedback.FeedbackType feedbackType, String source) {
        ObjectNode body = mapper.createObjectNode();
        body.put("text",          text);
        body.put("feedback_type", feedbackType.name().toLowerCase());
        body.put("source",        Optional.ofNullable(source).orElse("unknown"));
        return post("/api/analyze", body);
    }

    @CircuitBreaker(name = "nlpService", fallbackMethod = "analyzeBatchFallback")
    @Retry(name = "nlpService")
    @RateLimiter(name = "api")
    public JsonNode analyzeBatch(List<Map<String, String>> feedbacks) {
        if (feedbacks.size() > maxBatchSize) {
            throw new NlpServiceException("Batch size %d exceeds limit of %d".formatted(feedbacks.size(), maxBatchSize));
        }
        ArrayNode arr = mapper.createArrayNode();
        feedbacks.forEach(fb -> {
            ObjectNode node = mapper.createObjectNode();
            node.put("text",          fb.getOrDefault("text", ""));
            node.put("feedback_type", fb.getOrDefault("feedbackType", "general").toLowerCase());
            node.put("source",        fb.getOrDefault("source", "unknown"));
            arr.add(node);
        });
        ObjectNode body = mapper.createObjectNode();
        body.set("feedbacks", arr);
        return post("/api/analyze/batch", body);
    }

    @Retry(name = "nlpService")
    @RateLimiter(name = "api")
    public JsonNode getSentiment(String text) {
        return post("/api/sentiment", textBody(text));
    }

    @Retry(name = "nlpService")
    @RateLimiter(name = "api")
    public JsonNode getTopics(String text, String feedbackType) {
        ObjectNode body = textBody(text);
        body.put("feedback_type", feedbackType);
        return post("/api/topics", body);
    }

    @Retry(name = "nlpService")
    @RateLimiter(name = "api")
    public JsonNode getKeyphrases(String text, int topN) {
        ObjectNode body = textBody(text);
        body.put("top_n", topN);
        return post("/api/keyphrases", body);
    }

    @Retry(name = "nlpService")
    @RateLimiter(name = "api")
    public JsonNode getSummary(String text) {
        return post("/api/summarize", textBody(text));
    }

    public boolean isHealthy() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/health"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<Void> resp = httpClient.send(req, HttpResponse.BodyHandlers.discarding());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            log.warn("NLP health check failed: {}", e.getMessage());
            return false;
        }
    }

    // ── Internals ─────────────────────────────────────────────────────────

    private ObjectNode textBody(String text) {
        ObjectNode body = mapper.createObjectNode();
        body.put("text", text);
        return body;
    }

    private JsonNode post(String path, ObjectNode body) {
        String url = baseUrl + path;
        try {
            String requestBody = mapper.writeValueAsString(body);
            log.debug("NLP POST {} body={}", path, requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type",  "application/json")
                    .header("Accept",        "application/json")
                    .header("ngrok-skip-browser-warning", "true")   // avoids ngrok interstitial page
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("NLP service {} returned HTTP {}: {}", path, response.statusCode(), response.body());
                throw new NlpServiceException(
                        "NLP service returned HTTP %d for %s".formatted(response.statusCode(), path));
            }
            return mapper.readTree(response.body());

        } catch (NlpServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("NLP call failed [{}]: {}", url, e.getMessage());
            throw new NlpServiceException("NLP service unavailable: " + e.getMessage(), e);
        }
    }

    // ── Fallback Methods for Circuit Breaker ──────────────────────────────

    public JsonNode analyzeOneFallback(String text, Feedback.FeedbackType feedbackType, String source, Exception ex) {
        log.warn("Circuit breaker triggered for NLP service, returning fallback response");
        ObjectNode fallback = mapper.createObjectNode();
        fallback.put("status", "service_unavailable");
        fallback.put("message", "NLP service temporarily unavailable. Please retry later.");
        fallback.put("error", ex.getMessage());
        return fallback;
    }

    public JsonNode analyzeBatchFallback(List<Map<String, String>> feedbacks, Exception ex) {
        log.warn("Circuit breaker triggered for batch analysis, returning fallback response");
        ObjectNode fallback = mapper.createObjectNode();
        fallback.put("status", "service_unavailable");
        fallback.put("message", "NLP service temporarily unavailable for batch processing.");
        fallback.put("error", ex.getMessage());
        fallback.put("processed_count", 0);
        return fallback;
    }
}
