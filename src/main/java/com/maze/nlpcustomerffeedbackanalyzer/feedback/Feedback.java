package com.maze.nlpcustomerffeedbackanalyzer.feedback;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedbacks")
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

    @Column
    private String source;  // amazon, twitter, email, form, etc.

    @Column
    private String language; // detected language code (en, am, fr, ...)

    // ─── NLP Results ──────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    private Sentiment sentiment;

    @Column
    private Double sentimentConfidence;

    @Column
    private String primaryTopic;

    @Column
    private Double topicConfidence;

    @Column(columnDefinition = "TEXT")
    private String keyphrases;  // JSON array stored as text

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String rawNlpResult; // full JSON from NLP service

    // ─── Metadata ─────────────────────────────────────────────────────────

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime analyzedAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AnalysisStatus analysisStatus = AnalysisStatus.PENDING;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ─── Enums ────────────────────────────────────────────────────────────

    public enum FeedbackType {
        PRODUCT_REVIEW,
        SUPPORT_TICKET,
        SURVEY_RESPONSE,
        SOCIAL_MEDIA,
        GENERAL
    }

    public enum Sentiment {
        POSITIVE, NEGATIVE, NEUTRAL
    }

    public enum AnalysisStatus {
        PENDING, ANALYZING, COMPLETED, FAILED
    }
}
