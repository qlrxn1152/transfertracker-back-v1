CREATE TABLE competition_season (
                                    competition_season_id BIGINT NOT NULL AUTO_INCREMENT,

                                    league_code ENUM(
        'EPL',
        'LA_LIGA',
        'BUNDESLIGA',
        'SERIE_A',
        'LIGUE_1',
        'K_LEAGUE',
        'CHAMPIONS_LEAGUE',
        'EUROPA_LEAGUE',
        'CONFERENCE_LEAGUE',
        'FA_CUP',
        'EFL_CUP',
        'WORLD_CUP'
    ) NOT NULL,

                                    season INT NOT NULL,

                                    start_date DATE NULL,
                                    end_date DATE NULL,

                                    current_season BOOLEAN NOT NULL DEFAULT FALSE,

                                    supports_events BOOLEAN NOT NULL DEFAULT FALSE,
                                    supports_lineups BOOLEAN NOT NULL DEFAULT FALSE,

                                    supports_fixture_statistics BOOLEAN NOT NULL DEFAULT FALSE,
                                    supports_player_statistics BOOLEAN NOT NULL DEFAULT FALSE,

                                    supports_standings BOOLEAN NOT NULL DEFAULT FALSE,
                                    supports_players BOOLEAN NOT NULL DEFAULT FALSE,

                                    supports_top_scorers BOOLEAN NOT NULL DEFAULT FALSE,
                                    supports_top_assists BOOLEAN NOT NULL DEFAULT FALSE,
                                    supports_top_cards BOOLEAN NOT NULL DEFAULT FALSE,

                                    supports_injuries BOOLEAN NOT NULL DEFAULT FALSE,

                                    updated_at DATETIME(6) NOT NULL,

                                    PRIMARY KEY (competition_season_id),

                                    CONSTRAINT uk_competition_season_league_season
                                        UNIQUE (league_code, season)
);