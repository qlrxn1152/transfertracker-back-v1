CREATE TABLE member_favorite_player (

    member_favorite_player_id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,

    PRIMARY KEY (member_favorite_player_id),

    CONSTRAINT uk_member_favorite_player_member_player UNIQUE (member_id, player_id),

    CONSTRAINT fk_member_favorite_player_member FOREIGN KEY (member_id) REFERENCES member(member_id),
    CONSTRAINT fk_member_favorite_player_player FOREIGN KEY (player_id) REFERENCES player(player_id)


);