CREATE TABLE member_favorite_team (
    member_favorite_team_id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,

    PRIMARY KEY (member_favorite_team_id),

    CONSTRAINT uk_member_favorite_team_member_team UNIQUE (member_id, team_id),

    CONSTRAINT fk_member_favorite_team_member FOREIGN KEY (member_id) REFERENCES member(member_id),
    CONSTRAINT fk_member_favorite_team_team FOREIGN KEY (team_id) REFERENCES team(team_id)
);
