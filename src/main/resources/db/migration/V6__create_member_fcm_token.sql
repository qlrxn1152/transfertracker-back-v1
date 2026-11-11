CREATE TABLE member_fcm_token(
    member_fcm_token_id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    platform ENUM('WEB', 'MOBILE') NOT NULL,
    token VARCHAR(512) NOT NULL,
    created_at DATETIME(6) NOT NULL,

    PRIMARY KEY (member_fcm_token_id),

    CONSTRAINT uk_member_fcm_token_token UNIQUE (token),

    CONSTRAINT fk_member_fcm_token_member FOREIGN KEY (member_id) REFERENCES member(member_id)

);