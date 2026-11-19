-- 기존 transfer_post 데이터는 삭제하지 않고 post를 기준 테이블로 복사한다.
-- external_post_id로 연결하므로 이미 새 구조에 저장된 게시물과도 안전하게 병합된다.
INSERT IGNORE INTO post (
    journalist_name,
    content,
    external_post_id,
    content_created_at,
    is_transfer_related,
    photo_url,
    translated_content,
    translate_status,
    translated_at
)
SELECT
    sourcer_name,
    content,
    external_post_id,
    content_created_at,
    is_transfer_related,
    photo_url,
    translated_content,
    translate_status,
    translated_at
FROM transfer_post;

INSERT IGNORE INTO post_team (team_id, post_id, created_at)
SELECT
    legacy_team.team_id,
    canonical_post.post_id,
    legacy_team.created_at
FROM transfer_post_team legacy_team
JOIN transfer_post legacy_post
    ON legacy_post.transfer_post_id = legacy_team.post_id
JOIN post canonical_post
    ON canonical_post.external_post_id = legacy_post.external_post_id;

INSERT IGNORE INTO post_player (player_id, post_id, created_at)
SELECT
    legacy_player.player_id,
    canonical_post.post_id,
    legacy_player.created_at
FROM transfer_post_player legacy_player
JOIN transfer_post legacy_post
    ON legacy_post.transfer_post_id = legacy_player.transfer_post_id
JOIN post canonical_post
    ON canonical_post.external_post_id = legacy_post.external_post_id;
