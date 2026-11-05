-- Player
INSERT INTO player (player_name, api_football_id)
VALUES
    ('Lionel Messi', 910111),
    ('Cristiano Ronaldo', 910222),
    ('Neymar Junior', 910333);

-- Team
INSERT INTO team (team_name, api_football_id, league_api_code)
VALUES
    ('Barcelona', 910044, 'LA_LIGA'),
    ('PSG', 910055, 'LIGUE_1'),
    ('Real Madrid', 910066, 'LA_LIGA'),
    ('Arsenal', 910077, 'EPL'),
    ('Chelsea', 910088, 'EPL');

-- A: Messi / Barcelona <- PSG / 포함
INSERT INTO transfer (
    player_id, in_team_id, out_team_id, transfer_type, transfer_date
)
SELECT p.player_id, tin.team_id, tout.team_id, 'Free', '2026-01-01'
FROM player p
         JOIN team tin ON tin.api_football_id = 910044
         JOIN team tout ON tout.api_football_id = 910055
WHERE p.api_football_id = 910111;

-- B: Messi / Arsenal <- Chelsea / 제외 (팀, 리그 불일치)
INSERT INTO transfer (
    player_id, in_team_id, out_team_id, transfer_type, transfer_date
)
SELECT p.player_id, tin.team_id, tout.team_id, 'Loan', '2026-02-01'
FROM player p
         JOIN team tin ON tin.api_football_id = 910077
         JOIN team tout ON tout.api_football_id = 910088
WHERE p.api_football_id = 910111;

-- C: Ronaldo / Barcelona <- PSG / 제외 (선수 불일치)
INSERT INTO transfer (
    player_id, in_team_id, out_team_id, transfer_type, transfer_date
)
SELECT p.player_id, tin.team_id, tout.team_id, 'Permanent', '2026-03-01'
FROM player p
         JOIN team tin ON tin.api_football_id = 910044
         JOIN team tout ON tout.api_football_id = 910055
WHERE p.api_football_id = 910222;

-- D: Messi / Real Madrid <- PSG / 제외 (팀 불일치)
INSERT INTO transfer (
    player_id, in_team_id, out_team_id, transfer_type, transfer_date
)
SELECT p.player_id, tin.team_id, tout.team_id, 'Free', '2026-04-01'
FROM player p
         JOIN team tin ON tin.api_football_id = 910066
         JOIN team tout ON tout.api_football_id = 910055
WHERE p.api_football_id = 910111;

-- E: Neymar / Arsenal <- Chelsea / 제외
INSERT INTO transfer (
    player_id, in_team_id, out_team_id, transfer_type, transfer_date
)
SELECT p.player_id, tin.team_id, tout.team_id, 'Loan', '2026-05-01'
FROM player p
         JOIN team tin ON tin.api_football_id = 910077
         JOIN team tout ON tout.api_football_id = 910088
WHERE p.api_football_id = 910333;

-- F: Messi / PSG <- Barcelona / 포함
INSERT INTO transfer (
    player_id, in_team_id, out_team_id, transfer_type, transfer_date
)
SELECT p.player_id, tin.team_id, tout.team_id, 'Permanent', '2026-06-01'
FROM player p
         JOIN team tin ON tin.api_football_id = 910055
         JOIN team tout ON tout.api_football_id = 910044
WHERE p.api_football_id = 910111;