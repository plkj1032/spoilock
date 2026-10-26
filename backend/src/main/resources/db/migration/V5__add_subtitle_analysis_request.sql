
-- AI 분석 요청 시각
-- NULL: 자막은 있지만 아직 분석 요청 전
-- NOT NULL: 분석 요청이 들어온 상태
ALTER TABLE episode_analysis
    ADD COLUMN analysis_requested_at DATETIME(6) NULL;

-- Java SpoilerSourceType enum과 DB 제약조건 통일
ALTER TABLE spoiler_events
DROP CHECK chk_spoiler_source;

UPDATE spoiler_events
SET source_type = 'LICENSED_SUBTITLE'
WHERE source_type IN ('SRT', 'LICENSED');

ALTER TABLE spoiler_events
    ADD CONSTRAINT chk_spoiler_source
        CHECK (
            source_type IN (
                            'LICENSED_SUBTITLE',
                            'MANUAL',
                            'USER_CONTRIBUTED'
                )
            );
