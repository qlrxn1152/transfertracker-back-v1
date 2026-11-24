CREATE TABLE post_sync_status (
                                  post_sync_status_id BIGINT NOT NULL AUTO_INCREMENT,
                                  post_id BIGINT NOT NULL,

                                  current_step ENUM(
        'ASSIGN',
        'TRANSLATE',
        'NOTIFICATION'
    ) NOT NULL,

                                  current_step_status ENUM(
        'PENDING',
        'PROCESSING',
        'COMPLETED',
        'FAILED'
    ) NOT NULL,

                                  retry_count INT NOT NULL DEFAULT 0,
                                  error_message VARCHAR(255) NULL,

                                  created_at DATETIME(6) NOT NULL,
                                  updated_at DATETIME(6) NOT NULL,

                                  PRIMARY KEY (post_sync_status_id),

                                  CONSTRAINT uk_post_sync_status_post_id
                                      UNIQUE (post_id),

                                  CONSTRAINT fk_post_sync_status_post
                                      FOREIGN KEY (post_id)
                                          REFERENCES post (post_id)
);