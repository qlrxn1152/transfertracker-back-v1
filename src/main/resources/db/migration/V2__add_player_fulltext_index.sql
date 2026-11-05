ALTER TABLE player
    ADD FULLTEXT INDEX ft_player_name (
    player_name,
    player_name_ko
);