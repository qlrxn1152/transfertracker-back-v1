CREATE TABLE league_standing (
                                 league_standing_id BIGINT NOT NULL AUTO_INCREMENT,
                                 team_id BIGINT NOT NULL,

                                 league_code ENUM(
        'EPL',
        'LA_LIGA',
        'BUNDESLIGA',
        'SERIE_A',
        'LIGUE_1',
        'K_LEAGUE'
    ) NOT NULL,

                                 season INT NOT NULL,
                                 group_name VARCHAR(255) NOT NULL,

                                 ranking INT NOT NULL,
                                 points INT NOT NULL,
                                 goals_diff INT NOT NULL,

                                 form VARCHAR(255) NULL,
                                 status VARCHAR(255) NULL,
                                 description VARCHAR(255) NULL,

                                 all_played INT NULL,
                                 all_win INT NULL,
                                 all_draw INT NULL,
                                 all_lose INT NULL,
                                 all_goals_for INT NULL,
                                 all_goals_against INT NULL,

                                 home_played INT NULL,
                                 home_win INT NULL,
                                 home_draw INT NULL,
                                 home_lose INT NULL,
                                 home_goals_for INT NULL,
                                 home_goals_against INT NULL,

                                 away_played INT NULL,
                                 away_win INT NULL,
                                 away_draw INT NULL,
                                 away_lose INT NULL,
                                 away_goals_for INT NULL,
                                 away_goals_against INT NULL,

                                 source_updated_at DATETIME(6) NULL,
                                 updated_at DATETIME(6) NOT NULL,

                                 PRIMARY KEY (league_standing_id),

                                 CONSTRAINT uk_league_standing_league_season_group_team
                                     UNIQUE (league_code, season, group_name, team_id),

                                 CONSTRAINT fk_league_standing_team
                                     FOREIGN KEY (team_id) REFERENCES team (team_id)
);