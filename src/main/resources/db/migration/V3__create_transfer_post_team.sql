CREATE TABLE transfer_post_team (
    transfer_post_team_id BIGINT NOT NULL AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,

    PRIMARY KEY (transfer_post_team_id),

    CONSTRAINT uk_transfer_post_team UNIQUE (team_id, post_id),

    INDEX idx_transfer_post_team_post (post_id),

    CONSTRAINT fk_transfer_post_team_team FOREIGN KEY (team_id) REFERENCES team(team_id),
    CONSTRAINT fk_transfer_post_team_post FOREIGN KEY (post_id) REFERENCES transfer_post(transfer_post_id)
);