-- 1. 기존 Team 외래키 제약조건 제거
ALTER TABLE transfer_post
DROP FOREIGN KEY fk_transfer_post_team;

-- 2. 기존 team_id 컬럼 제거
ALTER TABLE transfer_post
DROP COLUMN team_id;