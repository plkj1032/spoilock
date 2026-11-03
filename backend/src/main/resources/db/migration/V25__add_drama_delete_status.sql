ALTER TABLE dramas
DROP CHECK chk_drama_status,
    ADD COLUMN deleted_at DATETIME(6) NULL,
    ADD CONSTRAINT chk_drama_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'HIDDEN', 'DELETED'));