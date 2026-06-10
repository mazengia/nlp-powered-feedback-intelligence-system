package com.maze.nlpcustomerffeedbackanalyzer.feedback;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedbacks", indexes = {
        @Index(name = "idx_sentiment",      columnList = "sentiment"),
        @Index(name = "idx_feedback_type",  columnList = "feedbackType"),
        @Index(name = "idx_analysis_status",columnList = "analysisStatus"),
        @Index(name = "idx_created_at",     columnList = "createdAt"),
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackType feedbackType;

    @Column(length = 100)
    private String source;

    @Column(length = 10)
    private String language;

    // ── NLP results ───────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Sentiment sentiment;

    @Column
    private Double sentimentConfidence;

    @Column(length = 200)
    private String primaryTopic;

    @Column
    private Double topicConfidence;

    @Column(columnDefinition = "TEXT")
    private String keyphrases;      // JSON array

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String rawNlpResult;    // full NLP service JSON

    // ── Metadata ──────────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime analyzedAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private AnalysisStatus analysisStatus = AnalysisStatus.PENDING;

    // ── Enums ─────────────────────────────────────────────────────────────

    public enum FeedbackType {
        PRODUCT_REVIEW, SUPPORT_TICKET, SURVEY_RESPONSE, SOCIAL_MEDIA, GENERAL
    }

    public enum Sentiment {
        POSITIVE, NEGATIVE, NEUTRAL
    }

    public enum AnalysisStatus {
        PENDING, ANALYZING, COMPLETED, FAILED
    }
}