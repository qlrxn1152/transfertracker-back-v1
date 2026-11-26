-- ============================================================
-- VEYRO Performance Test Seed
-- 성능 테스트 전용 DB: veyro_performance
--
-- Post: 1,000,000건 (기존 데이터 사용)
-- PostTeam: 950,000건
-- PostPlayer: 950,000건
--
-- High   : 700,000건
-- Medium : 200,000건
-- Low    :  50,000건
-- ============================================================


-- ============================================================
-- 1. Team 생성
-- ============================================================

INSERT INTO team (
    team_name,
    api_football_id,
    league_api_code,
    team_name_ko
)
SELECT
    'VEYRO Performance High FC',
    990000001,
    'EPL',
    '베이로 성능 대량 FC'
    WHERE NOT EXISTS (
    SELECT 1
    FROM team
    WHERE api_football_id = 990000001
);


INSERT INTO team (
    team_name,
    api_football_id,
    league_api_code,
    team_name_ko
)
SELECT
    'VEYRO Performance Medium FC',
    990000002,
    'EPL',
    '베이로 성능 중간 FC'
    WHERE NOT EXISTS (
    SELECT 1
    FROM team
    WHERE api_football_id = 990000002
);


INSERT INTO team (
    team_name,
    api_football_id,
    league_api_code,
    team_name_ko
)
SELECT
    'VEYRO Performance Low FC',
    990000003,
    'EPL',
    '베이로 성능 소량 FC'
    WHERE NOT EXISTS (
    SELECT 1
    FROM team
    WHERE api_football_id = 990000003
);


-- ============================================================
-- 2. Player 생성
-- ============================================================

INSERT INTO player (
    player_name,
    api_football_id,
    player_name_ko
)
SELECT
    'VEYRO Performance High Player',
    991000001,
    '베이로 성능 대량 선수'
    WHERE NOT EXISTS (
    SELECT 1
    FROM player
    WHERE api_football_id = 991000001
);


INSERT INTO player (
    player_name,
    api_football_id,
    player_name_ko
)
SELECT
    'VEYRO Performance Medium Player',
    991000002,
    '베이로 성능 중간 선수'
    WHERE NOT EXISTS (
    SELECT 1
    FROM player
    WHERE api_football_id = 991000002
);


INSERT INTO player (
    player_name,
    api_football_id,
    player_name_ko
)
SELECT
    'VEYRO Performance Low Player',
    991000003,
    '베이로 성능 소량 선수'
    WHERE NOT EXISTS (
    SELECT 1
    FROM player
    WHERE api_football_id = 991000003
);


-- ============================================================
-- 3. TeamPlayer 관계 생성
-- ============================================================

INSERT IGNORE INTO team_player (
    team_id,
    player_id
)
SELECT
    team.team_id,
    player.player_id
FROM team
         JOIN player
WHERE (team.api_football_id, player.api_football_id) IN (
                                                         (990000001, 991000001),
                                                         (990000002, 991000002),
                                                         (990000003, 991000003)
    );


-- ============================================================
-- 4. PostTeam 관계 생성
-- post_created_at에는 원본 게시물 작성 시간을 저장한다.
-- ============================================================


-- High Team: 700,000건

INSERT IGNORE INTO post_team (
    team_id,
    post_id,
    created_at,
    post_created_at
)
SELECT
    team.team_id,
    selected.post_id,
    NOW(6),
    selected.content_created_at
FROM team
         JOIN (
    SELECT
        post_id,
        content_created_at
    FROM post
    ORDER BY post_id
        LIMIT 700000
) AS selected
WHERE team.api_football_id = 990000001;


-- Medium Team: 200,000건

INSERT IGNORE INTO post_team (
    team_id,
    post_id,
    created_at,
    post_created_at
)
SELECT
    team.team_id,
    selected.post_id,
    NOW(6),
    selected.content_created_at
FROM team
         JOIN (
    SELECT
        post_id,
        content_created_at
    FROM post
    ORDER BY post_id
        LIMIT 200000 OFFSET 700000
) AS selected
WHERE team.api_football_id = 990000002;


-- Low Team: 50,000건

INSERT IGNORE INTO post_team (
    team_id,
    post_id,
    created_at,
    post_created_at
)
SELECT
    team.team_id,
    selected.post_id,
    NOW(6),
    selected.content_created_at
FROM team
         JOIN (
    SELECT
        post_id,
        content_created_at
    FROM post
    ORDER BY post_id
        LIMIT 50000 OFFSET 900000
) AS selected
WHERE team.api_football_id = 990000003;


-- ============================================================
-- 5. PostPlayer 관계 생성
-- ============================================================

-- High Player: 700,000건

INSERT IGNORE INTO post_player (
    player_id,
    post_id,
    created_at,
    post_created_at
)
SELECT
    player.player_id,
    selected.post_id,
    NOW(6),
    selected.content_created_at
FROM player
         JOIN (
    SELECT post_id, content_created_at
    FROM post
    ORDER BY post_id
        LIMIT 700000
) AS selected
WHERE player.api_football_id = 991000001;


-- Medium Player: 200,000건

INSERT IGNORE INTO post_player (
    player_id,
    post_id,
    created_at,
    post_created_at
)
SELECT
    player.player_id,
    selected.post_id,
    NOW(6),
    selected.content_created_at
FROM player
         JOIN (
    SELECT post_id, content_created_at
    FROM post
    ORDER BY post_id
        LIMIT 200000 OFFSET 700000
) AS selected
WHERE player.api_football_id = 991000002;


-- Low Player: 50,000건

INSERT IGNORE INTO post_player (
    player_id,
    post_id,
    created_at,
    post_created_at
)
SELECT
    player.player_id,
    selected.post_id,
    NOW(6),
    selected.content_created_at
FROM player
         JOIN (
    SELECT post_id, content_created_at
    FROM post
    ORDER BY post_id
        LIMIT 50000 OFFSET 900000
) AS selected
WHERE player.api_football_id = 991000003;


-- ============================================================
-- 6. 팀별 게시물 데이터 검증
-- ============================================================

SELECT
    team.team_id,
    team.team_name,
    team.team_name_ko,
    COUNT(post_team.post_team_id) AS post_count
FROM team
         LEFT JOIN post_team
                   ON post_team.team_id = team.team_id
WHERE team.api_football_id IN (
                               990000001,
                               990000002,
                               990000003
    )
GROUP BY
    team.team_id,
    team.team_name,
    team.team_name_ko
ORDER BY post_count DESC;


-- ============================================================
-- 7. 선수별 게시물 데이터 검증
-- ============================================================

SELECT
    player.player_id,
    player.player_name,
    player.player_name_ko,
    COUNT(post_player.post_player_id) AS post_count
FROM player
         LEFT JOIN post_player
                   ON post_player.player_id = player.player_id
WHERE player.api_football_id IN (
                                 991000001,
                                 991000002,
                                 991000003
    )
GROUP BY
    player.player_id,
    player.player_name,
    player.player_name_ko
ORDER BY post_count DESC;


-- ============================================================
-- 8. post_created_at 데이터 정합성 검증
-- 정상: invalid_count = 0
-- ============================================================

SELECT
    COUNT(*) AS invalid_count
FROM post_team pt
         JOIN post p
              ON pt.post_id = p.post_id
WHERE NOT (
    pt.post_created_at <=> p.content_created_at
    );

-- ============================================================
-- 9. PostPlayer post_created_at 데이터 정합성 검증
-- 정상: invalid_count = 0
-- ============================================================


SELECT COUNT(*) AS invalid_count
FROM post_player pp
         JOIN post p ON pp.post_id = p.post_id
WHERE NOT (
    pp.post_created_at <=> p.content_created_at
    );