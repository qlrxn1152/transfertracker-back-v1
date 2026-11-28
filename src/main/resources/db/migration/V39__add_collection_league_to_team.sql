ALTER TABLE team
    ADD COLUMN collection_league_code VARCHAR(64) NULL AFTER league_api_code;

UPDATE team
SET collection_league_code = league_api_code
WHERE league_api_code IS NOT NULL;

CREATE INDEX idx_team_collection_league_code
    ON team (collection_league_code);
