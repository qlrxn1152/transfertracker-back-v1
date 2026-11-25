CREATE INDEX idx_post_sync_status_step_status_updated
    ON post_sync_status (current_step, current_step_status, updated_at);
