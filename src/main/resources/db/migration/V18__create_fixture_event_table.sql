CREATE TABLE fixture_event
(
    fixture_event_id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    fixture_id        BIGINT       NOT NULL,
    team_id           BIGINT       NOT NULL,
    player_id         BIGINT       NULL,
    related_player_id BIGINT       NULL,
    elapsed           INT          NOT NULL,
    extra             INT          NULL,
    type              ENUM (
        'GOAL',
        'CARD',
        'SUBSTITUTION',
        'VAR'
    ) NOT NULL,
    detail            VARCHAR(255) NOT NULL,
    comments          VARCHAR(255) NULL,

    CONSTRAINT fk_fixture_event_fixture
        FOREIGN KEY (fixture_id)
            REFERENCES fixture (fixture_id),

    CONSTRAINT fk_fixture_event_team
        FOREIGN KEY (team_id)
            REFERENCES team (team_id),

    CONSTRAINT fk_fixture_event_player
        FOREIGN KEY (player_id)
            REFERENCES player (player_id),

    CONSTRAINT fk_fixture_event_related_player
        FOREIGN KEY (related_player_id)
            REFERENCES player (player_id),

    INDEX idx_fixture_event_timeline (
        fixture_id,
        elapsed,
        extra,
        fixture_event_id
    )
);