-- VEYRO team-posts baseline seed (performance DB only)
--
-- Reuses the existing 1,000,000 posts without changing post data.  The three
-- synthetic teams deliberately create a 700k / 200k / 50k post distribution
-- (950k post_team rows in total) for comparative, non-production load tests.
--
-- Run only against the veyro_performance database.  Every insert is
-- idempotent: a re-run preserves existing seeded relations instead of deleting
-- or recreating data.

INSERT INTO team (team_name, api_football_id, league_api_code, team_name_ko)
SELECT 'VEYRO Performance High FC', 990000001, 'EPL', '베이로 성능 대량 FC'
WHERE NOT EXISTS (
    SELECT 1 FROM team WHERE api_football_id = 990000001
);

INSERT INTO team (team_name, api_football_id, league_api_code, team_name_ko)
SELECT 'VEYRO Performance Medium FC', 990000002, 'EPL', '베이로 성능 중간 FC'
WHERE NOT EXISTS (
    SELECT 1 FROM team WHERE api_football_id = 990000002
);

INSERT INTO team (team_name, api_football_id, league_api_code, team_name_ko)
SELECT 'VEYRO Performance Low FC', 990000003, 'EPL', '베이로 성능 소량 FC'
WHERE NOT EXISTS (
    SELECT 1 FROM team WHERE api_football_id = 990000003
);

INSERT INTO player (player_name, api_football_id, player_name_ko)
SELECT 'VEYRO Performance High Player', 991000001, '베이로 성능 대량 선수'
WHERE NOT EXISTS (
    SELECT 1 FROM player WHERE api_football_id = 991000001
);

INSERT INTO player (player_name, api_football_id, player_name_ko)
SELECT 'VEYRO Performance Medium Player', 991000002, '베이로 성능 중간 선수'
WHERE NOT EXISTS (
    SELECT 1 FROM player WHERE api_football_id = 991000002
);

INSERT INTO player (player_name, api_football_id, player_name_ko)
SELECT 'VEYRO Performance Low Player', 991000003, '베이로 성능 소량 선수'
WHERE NOT EXISTS (
    SELECT 1 FROM player WHERE api_football_id = 991000003
);

-- Keep team/player detail relations coherent in the performance DB too.
INSERT IGNORE INTO team_player (team_id, player_id)
SELECT team.team_id, player.player_id
FROM team
JOIN player
WHERE (team.api_football_id, player.api_football_id) IN (
    (990000001, 991000001),
    (990000002, 991000002),
    (990000003, 991000003)
);

-- The primary-key order makes each range deterministic even if post IDs are
-- not contiguous.  INSERT IGNORE only suppresses the unique (team_id, post_id)
-- duplicates produced when this script is intentionally re-run.
INSERT IGNORE INTO post_team (team_id, post_id, created_at)
SELECT team.team_id, selected.post_id, NOW(6)
FROM team
JOIN (
    SELECT post_id FROM post ORDER BY post_id LIMIT 700000
) AS selected
WHERE team.api_football_id = 990000001;

INSERT IGNORE INTO post_team (team_id, post_id, created_at)
SELECT team.team_id, selected.post_id, NOW(6)
FROM team
JOIN (
    SELECT post_id FROM post ORDER BY post_id LIMIT 200000 OFFSET 700000
) AS selected
WHERE team.api_football_id = 990000002;

INSERT IGNORE INTO post_team (team_id, post_id, created_at)
SELECT team.team_id, selected.post_id, NOW(6)
FROM team
JOIN (
    SELECT post_id FROM post ORDER BY post_id LIMIT 50000 OFFSET 900000
) AS selected
WHERE team.api_football_id = 990000003;

-- Mirror the 700k / 200k / 50k distribution for post_player so player-post
-- endpoints have representative relation data as well (950k rows in total).
INSERT IGNORE INTO post_player (player_id, post_id, created_at)
SELECT player.player_id, selected.post_id, NOW(6)
FROM player
JOIN (
    SELECT post_id FROM post ORDER BY post_id LIMIT 700000
) AS selected
WHERE player.api_football_id = 991000001;

INSERT IGNORE INTO post_player (player_id, post_id, created_at)
SELECT player.player_id, selected.post_id, NOW(6)
FROM player
JOIN (
    SELECT post_id FROM post ORDER BY post_id LIMIT 200000 OFFSET 700000
) AS selected
WHERE player.api_football_id = 991000002;

INSERT IGNORE INTO post_player (player_id, post_id, created_at)
SELECT player.player_id, selected.post_id, NOW(6)
FROM player
JOIN (
    SELECT post_id FROM post ORDER BY post_id LIMIT 50000 OFFSET 900000
) AS selected
WHERE player.api_football_id = 991000003;

-- Verification query. Expected distribution after the first successful run:
-- High 700000, Medium 200000, Low 50000 (total 950000).
SELECT
    team.team_id,
    team.team_name,
    team.team_name_ko,
    COUNT(post_team.post_team_id) AS post_count
FROM team
LEFT JOIN post_team ON post_team.team_id = team.team_id
WHERE team.api_football_id IN (990000001, 990000002, 990000003)
GROUP BY team.team_id, team.team_name, team.team_name_ko
ORDER BY post_count DESC;

SELECT
    player.player_id,
    player.player_name,
    player.player_name_ko,
    COUNT(post_player.post_player_id) AS post_count
FROM player
LEFT JOIN post_player ON post_player.player_id = player.player_id
WHERE player.api_football_id IN (991000001, 991000002, 991000003)
GROUP BY player.player_id, player.player_name, player.player_name_ko
ORDER BY post_count DESC;
