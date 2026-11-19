ALTER TABLE post
    ADD COLUMN photo_url VARCHAR(1000) NULL AFTER is_transfer_related,
    ADD COLUMN translated_content TEXT NULL AFTER photo_url,
    ADD COLUMN translate_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' AFTER translated_content,
    ADD COLUMN translated_at DATETIME(6) NULL AFTER translate_status;
