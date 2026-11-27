ALTER TABLE fixture
    ADD COLUMN statistics_sync_completed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN player_statistics_sync_completed BOOLEAN NOT NULL DEFAULT FALSE;
