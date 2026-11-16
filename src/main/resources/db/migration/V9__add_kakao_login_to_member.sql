ALTER TABLE member
    MODIFY password VARCHAR(255) NULL,
    ADD COLUMN kakao_id VARCHAR(255) NULL AFTER login_id,
    ADD CONSTRAINT uk_member_kakao_id UNIQUE (kakao_id);
