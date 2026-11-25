-- 1. 반정규화 컬럼 추가
ALTER TABLE post_team
    ADD COLUMN post_created_at DATETIME(6) NULL;


-- 2. 기존 Post의 생성 시각 복사
UPDATE post_team pt
    JOIN post p ON pt.post_id = p.post_id
    SET pt.post_created_at = p.content_created_at
WHERE pt.post_created_at IS NULL;


-- 3. NOT NULL 제약조건 적용
ALTER TABLE post_team
    MODIFY COLUMN post_created_at DATETIME(6) NOT NULL;


-- 4. 팀별 최신 게시물 조회용 복합 인덱스 생성
CREATE INDEX idx_post_team_team_created_post
    ON post_team (
                  team_id,
                  post_created_at DESC,
                  post_id DESC
        );