
-- =========================================================
-- SPOILOCK V1
-- MySQL 8.0.16+
-- 총 19개 테이블
-- =========================================================

-- =========================================================
-- A. 백기완 : 회원 / 인증 / 작품 / 시청 진도
-- =========================================================

-- 1. 회원
CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       email VARCHAR(255) NULL,
                       password_hash VARCHAR(255) NULL,
                       nickname VARCHAR(50) NOT NULL,
                       role VARCHAR(20) NOT NULL DEFAULT 'USER',
                       account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                       token_version BIGINT NOT NULL DEFAULT 0,
                       created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                       updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

                       CONSTRAINT uk_users_email UNIQUE (email),
                       CONSTRAINT chk_token_version CHECK (token_version >= 0),
                       CONSTRAINT chk_user_role CHECK (role IN ('USER', 'ADMIN')),
                       CONSTRAINT chk_account_status CHECK (
                           account_status IN ('ACTIVE', 'SUSPENDED', 'WITHDRAWN')
                           )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 2. 카카오 소셜 로그인
CREATE TABLE oauth_accounts (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                user_id BIGINT NOT NULL,
                                provider VARCHAR(20) NOT NULL,
                                provider_user_id VARCHAR(100) NOT NULL,
                                linked_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                CONSTRAINT uk_oauth_provider_user
                                    UNIQUE (provider, provider_user_id),

                                CONSTRAINT fk_oauth_user
                                    FOREIGN KEY (user_id) REFERENCES users(id),

                                CONSTRAINT chk_oauth_provider
                                    CHECK (provider IN ('KAKAO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 3. Refresh Token 세션
CREATE TABLE refresh_sessions (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  user_id BIGINT NOT NULL,
                                  token_hash CHAR(64) NOT NULL,
                                  expires_at DATETIME(6) NOT NULL,
                                  revoked_at DATETIME(6) NULL,
                                  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                  CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),

                                  CONSTRAINT fk_refresh_user
                                      FOREIGN KEY (user_id) REFERENCES users(id),

                                  INDEX idx_refresh_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 4. 드라마
CREATE TABLE dramas (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        title VARCHAR(200) NOT NULL,
                        synopsis TEXT NULL,
                        poster_url VARCHAR(1000) NULL,
                        total_episode_count INT NOT NULL DEFAULT 0,
                        status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
                        created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                        updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

                        CONSTRAINT chk_total_episode_count
                            CHECK (total_episode_count >= 0),

                        CONSTRAINT chk_drama_status
                            CHECK (status IN ('DRAFT', 'PUBLISHED', 'HIDDEN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 5. 에피소드
CREATE TABLE episodes (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          drama_id BIGINT NOT NULL,
                          episode_number INT NOT NULL,
                          title VARCHAR(200) NOT NULL,
                          released_at DATETIME(6) NULL,
                          created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                          CONSTRAINT uk_episode_drama_number
                              UNIQUE (drama_id, episode_number),

                          CONSTRAINT fk_episode_drama
                              FOREIGN KEY (drama_id) REFERENCES dramas(id),

                          CONSTRAINT chk_episode_number
                              CHECK (episode_number > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 6. 사용자별 드라마 시청 진도
CREATE TABLE user_progress (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               user_id BIGINT NOT NULL,
                               drama_id BIGINT NOT NULL,
                               watched_episode_number INT NOT NULL DEFAULT 0,
                               updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

                               CONSTRAINT uk_user_drama_progress
                                   UNIQUE (user_id, drama_id),

                               CONSTRAINT fk_progress_user
                                   FOREIGN KEY (user_id) REFERENCES users(id),

                               CONSTRAINT fk_progress_drama
                                   FOREIGN KEY (drama_id) REFERENCES dramas(id),

                               CONSTRAINT chk_watched_episode
                                   CHECK (watched_episode_number >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =========================================================
-- B. 장준호 : WebSocket / STOMP
-- =========================================================

-- 7. 채팅방
-- 한 작품에 여러 채팅방 생성 가능
CREATE TABLE chat_rooms (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            drama_id BIGINT NOT NULL,
                            created_by BIGINT NOT NULL,
                            room_name VARCHAR(100) NOT NULL,
                            description TEXT NULL,
                            active BOOLEAN NOT NULL DEFAULT TRUE,
                            created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                            CONSTRAINT fk_chat_room_drama
                                FOREIGN KEY (drama_id) REFERENCES dramas(id),

                            CONSTRAINT fk_chat_room_creator
                                FOREIGN KEY (created_by) REFERENCES users(id),

                            INDEX idx_chat_room_drama (drama_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 8. 채팅방 참여자
CREATE TABLE chat_room_members (
                                   room_id BIGINT NOT NULL,
                                   user_id BIGINT NOT NULL,
                                   joined_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                   PRIMARY KEY (room_id, user_id),

                                   CONSTRAINT fk_chat_member_room
                                       FOREIGN KEY (room_id) REFERENCES chat_rooms(id),

                                   CONSTRAINT fk_chat_member_user
                                       FOREIGN KEY (user_id) REFERENCES users(id),

                                   INDEX idx_chat_member_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 9. 채팅 메시지
CREATE TABLE chat_messages (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               room_id BIGINT NOT NULL,
                               user_id BIGINT NOT NULL,
                               content TEXT NOT NULL,
                               required_episode_number INT NULL,
                               analysis_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                               moderation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                               created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                               CONSTRAINT fk_chat_message_room
                                   FOREIGN KEY (room_id) REFERENCES chat_rooms(id),

                               CONSTRAINT fk_chat_message_user
                                   FOREIGN KEY (user_id) REFERENCES users(id),

                               CONSTRAINT chk_chat_required_episode
                                   CHECK (
                                       required_episode_number IS NULL
                                           OR required_episode_number > 0
                                       ),

                               CONSTRAINT chk_chat_analysis_status
                                   CHECK (
                                       analysis_status IN (
                                                           'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'
                                           )
                                       ),

                               CONSTRAINT chk_chat_moderation_status
                                   CHECK (
                                       moderation_status IN (
                                                             'PENDING', 'APPROVED', 'REVIEW', 'REJECTED'
                                           )
                                       ),

                               INDEX idx_chat_message_room (room_id, id),
                               INDEX idx_chat_message_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =========================================================
-- C. 백기완 : SNS / 게시글 / 좋아요 / 팔로우
-- =========================================================

-- 10. SNS 게시글 / 댓글
CREATE TABLE posts (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       user_id BIGINT NOT NULL,
                       drama_id BIGINT NOT NULL,
                       parent_post_id BIGINT NULL,
                       title VARCHAR(200) NULL,
                       content TEXT NOT NULL,
                       required_episode_number INT NULL,
                       analysis_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                       moderation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                       created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                       updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

                       CONSTRAINT fk_post_user
                           FOREIGN KEY (user_id) REFERENCES users(id),

                       CONSTRAINT fk_post_drama
                           FOREIGN KEY (drama_id) REFERENCES dramas(id),

                       CONSTRAINT fk_post_parent
                           FOREIGN KEY (parent_post_id) REFERENCES posts(id),

                       CONSTRAINT chk_post_required_episode
                           CHECK (
                               required_episode_number IS NULL
                                   OR required_episode_number > 0
                               ),

                       CONSTRAINT chk_post_analysis_status
                           CHECK (
                               analysis_status IN (
                                                   'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'
                                   )
                               ),

                       CONSTRAINT chk_post_moderation_status
                           CHECK (
                               moderation_status IN (
                                                     'PENDING', 'APPROVED', 'REVIEW', 'REJECTED'
                                   )
                               ),

                       INDEX idx_post_feed (created_at, id),
                       INDEX idx_post_drama_feed (drama_id, created_at, id),
                       INDEX idx_post_parent (parent_post_id),
                       INDEX idx_post_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 11. 좋아요
CREATE TABLE post_likes (
                            user_id BIGINT NOT NULL,
                            post_id BIGINT NOT NULL,
                            created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                            PRIMARY KEY (user_id, post_id),

                            CONSTRAINT fk_post_like_user
                                FOREIGN KEY (user_id) REFERENCES users(id),

                            CONSTRAINT fk_post_like_post
                                FOREIGN KEY (post_id) REFERENCES posts(id),

                            INDEX idx_post_like_post (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 12. 팔로우
CREATE TABLE user_follows (
                              follower_id BIGINT NOT NULL,
                              following_id BIGINT NOT NULL,
                              created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                              PRIMARY KEY (follower_id, following_id),

                              CONSTRAINT fk_follow_follower
                                  FOREIGN KEY (follower_id) REFERENCES users(id),

                              CONSTRAINT fk_follow_following
                                  FOREIGN KEY (following_id) REFERENCES users(id),

                              CONSTRAINT chk_follow_self
                                  CHECK (follower_id <> following_id),

                              INDEX idx_follow_following (following_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =========================================================
-- D. 최서연 : SRT / LLM / ChromaDB
-- =========================================================

-- 13. 회차별 주요 스포일러 사건
CREATE TABLE spoiler_events (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                episode_id BIGINT NOT NULL,
                                chroma_doc_id VARCHAR(150) NULL,
                                event_summary TEXT NOT NULL,
                                source_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
                                verified BOOLEAN NOT NULL DEFAULT FALSE,
                                index_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                                created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

                                CONSTRAINT uk_spoiler_chroma_doc
                                    UNIQUE (chroma_doc_id),

                                CONSTRAINT fk_spoiler_episode
                                    FOREIGN KEY (episode_id) REFERENCES episodes(id),

                                CONSTRAINT chk_spoiler_source
                                    CHECK (source_type IN ('SRT', 'MANUAL', 'LICENSED')),

                                CONSTRAINT chk_spoiler_index_status
                                    CHECK (
                                        index_status IN (
                                                         'PENDING', 'INDEXING', 'INDEXED', 'FAILED'
                                            )
                                        ),

                                INDEX idx_spoiler_episode (episode_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 14. 에피소드 자막 분석 상태
CREATE TABLE episode_analysis (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  episode_id BIGINT NOT NULL,
                                  subtitle_filename VARCHAR(255) NULL,
                                  analysis_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                                  event_count INT NOT NULL DEFAULT 0,
                                  error_message TEXT NULL,
                                  analyzed_at DATETIME(6) NULL,
                                  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

                                  CONSTRAINT uk_episode_analysis
                                      UNIQUE (episode_id),

                                  CONSTRAINT fk_episode_analysis_episode
                                      FOREIGN KEY (episode_id) REFERENCES episodes(id),

                                  CONSTRAINT chk_episode_event_count
                                      CHECK (event_count >= 0),

                                  CONSTRAINT chk_episode_analysis_status
                                      CHECK (
                                          analysis_status IN (
                                                              'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'
                                              )
                                          )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 15. 채팅 AI 분석 결과
-- 메시지 하나당 분석 결과 최대 하나
CREATE TABLE chat_ai_results (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 message_id BIGINT NOT NULL,
                                 matched_event_id BIGINT NULL,
                                 predicted_episode_number INT NULL,
                                 confidence_score DECIMAL(5,4) NULL,
                                 risk_level VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
                                 reason TEXT NULL,
                                 analyzed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                 CONSTRAINT uk_chat_ai_message
                                     UNIQUE (message_id),

                                 CONSTRAINT fk_chat_ai_message
                                     FOREIGN KEY (message_id) REFERENCES chat_messages(id),

                                 CONSTRAINT fk_chat_ai_event
                                     FOREIGN KEY (matched_event_id) REFERENCES spoiler_events(id),

                                 CONSTRAINT chk_chat_ai_episode
                                     CHECK (
                                         predicted_episode_number IS NULL
                                             OR predicted_episode_number > 0
                                         ),

                                 CONSTRAINT chk_chat_confidence
                                     CHECK (
                                         confidence_score IS NULL
                                             OR confidence_score BETWEEN 0 AND 1
                                         ),

                                 CONSTRAINT chk_chat_risk
                                     CHECK (
                                         risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'UNKNOWN')
                                         ),

                                 INDEX idx_chat_ai_event (matched_event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 16. SNS 게시글 AI 분석 결과
-- 향후 SNS 자동 검열에 사용
CREATE TABLE post_ai_results (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 post_id BIGINT NOT NULL,
                                 matched_event_id BIGINT NULL,
                                 predicted_episode_number INT NULL,
                                 confidence_score DECIMAL(5,4) NULL,
                                 risk_level VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
                                 reason TEXT NULL,
                                 analyzed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                 CONSTRAINT uk_post_ai_post
                                     UNIQUE (post_id),

                                 CONSTRAINT fk_post_ai_post
                                     FOREIGN KEY (post_id) REFERENCES posts(id),

                                 CONSTRAINT fk_post_ai_event
                                     FOREIGN KEY (matched_event_id) REFERENCES spoiler_events(id),

                                 CONSTRAINT chk_post_ai_episode
                                     CHECK (
                                         predicted_episode_number IS NULL
                                             OR predicted_episode_number > 0
                                         ),

                                 CONSTRAINT chk_post_confidence
                                     CHECK (
                                         confidence_score IS NULL
                                             OR confidence_score BETWEEN 0 AND 1
                                         ),

                                 CONSTRAINT chk_post_risk
                                     CHECK (
                                         risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'UNKNOWN')
                                         ),

                                 INDEX idx_post_ai_event (matched_event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =========================================================
-- E. 최서연 : 신고 / 관리자
-- =========================================================

-- 17. 채팅 신고
CREATE TABLE chat_reports (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              message_id BIGINT NOT NULL,
                              reporter_id BIGINT NOT NULL,
                              reason VARCHAR(500) NOT NULL,
                              status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
                              created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                              processed_at DATETIME(6) NULL,

                              CONSTRAINT fk_chat_report_message
                                  FOREIGN KEY (message_id) REFERENCES chat_messages(id),

                              CONSTRAINT fk_chat_report_user
                                  FOREIGN KEY (reporter_id) REFERENCES users(id),

                              CONSTRAINT chk_chat_report_status
                                  CHECK (status IN ('OPEN', 'RESOLVED', 'DISMISSED')),

                              INDEX idx_chat_report_status (status, created_at),
                              INDEX idx_chat_report_message (message_id),
                              INDEX idx_chat_report_user (reporter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 18. 게시글 신고
CREATE TABLE post_reports (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              post_id BIGINT NOT NULL,
                              reporter_id BIGINT NOT NULL,
                              reason VARCHAR(500) NOT NULL,
                              status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
                              created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                              processed_at DATETIME(6) NULL,

                              CONSTRAINT fk_post_report_post
                                  FOREIGN KEY (post_id) REFERENCES posts(id),

                              CONSTRAINT fk_post_report_user
                                  FOREIGN KEY (reporter_id) REFERENCES users(id),

                              CONSTRAINT chk_post_report_status
                                  CHECK (status IN ('OPEN', 'RESOLVED', 'DISMISSED')),

                              INDEX idx_post_report_status (status, created_at),
                              INDEX idx_post_report_post (post_id),
                              INDEX idx_post_report_user (reporter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 19. 관리자 작업 기록
CREATE TABLE admin_audit_logs (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  admin_user_id BIGINT NOT NULL,
                                  action VARCHAR(40) NOT NULL,
                                  target_type VARCHAR(40) NOT NULL,
                                  target_id BIGINT NOT NULL,
                                  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                  CONSTRAINT fk_admin_audit_user
                                      FOREIGN KEY (admin_user_id) REFERENCES users(id),

                                  INDEX idx_admin_audit_user (admin_user_id),
                                  INDEX idx_admin_audit_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
