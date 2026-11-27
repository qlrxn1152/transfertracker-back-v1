CREATE TABLE fixture_lineup (
                                fixture_lineup_id BIGINT NOT NULL AUTO_INCREMENT,
                                fixture_id BIGINT NOT NULL,
                                team_id BIGINT NOT NULL,
                                formation VARCHAR(255) NULL,

                                PRIMARY KEY (fixture_lineup_id),

                                CONSTRAINT uk_fixture_lineup_fixture_team
                                    UNIQUE (fixture_id, team_id),

                                CONSTRAINT fk_fixture_lineup_fixture
                                    FOREIGN KEY (fixture_id) REFERENCES fixture (fixture_id),

                                CONSTRAINT fk_fixture_lineup_team
                                    FOREIGN KEY (team_id) REFERENCES team (team_id)
);

CREATE TABLE fixture_lineup_player (
                                       fixture_lineup_player_id BIGINT NOT NULL AUTO_INCREMENT,
                                       fixture_lineup_id BIGINT NOT NULL,
                                       player_id BIGINT NOT NULL,
                                       number INT NULL,
                                       position VARCHAR(255) NULL,
                                       grid VARCHAR(255) NULL,
                                       fixture_lineup_player_status ENUM('STARTER', 'SUBSTITUTE') NOT NULL,

                                       PRIMARY KEY (fixture_lineup_player_id),

                                       CONSTRAINT uk_fixture_lineup_player_lineup_player
                                           UNIQUE (fixture_lineup_id, player_id),

                                       CONSTRAINT fk_fixture_lineup_player_lineup
                                           FOREIGN KEY (fixture_lineup_id)
                                               REFERENCES fixture_lineup (fixture_lineup_id),

                                       CONSTRAINT fk_fixture_lineup_player_player
                                           FOREIGN KEY (player_id)
                                               REFERENCES player (player_id)
);