ALTER TABLE dramas
    ADD COLUMN provision_type VARCHAR(20)
        NOT NULL DEFAULT 'INFO_ONLY',
    ADD CONSTRAINT chk_drama_provision_type
        CHECK (provision_type IN ('INFO_ONLY', 'STREAMING'));