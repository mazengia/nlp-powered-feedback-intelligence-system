package com.maze.nlpcustomerffeedbackanalyzer.feedback;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class FeedbackDTOs {

    // ── Requests ──────────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FeedbackRequest {

        @NotBlank(message = "Feedback text is required")
        @Size(min = 3, max = 10_000, message = "Text must be 3–10 000 characters")
        private String text;

        @Builder.Default
        private Feedback.FeedbackType feedbackType = Feedback.FeedbackType.GENERAL;

        @Size(max = 100, message = "Source must not exceed 100 characters")
        private String source;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BatchFeedbackRequest {

        @NotEmpty(message = "Feedbacks list must not be empty")
        @Size(max = 50, message = "Maximum 50 items per batch")
        @Valid
        private List<FeedbackRequest> feedbacks;

        /** Applied to any item whose feedbackType is null */
        @Builder.Default
        private Feedback.FeedbackType defaultFeedbackType = Feedback.FeedbackType.GENERAL;
    }

    // ── Responses ─────────────────────────────────────────────────────────

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class FeedbackResponse {
        private Long   id;
        private String text;
        private String feedbackType;
        private String source;
        private String language;

        private SentimentResult  sentiment;
        private TopicResult      topic;
        private KeyphrasesResult keyphrases;
        private SummaryResult    summary;

        private Feedback.AnalysisStatus analysisStatus;
        private LocalDateTime           createdAt;
        private LocalDateTime           analyzedAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SentimentResult {
        private String  sentiment;
        private Double  confidence;
        private String  emoji;
        private String  language;
        private Boolean amharicTranslated;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TopicResult {
        private String                    primaryTopic;
        private Double                    confidence;
        private List<Map<String, Object>> topTopics;
        private String                    feedbackType;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class KeyphrasesResult {
        private List<Map<String, Object>> keyphrases;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SummaryResult {
        private String summary;
        private String note;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BatchFeedbackResponse {
        private int                    count;
        private List<FeedbackResponse> results;
        private InsightsResult         insights;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class InsightsResult {
        private int                totalFeedbacks;
        private Map<String, Long>  sentimentDistribution;
        private double             positiveRatePct;
        private double             negativeRatePct;
        private Map<String, Long>  topTopics;
        private Map<String, Long>  topKeyphrases;
        private String             alert;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ErrorResponse {
        private String        message;
        private String        details;
        private LocalDateTime timestamp;
    }
}