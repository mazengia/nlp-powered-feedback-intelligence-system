-- Initialize PostgreSQL database for feedback analyzer

-- Create schema
CREATE SCHEMA IF NOT EXISTS feedback_analyzer;

-- Create tables
CREATE TABLE IF NOT EXISTS feedbacks (
    id BIGSERIAL PRIMARY KEY,
    text TEXT NOT NULL,
    feedback_type VARCHAR(50) NOT NULL,
    source VARCHAR(100),
    language VARCHAR(10),
    sentiment VARCHAR(20),
    sentiment_confidence DOUBLE PRECISION,
    primary_topic VARCHAR(200),
    topic_confidence DOUBLE PRECISION,
    keyphrases TEXT,
    summary TEXT,
    raw_nlp_result TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    analyzed_at TIMESTAMP,
    analysis_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_by VARCHAR(100),
    modified_by VARCHAR(100),
    modified_at TIMESTAMP
);

CREATE INDEX idx_sentiment ON feedbacks(sentiment);
CREATE INDEX idx_feedback_type ON feedbacks(feedback_type);
CREATE INDEX idx_analysis_status ON feedbacks(analysis_status);
CREATE INDEX idx_created_at ON feedbacks(created_at);

-- Create audit logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    operation VARCHAR(20) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    changed_fields VARCHAR(200),
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(50),
    details VARCHAR(500)
);

CREATE INDEX idx_entity_type ON audit_logs(entity_type);
CREATE INDEX idx_entity_id ON audit_logs(entity_id);
CREATE INDEX idx_operation ON audit_logs(operation);
CREATE INDEX idx_audit_created_at ON audit_logs(created_at);

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA feedback_analyzer TO feedback_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA feedback_analyzer TO feedback_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA feedback_analyzer TO feedback_user;

