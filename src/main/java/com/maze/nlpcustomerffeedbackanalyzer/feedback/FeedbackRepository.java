package com.maze.nlpcustomerffeedbackanalyzer.feedback;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Page<Feedback> findByFeedbackType(Feedback.FeedbackType feedbackType, Pageable pageable);

    Page<Feedback> findBySentiment(Feedback.Sentiment sentiment, Pageable pageable);

    List<Feedback> findByAnalysisStatus(Feedback.AnalysisStatus status);

    Page<Feedback> findBySource(String source, Pageable pageable);

    Page<Feedback> findByLanguage(String language, Pageable pageable);

    long countBySentiment(Feedback.Sentiment sentiment);

    long countByFeedbackType(Feedback.FeedbackType feedbackType);

    @Query("SELECT f.sentiment, COUNT(f) FROM Feedback f GROUP BY f.sentiment")
    List<Object[]> countBySentimentGrouped();

    @Query("""
           SELECT f.primaryTopic, COUNT(f)
             FROM Feedback f
            WHERE f.primaryTopic IS NOT NULL
            GROUP BY f.primaryTopic
            ORDER BY COUNT(f) DESC
           """)
    List<Object[]> topTopics();
}