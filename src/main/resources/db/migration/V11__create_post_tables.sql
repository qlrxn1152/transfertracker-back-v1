
-- ====================================
-- 1. VEYRO 자체 게시물
-- ====================================

CREATE TABLE post (
                      post_id BIGINT NOT NULL AUTO_INCREMENT,
                      journalist_name VARCHAR(50) NOT NULL,
                      content VARCHAR(500) NOT NULL,
                      external_post_id VARCHAR(255) NOT NULL,
                      content_created_at DATETIME(6) NOT NULL,
                      is_transfer_related BOOLEAN NOT NULL,

                      PRIMARY KEY (post_id),

                      CONSTRAINT uk_post_external_post_id
                          UNIQUE (external_post_id),

                      INDEX idx_post_related (is_transfer_related)
);





-- ====================================
-- 2. Post - Player 연결 테이블
-- ====================================

CREATE TABLE post_player (
                             post_player_id BIGINT NOT NULL AUTO_INCREMENT,
                             player_id BIGINT NOT NULL,
                             post_id BIGINT NOT NULL,
                             created_at DATETIME(6) NOT NULL,

                             PRIMARY KEY (post_player_id),

                             CONSTRAINT uk_post_player_player_post
                                 UNIQUE (player_id, post_id),

                             INDEX idx_post_player_post (post_id),

                             CONSTRAINT fk_post_player_player
                                 FOREIGN KEY (player_id)
                                     REFERENCES player(player_id),

                             CONSTRAINT fk_post_player_post
                                 FOREIGN KEY (post_id)
                                     REFERENCES post(post_id)
);


-- ====================================
-- 3. Post - Team 연결 테이블
-- ====================================

CREATE TABLE post_team (
                           post_team_id BIGINT NOT NULL AUTO_INCREMENT,
                           team_id BIGINT NOT NULL,
                           post_id BIGINT NOT NULL,
                           created_at DATETIME(6) NOT NULL,

                           PRIMARY KEY (post_team_id),

                           CONSTRAINT uk_post_team_team_post
                               UNIQUE (team_id, post_id),

                           INDEX idx_post_team_post (post_id),

                           CONSTRAINT fk_post_team_team
                               FOREIGN KEY (team_id)
                                   REFERENCES team(team_id),

                           CONSTRAINT fk_post_team_post
                               FOREIGN KEY (post_id)
                                   REFERENCES post(post_id)
);
