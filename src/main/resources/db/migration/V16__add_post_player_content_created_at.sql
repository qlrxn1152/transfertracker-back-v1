-- 1. 반정규화 컬럼 추가
ALTER TABLE post_player
    ADD COLUMN post_created_at DATETIME(6) NULL;


-- 2. 원본 게시물 작성 시간 복사
UPDATE post_player pp
    JOIN post p ON pp.post_id = p.post_id
    SET pp.post_created_at = p.content_created_at
WHERE pp.post_created_at IS NULL;


-- 3. NOT NULL 제약조건 적용
ALTER TABLE post_player
    MODIFY COLUMN post_created_at DATETIME(6) NOT NULL;


-- 4. 복합 인덱스 생성
CREATE INDEX idx_post_player_player_created_post
    ON post_player (
                    player_id,
                    post_created_at DESC,
                    post_id DESC
        );