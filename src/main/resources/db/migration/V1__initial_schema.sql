CREATE TABLE IF NOT EXISTS system_config (
    id VARCHAR(50) PRIMARY KEY,
    striim_url VARCHAR(255) NOT NULL,
    striim_token_enc TEXT NOT NULL,
    splunk_hec_url VARCHAR(255) NOT NULL,
    splunk_token_enc TEXT NOT NULL,
    splunk_index VARCHAR(100) NOT NULL,
    interval_seconds INTEGER DEFAULT 60,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS execution_history (
    execution_id VARCHAR(50) PRIMARY KEY,
    trigger_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    metrics_collected_count INTEGER,
    error_message TEXT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    published_to_splunk BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_execution_start_time ON execution_history(start_time DESC);
CREATE INDEX idx_execution_status ON execution_history(status);
