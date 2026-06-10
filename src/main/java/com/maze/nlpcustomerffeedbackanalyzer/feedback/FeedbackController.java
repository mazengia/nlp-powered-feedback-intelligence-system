package com.maze.nlpcustomerffeedbackanalyzer.feedback;

import com.maze.nlpcustomerffeedbackanalyzer.feedback.FeedbackDTOs.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Feedback Analyzer", description = "Analyze customer feedback using NLP — sentiment, topics, keyphrases, summarization, Amharic support")
public class FeedbackController {

    private final FeedbackService feedbackService;


    @PostMapping("/analyze")
    @Operation(
            summary     = "Analyze a single feedback",
            description = "Runs the full NLP pipeline: sentiment, topic classification, key phrases, and summarization. Supports Amharic.")
    public ResponseEntity<FeedbackResponse> analyzeFeedback(
            @Valid @RequestBody FeedbackRequest request) {
        return ResponseEntity.ok(feedbackService.analyzeFeedback(request));
    }

    @PostMapping("/analyze/batch")
    @Operation(
            summary     = "Analyze multiple feedbacks at once",
            description = "Batch analysis with aggregate insights. Max 50 items per request.")
    public ResponseEntity<BatchFeedbackResponse> analyzeBatch(
            @Valid @RequestBody BatchFeedbackRequest request) {
        return ResponseEntity.ok(feedbackService.analyzeBatch(request));
    }


    @GetMapping
    @Operation(summary = "Get all analyzed feedbacks (paginated)")
    public ResponseEntity<Page<FeedbackResponse>> getAllFeedbacks(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(feedbackService.getAllFeedbacks(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a feedback by ID")
    public ResponseEntity<FeedbackResponse> getFeedbackById(
            @Parameter(description = "Feedback ID") @PathVariable Long id) {
        return ResponseEntity.ok(feedbackService.getFeedbackById(id));
    }

    @GetMapping("/type/{feedbackType}")
    @Operation(summary = "Get feedbacks filtered by type (paginated)")
    public ResponseEntity<Page<FeedbackResponse>> getByType(
            @PathVariable Feedback.FeedbackType feedbackType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(feedbackService.getFeedbacksByType(feedbackType, pageable));
    }

    @GetMapping("/sentiment/{sentiment}")
    @Operation(summary = "Get feedbacks filtered by sentiment (paginated)")
    public ResponseEntity<Page<FeedbackResponse>> getBySentiment(
            @PathVariable Feedback.Sentiment sentiment,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(feedbackService.getFeedbacksBySentiment(sentiment, pageable));
    }


    @GetMapping("/insights")
    @Operation(
            summary     = "Get aggregate insights",
            description = "Sentiment distribution, top topics, negative-rate alert, and statistics across all stored feedbacks.")
    public ResponseEntity<InsightsResult> getInsights() {
        return ResponseEntity.ok(feedbackService.getInsights());
    }


    @GetMapping("/health")
    @Operation(summary = "Health check for the NLP service connection")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        boolean nlpUp = feedbackService.isNlpServiceHealthy();
        return ResponseEntity.ok(Map.of(
                "backend",    "UP",
                "nlpService", nlpUp ? "UP" : "DOWN",
                "status",     nlpUp ? "ALL_SYSTEMS_GO" : "NLP_UNAVAILABLE"
        ));
    }
}