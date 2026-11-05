-- 선수 2명
INSERT INTO player (player_name, api_football_id)
VALUES
    ('Lionel Messi', 920111),
    ('Cristiano Ronaldo', 920222);

-- 팀 4개
INSERT INTO team (team_name, api_football_id, league_api_code)
VALUES
    ('Barcelona', 920044, 'LA_LIGA'),
    ('Arsenal', 920055, 'EPL'),
    ('Real Madrid', 920066, 'LA_LIGA'),
    ('PSG', 920077, 'LIGUE_1');

-- A: Messi / Arsenal -> Barcelona
-- 팀 조건: Barcelona
-- 리그 조건: Arsenal(EPL)
-- 예상: 포함
INSERT INTO transfer (
    player_id,
    in_team_id,
    out_team_id,
    transfer_type,
    transfer_date
)
SELECT
    p.player_id,
    tin.team_id,
    tout.team_id,
    'Permanent',
    '2026-07-01'
FROM player p
         JOIN team tin ON tin.api_football_id = 920044
         JOIN team tout ON tout.api_football_id = 920055
WHERE p.api_football_id = 920111;

-- B: Ronaldo / Arsenal -> Barcelona
-- 선수 이름 불일치
-- 예상: 제외
INSERT INTO transfer (
    player_id,
    in_team_id,
    out_team_id,
    transfer_type,
    transfer_date
)
SELECT
    p.player_id,
    tin.team_id,
    tout.team_id,
    'Loan',
    '2026-06-01'
FROM player p
         JOIN team tin ON tin.api_football_id = 920044
         JOIN team tout ON tout.api_football_id = 920055
WHERE p.api_football_id = 920222;

-- C: Messi / PSG -> Real Madrid
-- Barcelona, EPL 조건 모두 불일치
-- 예상: 제외
INSERT INTO transfer (
    player_id,
    in_team_id,
    out_team_id,
    transfer_type,
    transfer_date
)
SELECT
    p.player_id,
    tin.team_id,
    tout.team_id,
    'Free',
    '2026-05-01'
FROM player p
         JOIN team tin ON tin.api_football_id = 920066
         JOIN team tout ON tout.api_football_id = 920077
WHERE p.api_football_id = 920111;