package com.maze.nlpcustomerffeedbackanalyzer;

import com.maze.nlpcustomerffeedbackanalyzer.feedback.Feedback;
import com.maze.nlpcustomerffeedbackanalyzer.feedback.FeedbackDTOs.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Feedback Analyzer", description = "Analyze customer feedback using NLP & LLM")
public class FeedbackController {

    private final FeedbackService feedbackService;

    // ─── Analyze ──────────────────────────────────────────────────────────

    @PostMapping("/analyze")
    @Operation(
        summary     = "Analyze a single feedback",
        description = "Runs full NLP pipeline: sentiment, topic classification, key phrases, and summarization. Supports Amharic."
    )
    public ResponseEntity<FeedbackResponse> analyzeFeedback(
            @Valid @RequestBody FeedbackRequest request) {
        return ResponseEntity.ok(feedbackService.analyzeFeedback(request));
    }

    @PostMapping("/analyze/batch")
    @Operation(
        summary     = "Analyze multiple feedbacks at once",
        description = "Batch analysis with aggregate insights. Max 50 items per request."
    )
    public ResponseEntity<BatchFeedbackResponse> analyzeBatch(
            @Valid @RequestBody BatchFeedbackRequest request) {
        return ResponseEntity.ok(feedbackService.analyzeBatch(request));
    }

    // ─── Read ─────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Get all analyzed feedbacks")
    public ResponseEntity<List<FeedbackResponse>> getAllFeedbacks() {
        return ResponseEntity.ok(feedbackService.getAllFeedbacks());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a feedback by ID")
    public ResponseEntity<FeedbackResponse> getFeedbackById(
            @Parameter(description = "Feedback ID") @PathVariable Long id) {
        return ResponseEntity.ok(feedbackService.getFeedbackById(id));
    }

    @GetMapping("/type/{feedbackType}")
    @Operation(summary = "Get feedbacks filtered by type")
    public ResponseEntity<List<FeedbackResponse>> getByType(
            @PathVariable Feedback.FeedbackType feedbackType) {
        return ResponseEntity.ok(feedbackService.getFeedbacksByType(feedbackType));
    }

    @GetMapping("/sentiment/{sentiment}")
    @Operation(summary = "Get feedbacks filtered by sentiment")
    public ResponseEntity<List<FeedbackResponse>> getBySentiment(
            @PathVariable Feedback.Sentiment sentiment) {
        return ResponseEntity.ok(feedbackService.getFeedbacksBySentiment(sentiment));
    }

    // ─── Insights ─────────────────────────────────────────────────────────

    @GetMapping("/insights")
    @Operation(
        summary     = "Get aggregate insights",
        description = "Returns sentiment distribution, top topics, negative rate alert, and key statistics across all stored feedbacks."
    )
    public ResponseEntity<InsightsResult> getInsights() {
        return ResponseEntity.ok(feedbackService.getInsights());
    }

    // ─── Health ───────────────────────────────────────────────────────────

    @GetMapping("/health")
    @Operation(summary = "Health check for the NLP service connection")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        boolean nlpHealthy = feedbackService.isNlpServiceHealthy();
        return ResponseEntity.ok(Map.of(
                "backend",    "UP",
                "nlpService", nlpHealthy ? "UP" : "DOWN",
                "status",     nlpHealthy ? "ALL_SYSTEMS_GO" : "NLP_UNAVAILABLE"
        ));
    }
}
