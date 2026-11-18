CREATE TABLE transfer_post_player (
    transfer_post_player_id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    transfer_post_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,

    PRIMARY KEY (transfer_post_player_id),

    CONSTRAINT uk_transfer_post_player_player_post UNIQUE (player_id, transfer_post_id),

    INDEX idx_transfer_post_player_post (transfer_post_id),

    CONSTRAINT fk_transfer_post_player_player FOREIGN KEY (player_id) REFERENCES player(player_id),
    CONSTRAINT fk_transfer_post_player_post FOREIGN KEY (transfer_post_id) REFERENCES transfer_post(transfer_post_id)
);