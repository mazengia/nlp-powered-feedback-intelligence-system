package com.maze.nlpcustomerffeedbackanalyzer.feedback;

 import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// ─────────────────────────────────────────────────────────────────────────────
// REQUEST DTOs
// ─────────────────────────────────────────────────────────────────────────────

public class FeedbackDTOs {

    /** Single feedback submission */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FeedbackRequest {

        @NotBlank(message = "Feedback text is required")
        @Size(min = 3, max = 10000, message = "Text must be between 3 and 10000 characters")
        private String text;

        @Builder.Default
        private Feedback.FeedbackType feedbackType = Feedback.FeedbackType.GENERAL;

        private String source;   // amazon, twitter, form, email, etc.
    }

    /** Batch feedback submission */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchFeedbackRequest {
        private List<FeedbackRequest> feedbacks;
//        @Builder.Default
        // convenience: set a common feedback type for the whole batch
        private Feedback.FeedbackType defaultFeedbackType = Feedback.FeedbackType.GENERAL;
    }

// ─────────────────────────────────────────────────────────────────────────────
// RESPONSE DTOs
// ─────────────────────────────────────────────────────────────────────────────

    /** Rich analysis result returned to clients */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FeedbackResponse {
        private Long   id;
        private String text;
        private String feedbackType;
        private String source;
        private String language;

        private SentimentResult    sentiment;
        private TopicResult        topic;
        private KeyphrasesResult   keyphrases;
        private SummaryResult      summary;

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
        private String       primaryTopic;
        private Double       confidence;
        private List<Map<String, Object>> topTopics;
        private String       feedbackType;
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

    /** Batch analysis result */
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BatchFeedbackResponse {
        private Integer              count;
        private List<FeedbackResponse> results;
        private InsightsResult       insights;
    }

    /** Aggregate insights across a batch */
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class InsightsResult {
        private Integer            totalFeedbacks;
        private Map<String, Long>  sentimentDistribution;
        private Double             positiveRatePct;
        private Double             negativeRatePct;
        private Map<String, Long>  topTopics;
        private Map<String, Long>  topKeyphrases;
        private String             alert;  // e.g. "High negative feedback detected!"
    }

    /** Generic API error response */
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ErrorResponse {
        private String message;
        private String details;
        private LocalDateTime timestamp;
    }
}
