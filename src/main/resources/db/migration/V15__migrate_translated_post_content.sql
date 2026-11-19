ALTER TABLE post
    MODIFY COLUMN content TEXT NOT NULL;

UPDATE post canonical_post
JOIN transfer_post legacy_post
    ON legacy_post.external_post_id = canonical_post.external_post_id
SET canonical_post.content = legacy_post.translated_content
WHERE legacy_post.translated_content IS NOT NULL
  AND CHAR_LENGTH(TRIM(legacy_post.translated_content)) > 0;
