DELETE FROM transfer
WHERE player_id IN (
    SELECT player_id
    FROM player
    WHERE api_football_id IN (910111, 910222, 910333)
);

DELETE FROM player
WHERE api_football_id IN (910111, 910222, 910333);

DELETE FROM team
WHERE api_football_id IN (
                          910044, 910055, 910066, 910077, 910088
    );