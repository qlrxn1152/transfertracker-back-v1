-- Player
INSERT INTO player (player_name, api_football_id)
VALUES
    ('Lionel Messi', 111),
    ('Cristiano Ronaldo', 222),
    ('Neymar Junior', 333);

-- Team
INSERT INTO team (team_name, api_football_id)
VALUES
    ('Barcelona', 44),
    ('PSG', 55),
    ('Inter Miami', 66);

-- Transfer: Messi
INSERT INTO transfer (
    player_id, in_team_id, out_team_id,
    transfer_type, transfer_date
)
SELECT
    p.player_id,
    tin.team_id,
    tout.team_id,
    'Free',
    '2026-07-01'
FROM player p
         JOIN team tin ON tin.api_football_id = 66
         JOIN team tout ON tout.api_football_id = 55
WHERE p.api_football_id = 111;

-- Transfer: Ronaldo
INSERT INTO transfer (
    player_id, in_team_id, out_team_id,
    transfer_type, transfer_date
)
SELECT
    p.player_id,
    tin.team_id,
    tout.team_id,
    'Permanent',
    '2026-06-01'
FROM player p
         JOIN team tin ON tin.api_football_id = 55
         JOIN team tout ON tout.api_football_id = 44
WHERE p.api_football_id = 222;

-- Transfer: Neymar
INSERT INTO transfer (
    player_id, in_team_id, out_team_id,
    transfer_type, transfer_date
)
SELECT
    p.player_id,
    tin.team_id,
    tout.team_id,
    'Loan',
    '2026-05-01'
FROM player p
         JOIN team tin ON tin.api_football_id = 44
         JOIN team tout ON tout.api_football_id = 55
WHERE p.api_football_id = 333;