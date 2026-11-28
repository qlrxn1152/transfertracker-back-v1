ALTER TABLE league_standing
    MODIFY COLUMN league_code ENUM(
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
    ) NOT NULL;

ALTER TABLE player_ranking
    MODIFY COLUMN league_code ENUM(
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
    ) NOT NULL;

ALTER TABLE player_season_statistics
    MODIFY COLUMN league_code ENUM(
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
    ) NOT NULL;