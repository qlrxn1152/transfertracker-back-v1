-- FK 제약 조건 때문에 transfer를 먼저 삭제
DELETE FROM transfer
WHERE player_id IN (
    SELECT player_id
    FROM player
    WHERE api_football_id IN (920111, 920222)
);

DELETE FROM player
WHERE api_football_id IN (920111, 920222);

DELETE FROM team
WHERE api_football_id IN (
                          920044, 920055, 920066, 920077
    );