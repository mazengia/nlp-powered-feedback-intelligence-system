package com.maze.nlpcustomerffeedbackanalyzer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maze.nlpcustomerffeedbackanalyzer.feedback.Feedback;
import com.maze.nlpcustomerffeedbackanalyzer.feedback.FeedbackDTOs.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {

    private final FeedbackRepository   repository;
    private final NlpServiceClient     nlpClient;
    private final ObjectMapper         mapper = new ObjectMapper();

    // ─── Analyze Single Feedback ──────────────────────────────────────────

    @Transactional
    public FeedbackResponse analyzeFeedback(FeedbackRequest request) {
        // 1. Persist with PENDING status
        Feedback feedback = Feedback.builder()
                .text(request.getText())
                .feedbackType(request.getFeedbackType())
                .source(request.getSource())
                .analysisStatus(Feedback.AnalysisStatus.ANALYZING)
                .build();
        feedback = repository.save(feedback);

        try {
            // 2. Call NLP service
            JsonNode nlpResult = nlpClient.analyzeOne(
                    request.getText(),
                    request.getFeedbackType(),
                    request.getSource()
            );

            // 3. Map NLP results to entity
            enrichFeedbackFromNlp(feedback, nlpResult);
            feedback.setAnalysisStatus(Feedback.AnalysisStatus.COMPLETED);
            feedback.setAnalyzedAt(LocalDateTime.now());
            feedback = repository.save(feedback);

            log.info("Feedback [id={}] analyzed | sentiment={} | topic={}",
                    feedback.getId(), feedback.getSentiment(), feedback.getPrimaryTopic());

            return buildResponse(feedback, nlpResult);

        } catch (Exception e) {
            log.error("Analysis failed for feedback id={}: {}", feedback.getId(), e.getMessage());
            feedback.setAnalysisStatus(Feedback.AnalysisStatus.FAILED);
            repository.save(feedback);
            throw new RuntimeException("Analysis failed: " + e.getMessage(), e);
        }
    }

    // ─── Batch Analyze ────────────────────────────────────────────────────

    @Transactional
    public BatchFeedbackResponse analyzeBatch(BatchFeedbackRequest batchRequest) {
        List<FeedbackRequest> feedbackRequests = batchRequest.getFeedbacks();

        // Save all as ANALYZING
        List<Feedback> entities = feedbackRequests.stream().map(req ->
                repository.save(Feedback.builder()
                        .text(req.getText())
                        .feedbackType(req.getFeedbackType())
                        .source(req.getSource())
                        .analysisStatus(Feedback.AnalysisStatus.ANALYZING)
                        .build())
        ).collect(Collectors.toList());

        // Build payload for NLP batch endpoint
        List<Map<String, String>> nlpPayload = feedbackRequests.stream().map(req -> {
            Map<String, String> m = new HashMap<>();
            m.put("text",         req.getText());
            m.put("feedbackType", req.getFeedbackType().name());
            m.put("source",       req.getSource() != null ? req.getSource() : "unknown");
            return m;
        }).collect(Collectors.toList());

        JsonNode batchNlpResult = nlpClient.analyzeBatch(nlpPayload);
        JsonNode nlpResults = batchNlpResult.get("results");
        JsonNode nlpInsights = batchNlpResult.get("insights");

        // Map results back to entities
        List<FeedbackResponse> responses = new ArrayList<>();
        for (int i = 0; i < entities.size(); i++) {
            Feedback entity = entities.get(i);
            try {
                JsonNode nlpItem = nlpResults.get(i);
                enrichFeedbackFromNlp(entity, nlpItem);
                entity.setAnalysisStatus(Feedback.AnalysisStatus.COMPLETED);
                entity.setAnalyzedAt(LocalDateTime.now());
                entity = repository.save(entity);
                responses.add(buildResponse(entity, nlpItem));
            } catch (Exception e) {
                log.error("Batch item {} failed: {}", i, e.getMessage());
                entity.setAnalysisStatus(Feedback.AnalysisStatus.FAILED);
                repository.save(entity);
            }
        }

        InsightsResult insights = mapInsights(nlpInsights);

        return BatchFeedbackResponse.builder()
                .count(responses.size())
                .results(responses)
                .insights(insights)
                .build();
    }

    // ─── Queries ──────────────────────────────────────────────────────────

    public List<FeedbackResponse> getAllFeedbacks() {
        return repository.findAll().stream()
                .map(f -> buildResponse(f, null))
                .collect(Collectors.toList());
    }

    public FeedbackResponse getFeedbackById(Long id) {
        Feedback f = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Feedback not found: " + id));
        return buildResponse(f, null);
    }

    public List<FeedbackResponse> getFeedbacksByType(Feedback.FeedbackType type) {
        return repository.findByFeedbackType(type).stream()
                .map(f -> buildResponse(f, null))
                .collect(Collectors.toList());
    }

    public List<FeedbackResponse> getFeedbacksBySentiment(Feedback.Sentiment sentiment) {
        return repository.findBySentiment(sentiment).stream()
                .map(f -> buildResponse(f, null))
                .collect(Collectors.toList());
    }

    public InsightsResult getInsights() {
        List<Object[]> sentimentCounts = repository.countBySentimentGrouped();
        List<Object[]> topTopics       = repository.topTopics();

        Map<String, Long> sentimentMap = new HashMap<>();
        long total = 0;
        for (Object[] row : sentimentCounts) {
            String key = row[0].toString();
            long count = (Long) row[1];
            sentimentMap.put(key, count);
            total += count;
        }

        double posRate = total > 0 ? (sentimentMap.getOrDefault("POSITIVE", 0L) * 100.0 / total) : 0;
        double negRate = total > 0 ? (sentimentMap.getOrDefault("NEGATIVE", 0L) * 100.0 / total) : 0;

        Map<String, Long> topicsMap = new LinkedHashMap<>();
        for (Object[] row : topTopics.subList(0, Math.min(5, topTopics.size()))) {
            topicsMap.put((String) row[0], (Long) row[1]);
        }

        return InsightsResult.builder()
                .totalFeedbacks((int) total)
                .sentimentDistribution(sentimentMap)
                .positiveRatePct(Math.round(posRate * 10.0) / 10.0)
                .negativeRatePct(Math.round(negRate * 10.0) / 10.0)
                .topTopics(topicsMap)
                .alert(negRate > 40 ? "⚠️ High negative feedback detected! (" + Math.round(negRate) + "%)" : null)
                .build();
    }

    public boolean isNlpServiceHealthy() {
        return nlpClient.isHealthy();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private void enrichFeedbackFromNlp(Feedback feedback, JsonNode nlpResult) {
        try {
            if (nlpResult == null) return;

            // Language
            JsonNode input = nlpResult.path("input");
            if (!input.isMissingNode()) {
                feedback.setLanguage(input.path("language").asText());
            }

            // Sentiment
            JsonNode sentimentNode = nlpResult.path("sentiment");
            if (!sentimentNode.isMissingNode()) {
                String s = sentimentNode.path("sentiment").asText("").toUpperCase();
                try { feedback.setSentiment(Feedback.Sentiment.valueOf(s)); } catch (Exception ignored) {}
                feedback.setSentimentConfidence(sentimentNode.path("confidence").asDouble());
            }

            // Topic
            JsonNode topicNode = nlpResult.path("topic");
            if (!topicNode.isMissingNode()) {
                feedback.setPrimaryTopic(topicNode.path("primary_topic").asText());
                feedback.setTopicConfidence(topicNode.path("confidence").asDouble());
            }

            // Keyphrases (store as JSON string)
            JsonNode kpNode = nlpResult.path("keyphrases");
            if (!kpNode.isMissingNode()) {
                feedback.setKeyphrases(mapper.writeValueAsString(kpNode.path("keyphrases")));
            }

            // Summary
            JsonNode summaryNode = nlpResult.path("summary");
            if (!summaryNode.isMissingNode()) {
                feedback.setSummary(summaryNode.path("summary").asText());
            }

            // Full raw result
            feedback.setRawNlpResult(mapper.writeValueAsString(nlpResult));

        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize NLP result: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private FeedbackResponse buildResponse(Feedback f, JsonNode nlpResult) {
        SentimentResult sentimentResult = null;
        TopicResult topicResult = null;
        KeyphrasesResult kpResult = null;
        SummaryResult summaryResult = null;

        if (nlpResult != null) {
            // Build rich response from NLP node
            JsonNode s = nlpResult.path("sentiment");
            if (!s.isMissingNode()) {
                sentimentResult = SentimentResult.builder()
                        .sentiment(s.path("sentiment").asText())
                        .confidence(s.path("confidence").asDouble())
                        .emoji(s.path("emoji").asText())
                        .language(s.path("language").asText())
                        .amharicTranslated(s.path("amharic_translated").asBoolean())
                        .build();
            }
            JsonNode t = nlpResult.path("topic");
            if (!t.isMissingNode()) {
                topicResult = TopicResult.builder()
                        .primaryTopic(t.path("primary_topic").asText())
                        .confidence(t.path("confidence").asDouble())
                        .feedbackType(t.path("feedback_type").asText())
                        .build();
            }
            JsonNode kp = nlpResult.path("keyphrases");
            if (!kp.isMissingNode()) {
                try {
                    List<Map<String, Object>> phrases = mapper.convertValue(
                            kp.path("keyphrases"),
                            mapper.getTypeFactory().constructCollectionType(List.class, Map.class)
                    );
                    kpResult = KeyphrasesResult.builder().keyphrases(phrases).build();
                } catch (Exception ignored) {}
            }
            JsonNode sm = nlpResult.path("summary");
            if (!sm.isMissingNode()) {
                summaryResult = SummaryResult.builder()
                        .summary(sm.path("summary").asText())
                        .note(sm.path("note").asText(null))
                        .build();
            }
        } else {
            // Build from stored entity fields
            if (f.getSentiment() != null) {
                sentimentResult = SentimentResult.builder()
                        .sentiment(f.getSentiment().name().toLowerCase())
                        .confidence(f.getSentimentConfidence())
                        .build();
            }
            if (f.getPrimaryTopic() != null) {
                topicResult = TopicResult.builder()
                        .primaryTopic(f.getPrimaryTopic())
                        .confidence(f.getTopicConfidence())
                        .build();
            }
            if (f.getSummary() != null) {
                summaryResult = SummaryResult.builder().summary(f.getSummary()).build();
            }
        }

        return FeedbackResponse.builder()
                .id(f.getId())
                .text(f.getText())
                .feedbackType(f.getFeedbackType() != null ? f.getFeedbackType().name() : null)
                .source(f.getSource())
                .language(f.getLanguage())
                .sentiment(sentimentResult)
                .topic(topicResult)
                .keyphrases(kpResult)
                .summary(summaryResult)
                .analysisStatus(f.getAnalysisStatus())
                .createdAt(f.getCreatedAt())
                .analyzedAt(f.getAnalyzedAt())
                .build();
    }

    @SuppressWarnings("unchecked")
    private InsightsResult mapInsights(JsonNode node) {
        if (node == null || node.isNull()) return InsightsResult.builder().build();
        try {
            Map<String, Long> sentimentDist = mapper.convertValue(
                    node.path("sentiment_distribution"),
                    mapper.getTypeFactory().constructMapType(Map.class, String.class, Long.class)
            );
            Map<String, Long> topTopics = mapper.convertValue(
                    node.path("top_topics"),
                    mapper.getTypeFactory().constructMapType(Map.class, String.class, Long.class)
            );
            return InsightsResult.builder()
                    .totalFeedbacks(node.path("total_feedbacks").asInt())
                    .sentimentDistribution(sentimentDist)
                    .positiveRatePct(node.path("positive_rate_pct").asDouble())
                    .negativeRatePct(node.path("negative_rate_pct").asDouble())
                    .topTopics(topTopics)
                    .alert(node.path("alert").asText(null))
                    .build();
        } catch (Exception e) {
            log.warn("Failed to map insights: {}", e.getMessage());
            return InsightsResult.builder().build();
        }
    }
}
