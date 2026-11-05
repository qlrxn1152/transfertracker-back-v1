DELETE FROM transfer
WHERE player_id IN (
    SELECT player_id
    FROM player
    WHERE api_football_id IN (111, 222, 333)
);

DELETE FROM player
WHERE api_football_id IN (111, 222, 333);

DELETE FROM team
WHERE api_football_id IN (44, 55, 66);