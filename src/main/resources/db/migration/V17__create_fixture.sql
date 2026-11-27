CREATE TABLE fixture (
                         fixture_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         api_football_id BIGINT NOT NULL,

                         home_team_id BIGINT NOT NULL,
                         away_team_id BIGINT NOT NULL,

                         league_code VARCHAR(50) NOT NULL,
                         season INT NOT NULL,
                         round VARCHAR(255) NOT NULL,

                         kickoff_at DATETIME(6) NOT NULL,
                         status VARCHAR(50) NOT NULL,

                         elapsed INT,
                         extra INT,

                         home_score INT,
                         away_score INT,

                         venue_name VARCHAR(255),
                         updated_at DATETIME(6) NOT NULL,

                         CONSTRAINT uk_fixture_api_football_id
                             UNIQUE (api_football_id),

                         CONSTRAINT fk_fixture_home_team
                             FOREIGN KEY (home_team_id) REFERENCES team (team_id),

                         CONSTRAINT fk_fixture_away_team
                             FOREIGN KEY (away_team_id) REFERENCES team (team_id)
);