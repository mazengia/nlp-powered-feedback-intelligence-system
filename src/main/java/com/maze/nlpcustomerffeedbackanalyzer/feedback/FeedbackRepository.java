package com.maze.nlpcustomerffeedbackanalyzer.feedback;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByFeedbackType(Feedback.FeedbackType feedbackType);

    List<Feedback> findBySentiment(Feedback.Sentiment sentiment);

    List<Feedback> findByAnalysisStatus(Feedback.AnalysisStatus status);

    List<Feedback> findBySource(String source);

    List<Feedback> findByLanguage(String language);

    long countBySentiment(Feedback.Sentiment sentiment);

    long countByFeedbackType(Feedback.FeedbackType feedbackType);

    @Query("SELECT f.sentiment, COUNT(f) FROM Feedback f GROUP BY f.sentiment")
    List<Object[]> countBySentimentGrouped();

    @Query("SELECT f.primaryTopic, COUNT(f) FROM Feedback f WHERE f.primaryTopic IS NOT NULL GROUP BY f.primaryTopic ORDER BY COUNT(f) DESC")
    List<Object[]> topTopics();
}
