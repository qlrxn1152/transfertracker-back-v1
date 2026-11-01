CREATE TABLE member (
    member_id BIGINT NOT NULL AUTO_INCREMENT,
    login_id VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    member_grade ENUM('ADMIN', 'USER') NOT NULL,

    PRIMARY KEY (member_id),
    CONSTRAINT uk_member_login_id UNIQUE (login_id)
) ENGINE=InnoDB;

CREATE TABLE player (
    player_id BIGINT NOT NULL AUTO_INCREMENT,
    player_name VARCHAR(255) NOT NULL,
    api_football_id BIGINT NOT NULL,
    player_name_ko VARCHAR(255),

    PRIMARY KEY (player_id),
    UNIQUE KEY uk_player_api_football_id (api_football_id),
    INDEX idx_player_player_name (player_name)
) ENGINE=InnoDB;

CREATE TABLE team(
    team_id BIGINT NOT NULL AUTO_INCREMENT,
    team_name VARCHAR(255) NOT NULL,
    api_football_id BIGINT NOT NULL,
    league_api_code ENUM('EPL', 'LA_LIGA', 'BUNDESLIGA', 'SERIE_A', 'LIGUE_1', 'K_LEAGUE'),
    team_name_ko VARCHAR(255),

    PRIMARY KEY (team_id),

    UNIQUE KEY uk_team_api_football_id (api_football_id),
    UNIQUE KEY uk_team_name (team_name),
    UNIQUE KEY uk_team_name_ko (team_name_ko)
) ENGINE=InnoDB;

CREATE TABLE team_player(
    team_player_id BIGINT NOT NULL AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,

    PRIMARY KEY (team_player_id),

    UNIQUE KEY uk_team_player (team_id, player_id),
    UNIQUE KEY uk_team_player_player (player_id),

    CONSTRAINT fk_team_player_team FOREIGN KEY (team_id) REFERENCES team (team_id),
    CONSTRAINT fk_team_player_player FOREIGN KEY (player_id) REFERENCES player (player_id)

) ENGINE=InnoDB;

CREATE TABLE transfer(
    transfer_id BIGINT NOT NULL AUTO_INCREMENT,
    player_id BIGINT NOT NULL,
    in_team_id BIGINT NOT NULL,
    out_team_id BIGINT NOT NULL,
    transfer_type VARCHAR(255) NOT NULL,
    transfer_date DATE NOT NULL,

    PRIMARY KEY (transfer_id),

    CONSTRAINT fk_transfer_player FOREIGN KEY (player_id) REFERENCES player (player_id),
    CONSTRAINT fk_transfer_in_team FOREIGN KEY (in_team_id) REFERENCES team (team_id),
    CONSTRAINT fk_transfer_out_team FOREIGN KEY (out_team_id) REFERENCES team (team_id),

    INDEX idx_transfer_transfer_date (transfer_date),

    UNIQUE KEY uk_transfer (player_id, in_team_id, out_team_id, transfer_date),

    CONSTRAINT chk_transfer_different_teams CHECK (in_team_id <> out_team_id)
) ENGINE=InnoDB;

CREATE TABLE transfer_post(
    transfer_post_id BIGINT NOT NULL AUTO_INCREMENT,
    content VARCHAR(500) NOT NULL,
    external_post_id VARCHAR(255) NOT NULL,
    content_created_at DATETIME(6) NOT NULL,
    is_transfer_related BOOLEAN NOT NULL,
    photo_url VARCHAR(1000),
    translated_content TEXT,
    translated_at DATETIME(6),
    team_id BIGINT,
    sourcer_name ENUM('FABRIZIO_ROMANO', 'DAVID_ORNSTEIN', 'MATTEO_MORETTO', 'FLORIAN_PLETTENBERG', 'SANTI_AOUNA', 'DI_MARZIO') NOT NULL,
    translate_status ENUM('PENDING', 'FAILED', 'COMPLETED') NOT NULL,

    PRIMARY KEY (transfer_post_id),

    UNIQUE KEY uk_transfer_post_sourcer_content (sourcer_name, content),
    UNIQUE KEY uk_transfer_post_external_post_id (external_post_id),

    INDEX idx_transfer_post_related (is_transfer_related),
    INDEX idx_transfer_post_status (translate_status),

    CONSTRAINT fk_transfer_post_team FOREIGN KEY (team_id) REFERENCES team (team_id)

) ENGINE=InnoDB;