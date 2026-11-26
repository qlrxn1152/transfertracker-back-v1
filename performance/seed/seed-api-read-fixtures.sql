-- VEYRO public read API fixtures (performance DB only)
--
-- Prerequisite: seed-posts.sql, seed-posts-extend.sql and
-- seed-team-posts.sql. This file never deletes data and is safe to re-run.
-- It adds representative transfer rows for the synthetic teams/players that
-- are already used by post/team/player performance scenarios.

-- Domain-list fixtures: 600 teams (100 per supported league), 15,000 players
-- (25 per team), and 300,000 transfer histories (20 per player). These are
-- intentionally separate from the 3 high/medium/low post-relation fixtures:
-- the latter retain their 700k/200k/50k distribution for post endpoint tests.
INSERT IGNORE INTO team (
    team_name,
    api_football_id,
    league_api_code,
    team_name_ko
)
WITH RECURSIVE numbers AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM numbers WHERE n < 600
)
SELECT /*+ SET_VAR(cte_max_recursion_depth = 1000) */
    CONCAT('VEYRO Performance ', ELT(MOD(n - 1, 6) + 1, 'EPL', 'LaLiga', 'Bundesliga', 'SerieA', 'Ligue1', 'KLeague'), ' Team ', LPAD(CEIL(n / 6), 3, '0')),
    992000000 + n,
    ELT(MOD(n - 1, 6) + 1, 'EPL', 'LA_LIGA', 'BUNDESLIGA', 'SERIE_A', 'LIGUE_1', 'K_LEAGUE'),
    CONCAT('베이로 성능 ', ELT(MOD(n - 1, 6) + 1, 'EPL', '라리가', '분데스리가', '세리에A', '리그1', 'K리그'), ' 팀 ', LPAD(CEIL(n / 6), 3, '0'))
FROM numbers;

INSERT IGNORE INTO player (
    player_name,
    api_football_id,
    player_name_ko
)
WITH RECURSIVE numbers AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM numbers WHERE n < 15000
)
SELECT /*+ SET_VAR(cte_max_recursion_depth = 20000) */
    CONCAT('VEYRO Performance Player ', LPAD(n, 5, '0')),
    993000000 + n,
    CONCAT('베이로 성능 선수 ', LPAD(n, 5, '0'))
FROM numbers;

INSERT IGNORE INTO team_player (team_id, player_id)
SELECT
    team.team_id,
    player.player_id
FROM player
JOIN team ON team.api_football_id = 992000000 + CEIL((player.api_football_id - 993000000) / 25)
WHERE player.api_football_id BETWEEN 993000001 AND 993015000;

INSERT IGNORE INTO transfer (
    player_id,
    in_team_id,
    out_team_id,
    transfer_type,
    transfer_date
)
WITH RECURSIVE numbers AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM numbers WHERE n < 300000
)
SELECT /*+ SET_VAR(cte_max_recursion_depth = 300000) */
    player.player_id,
    in_team.team_id,
    out_team.team_id,
    ELT(MOD(numbers.n - 1, 3) + 1, 'PERMANENT', 'LOAN', 'FREE_TRANSFER'),
    DATE_SUB('2026-11-01', INTERVAL MOD(numbers.n - 1, 3650) DAY)
FROM numbers
JOIN player ON player.api_football_id = 993000000 + CEIL(numbers.n / 20)
JOIN team AS out_team ON out_team.api_football_id = 992000000 + CEIL((player.api_football_id - 993000000) / 25)
JOIN team AS in_team ON in_team.api_football_id = 992000000 + MOD(
    (CEIL((player.api_football_id - 993000000) / 25) - 1) + MOD(numbers.n - 1, 599) + 1,
    600
) + 1;

INSERT IGNORE INTO transfer (
    player_id,
    in_team_id,
    out_team_id,
    transfer_type,
    transfer_date
)
SELECT
    player.player_id,
    in_team.team_id,
    out_team.team_id,
    'PERMANENT',
    '2026-07-01'
FROM player
JOIN team AS in_team ON in_team.api_football_id = 990000001
JOIN team AS out_team ON out_team.api_football_id = 990000002
WHERE player.api_football_id = 991000001;

INSERT IGNORE INTO transfer (
    player_id,
    in_team_id,
    out_team_id,
    transfer_type,
    transfer_date
)
SELECT
    player.player_id,
    in_team.team_id,
    out_team.team_id,
    'LOAN',
    '2026-07-02'
FROM player
JOIN team AS in_team ON in_team.api_football_id = 990000002
JOIN team AS out_team ON out_team.api_football_id = 990000003
WHERE player.api_football_id = 991000002;

-- Copy the returned values into the local environment only, for example:
-- TEAM_ID=... PLAYER_ID=... POST_ID=... TRANSFER_ID=... ./performance/run-k6.sh all-read
SELECT 'TEAM_ID' AS variable_name, team_id AS value, team_name
FROM team
WHERE api_football_id = 990000001;

SELECT 'PLAYER_ID' AS variable_name, player_id AS value, player_name
FROM player
WHERE api_football_id = 991000001;

SELECT 'POST_ID' AS variable_name, post_id AS value, journalist_name
FROM post
WHERE external_post_id = 'PERF_POST_1';

SELECT 'TRANSFER_ID' AS variable_name, transfer.transfer_id AS value, player.player_name
FROM transfer
JOIN player ON player.player_id = transfer.player_id
WHERE player.api_football_id = 991000001
  AND transfer.transfer_date = '2026-07-01';
