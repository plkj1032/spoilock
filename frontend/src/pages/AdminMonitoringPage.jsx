import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { adminMonitoringApi } from "../api/adminMonitoringApi";
import "../styles/AdminCenter.css";
import "../styles/AdminMonitoring.css";

// ========================================
// 1. 상태 코드 한글 표시
// ========================================
const LABELS = {
  PUBLIC: "전체 공개",
  EPISODE_GATED: "시청 진도 제한",
  HELD: "검토 대기",
  FAILED: "분석 실패",
  PENDING: "대기 중",
  PROCESSING: "분석 중",
  COMPLETED: "분석 완료",
  SKIPPED: "검사 제외",

  SUBTITLE: "자막",
  SUBTITLE_VIDEO: "자막 + 영상",
  VIDEO: "영상(STT)",

  AI_RESULT: "AI 판정 반환",
  AI_FAILED: "AI 판정 실패",
  DB_SAVED: "DB 결과 저장",
  DB_FAILED: "DB 결과 저장 실패",
  REDIS_PUBLISHED: "Redis 갱신 알림 전송",
  REDIS_FAILED: "Redis 갱신 알림 실패",
};

// ========================================
// V21: 서버가 기록한 정책 코드만 관리자에게 표시합니다.
// 이전 메시지의 NULL은 추측하지 않고 '정책 기록 없음'으로 표시합니다.
// ========================================
const POLICY_LABELS = {
  GENERAL_CHAT: "일반 대화 · 전체 공개",
  GENERAL_HELD: "일반 대화 · 공개 보류",
  UNRELATED_CHAT: "사건 무관 · 전체 공개",
  UNRELATED_HELD: "사건 무관 · 공개 보류",
  EVENT_MATCH: "스포일러 사건 매칭",
  MATCH_REVIEW: "사건 매칭 · 검토 필요",
  MATCH_INVALID: "사건 매칭 정보 불완전",
  UNVERIFIED_EVENT: "검증되지 않은 사건",
  GUESS_KNOWN: "본 회차 내용을 추측처럼 · 회차 제한",
  GUESS_UNKNOWN: "안 본 회차 추측 · 전체 공개",
  FINALE_ONLY: "판정 애매 · 마지막 회차부터 공개",
  GUESS_UNVERIFIED: "본 회차 추측 · 미검증 사건 보류",
  NO_MATCH_PUBLISHED: "일치 사건 없음 · 정책 공개",
  OFF_TOPIC: "작품과 무관한 일상 대화",
  NO_MATCH_REVIEW: "일치 사건 없음 · 검토 대기",
  UNCERTAIN_REVIEW: "판정 불확실 · 검토 대기",
  NO_EVENTS: "등록된 사건 없음",
  KB_LOOKUP_FAILED: "사건 DB 조회 실패",
  AI_REQUEST_FAILED: "AI 서버 호출 실패",
  AI_SERVER_ERROR: "AI 서버 판정 오류",
  EMPTY_VERDICT: "AI 빈 응답",
  UNKNOWN_VERDICT: "알 수 없는 AI 판정",
  OTHER_FAILED: "기타 분석 실패",
  OTHER_HELD: "기타 검토 대기",
  OTHER_PUBLIC: "기타 전체 공개",
};

function policyLabel(code) {
  if (!code) return "정책 기록 없음";
  return POLICY_LABELS[code] || "기타 정책 판정";
}

function policyTone(code) {
  if (!code) return "neutral";
  if (/FAILED|ERROR|INVALID|EMPTY|UNKNOWN/.test(code)) return "danger";
  if (/HELD|REVIEW|UNCERTAIN|UNVERIFIED|NO_EVENTS/.test(code)) return "warn";
  return "good";
}

function PolicyTag({ code }) {
  return (
    <span className={`monitor-tag monitor-tag-${policyTone(code)}`}>
      {policyLabel(code)}
    </span>
  );
}

// ========================================
// 2. 공통 포맷 함수
// ========================================
function number(value) {
  return Number(value ?? 0).toLocaleString("ko-KR");
}

function percent(part, total) {
  return total ? `${((part / total) * 100).toFixed(1)}%` : "0.0%";
}

function duration(value) {
  if (value == null) return "측정값 없음";

  return value >= 1000 ? `${(value / 1000).toFixed(2)}초` : `${value}ms`;
}

function date(value) {
  return value ? String(value).replace("T", " ").slice(0, 19) : "-";
}

function stateText(value) {
  return LABELS[value] || value || "기록 없음";
}

function tone(value) {
  if (
    ["FAILED", "AI_FAILED", "DB_FAILED", "REDIS_FAILED", "DOWN"].includes(value)
  ) {
    return "danger";
  }

  if (
    ["EPISODE_GATED", "HELD", "PENDING", "PROCESSING", "WARN"].includes(value)
  ) {
    return "warn";
  }

  if (["PUBLIC", "COMPLETED", "OK"].includes(value)) {
    return "good";
  }

  return "neutral";
}

// ========================================
// 3. 상태 뱃지
// ========================================
function StatusTag({ value }) {
  return (
    <span className={`monitor-tag monitor-tag-${tone(value)}`}>
      {stateText(value)}
    </span>
  );
}

// ========================================
// 4. 통계 카드
// ========================================
function Metric({ label, value, hint }) {
  return (
    <article className="monitor-stat">
      <span>{label}</span>
      <strong>{value}</strong>
      {hint && <small>{hint}</small>}
    </article>
  );
}

// ========================================
// 5. 시간대별 채팅 처리량
// ========================================
function ChatChart({ timeline, range }) {
  const peak = Math.max(1, ...timeline.map((row) => row.total));

  if (!timeline.length) {
    return <p className="monitor-empty">선택한 기간에 채팅 기록이 없습니다.</p>;
  }

  return (
    <div
      className="monitor-chart"
      role="img"
      aria-label="전체 채팅, 진도 제한 및 분석 실패 처리량"
    >
      {timeline.map((row) => (
        <div className="monitor-bar-row" key={row.period}>
          <span className="monitor-bar-label">
            {row.period.slice(range === "24h" ? 11 : 5)}
          </span>

          <div className="monitor-bars">
            <div
              className="monitor-bar monitor-bar-total"
              style={{
                width: `${(row.total / peak) * 100}%`,
              }}
              title={`채팅 ${row.total}건`}
            />

            <div
              className="monitor-bar monitor-bar-gated"
              style={{
                width: `${(row.episodeGated / peak) * 100}%`,
              }}
              title={`진도 제한 ${row.episodeGated}건`}
            />

            <div
              className="monitor-bar monitor-bar-failed"
              style={{
                width: `${(row.failed / peak) * 100}%`,
              }}
              title={`분석 실패 ${row.failed}건`}
            />
          </div>

          <strong>{row.total}건</strong>
        </div>
      ))}
    </div>
  );
}

// ========================================
// V21: 최근 정책 판정 로그 — 기존 chats.recent 결과 재사용
// 별도의 컴포넌트 파일/추가 API가 필요하지 않습니다.
// ========================================
function MonitoringPolicyLog({ rows, onTrace }) {
  return (
    <section className="monitor-panel" aria-label="최근 AI 정책 판정 로그">
      <h2>
        최근 AI 정책 판정 로그{" "}
        <span className="monitor-subtitle">최대 20건 · 자동 갱신</span>
      </h2>
      <p className="monitor-note">
        AI 판정의 정책 코드와 처리 결과만 표시합니다. 채팅 원문이나 서버 오류
        원문은 표시하지 않습니다.
      </p>
      <div className="monitor-table-wrap">
        <table className="monitor-table">
          <thead>
            <tr>
              <th>메시지 ID</th>
              <th>작품</th>
              <th>정책 사유</th>
              <th>최종 판정</th>
              <th>분석 시각</th>
              <th>추적</th>
            </tr>
          </thead>
          <tbody>
            {rows.length > 0 ? (
              rows.map((row) => (
                <tr key={row.messageId}>
                  <td>#{row.messageId}</td>
                  <td>{row.dramaTitle || "-"}</td>
                  <td>
                    <PolicyTag
                      code={row.policyCode ?? row.analysisPolicyCode}
                    />
                  </td>
                  <td>
                    <StatusTag
                      value={row.analysisOutcome || row.analysisStatus}
                    />
                  </td>
                  <td>{date(row.analyzedAt)}</td>
                  <td>
                    <button
                      type="button"
                      className="monitor-text-button"
                      onClick={() => onTrace(row.messageId)}
                    >
                      흐름 보기
                    </button>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={6} className="monitor-empty">
                  선택한 기간에 채팅 기록이 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}

// ========================================
// 6. 관리자 AI 모니터링 메인 페이지
// ========================================
export default function AdminMonitoringPage() {
  // 조회 조건
  const [range, setRange] = useState("24h");
  const [dramaId, setDramaId] = useState("");

  // 메시지 추적
  const [messageInput, setMessageInput] = useState("");
  const [targetMessageId, setTargetMessageId] = useState("");
  const traceSectionRef = useRef(null);
  const [traceLoadedId, setTraceLoadedId] = useState("");
  const [traceErrorId, setTraceErrorId] = useState("");
  const [traceInputError, setTraceInputError] = useState("");

  // API 데이터
  const [chats, setChats] = useState(null);
  const [advanced, setAdvanced] = useState(null);

  // 화면 상태
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [updated, setUpdated] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);

  // ========================================
  // 7. API 조회 및 10초 자동 갱신
  // ========================================
  useEffect(() => {
    let mounted = true;
    let current = null;

    async function reload() {
      if (document.hidden) return;

      // 이전 요청이 남아 있으면 취소
      current?.abort();

      const controller = new AbortController();
      current = controller;

      const base = {
        range,
        dramaId,
        signal: controller.signal,
      };

      // 한쪽 API가 실패해도 다른 결과 표시
      const responses = await Promise.allSettled([
        adminMonitoringApi.chats(base),
        adminMonitoringApi.advanced({
          ...base,
          messageId: targetMessageId,
        }),
      ]);

      if (!mounted || controller.signal.aborted) {
        return;
      }

      const [chatResult, advancedResult] = responses;

      if (chatResult.status === "fulfilled") {
        setChats(chatResult.value);
      } else {
        setChats(null);
      }

      if (advancedResult.status === "fulfilled") {
        setAdvanced(advancedResult.value);
        setTraceLoadedId(targetMessageId);
        setTraceErrorId("");
      } else {
        setAdvanced(null);
        setTraceLoadedId(targetMessageId);
        setTraceErrorId(targetMessageId);
      }

      const failed = [];

      if (chatResult.status === "rejected") {
        failed.push(`채팅 통계: ${chatResult.reason?.message || "조회 실패"}`);
      }

      if (advancedResult.status === "rejected") {
        failed.push(
          `확장 모니터링: ${advancedResult.reason?.message || "조회 실패"}`,
        );
      }

      setError(failed.join(" / "));

      if (!failed.length) {
        setUpdated(new Date());
      }

      setLoading(false);
    }

    // 최초 API 호출
    void reload();

    // 10초마다 자동 갱신
    const id = window.setInterval(() => {
      void reload();
    }, 10000);

    // 다른 탭에서 돌아오면 즉시 갱신
    const visible = () => {
      if (!document.hidden) {
        void reload();
      }
    };

    document.addEventListener("visibilitychange", visible);

    return () => {
      mounted = false;
      window.clearInterval(id);
      current?.abort();

      document.removeEventListener("visibilitychange", visible);
    };
  }, [range, dramaId, targetMessageId, reloadKey]);

  // 같은 메시지 ID를 반복 클릭해도 강제로 다시 조회할 수 있습니다.
  function openTrace(messageId) {
    const id = String(messageId ?? "").trim();
    if (!/^[1-9]\d*$/.test(id) || !Number.isSafeInteger(Number(id))) {
      setTraceInputError("메시지 ID에 1 이상의 정수를 입력해 주세요.");
      return;
    }
    setTraceInputError("");
    setMessageInput(id);
    setTraceLoadedId("");
    setTraceErrorId("");
    setTargetMessageId(id);
    setReloadKey((key) => key + 1);
    window.requestAnimationFrame(() => {
      traceSectionRef.current?.scrollIntoView({
        behavior: "smooth",
        block: "start",
      });
    });
  }

  const summary = chats?.summary;
  const timeline = chats?.timeline || [];
  const jobs = advanced?.jobs;

  // ========================================
  // 8. 화면 렌더링
  // ========================================
  return (
    <main className="container main-content admin-center monitor-page">
      <Link to="/admin" className="admin-center-back">
        ← 관리자 센터
      </Link>

      <header className="admin-center-heading">
        <span className="section-overline">SPOILOCK · AI OPS</span>

        <h1>AI 운영 모니터링</h1>

        <p>
          채팅 판정, 자막·영상 분석 작업, FastAPI·Redis 연결 상태를 조회합니다.
          원문 및 사용자 정보는 제외됩니다.
        </p>
      </header>

      {/* ========================================
          9. 조회 필터
      ======================================== */}
      <section className="monitor-toolbar" aria-label="조회 필터">
        <label>
          조회 기간
          <select
            value={range}
            onChange={(e) => {
              setRange(e.target.value);
            }}
          >
            <option value="24h">최근 24시간</option>
            <option value="7d">최근 7일</option>
          </select>
        </label>

        <label>
          작품
          <select
            value={dramaId}
            onChange={(e) => {
              setDramaId(e.target.value);
            }}
          >
            <option value="">전체 작품</option>

            {(chats?.dramas || []).map((d) => (
              <option key={d.id} value={d.id}>
                {d.title}
              </option>
            ))}
          </select>
        </label>

        <button
          type="button"
          className="button button-outline"
          onClick={() => {
            setReloadKey((value) => value + 1);
          }}
        >
          새로고침
        </button>
      </section>

      {/* ========================================
          10. 자동 갱신 상태
      ======================================== */}
      <p className="monitor-updated" role="status">
        {loading && !chats && !advanced
          ? "조회 중…"
          : `10초 자동 갱신${
              updated
                ? ` · 마지막 갱신 ${updated.toLocaleTimeString("ko-KR")}`
                : ""
            }`}
      </p>

      {error && (
        <p className="monitor-alert" role="alert">
          {error}
        </p>
      )}

      {/* ========================================
          11. 서버 연결 상태
      ======================================== */}
      {advanced && (
        <section className="monitor-panel">
          <h2>
            서비스 연결 상태{" "}
            <span className="monitor-subtitle">
              이번 요청을 처리한 서버: {advanced.nodeName || "확인 불가"}
            </span>
          </h2>

          <p className="monitor-note">
            FastAPI는 포트 연결만 검사합니다. 실제 /judge 또는 관리자 API 성공
            여부와는 별개입니다. JVM 힙은 EC2 전체 메모리가 아닙니다.
          </p>

          <div className="monitor-health-grid">
            {(advanced.services || []).map((svc) => (
              <div className="monitor-health-item" key={svc.name}>
                <div>
                  <strong>{svc.name}</strong>
                  <StatusTag value={svc.status} />
                </div>

                <small>{svc.detail}</small>
              </div>
            ))}

            <div className="monitor-health-item">
              <div>
                <strong>현재 JVM 힙</strong>
                <span>
                  {advanced.heapUsedMb ?? "-"} / {advanced.heapMaxMb ?? "-"} MB
                </span>
              </div>

              <small>ALB가 선택한 현재 응답 서버 기준</small>
            </div>
          </div>
        </section>
      )}

      {/* ========================================
          12. 채팅 분석 통계
      ======================================== */}
      {summary && (
        <>
          <section className="monitor-stats" aria-label="채팅 모니터링 통계">
            <Metric
              label="채팅 메시지"
              value={`${number(summary.total)}건`}
              hint="시스템 메시지 제외"
            />

            <Metric
              label="분석 완료"
              value={`${number(summary.completed)}건`}
              hint={percent(summary.completed, summary.total)}
            />

            <Metric
              label="진도 제한"
              value={`${number(summary.episodeGated)}건`}
              hint="공개 정책 결과"
            />

            <Metric
              label="검토 대기"
              value={`${number(summary.held)}건`}
              hint="원문 공개 보류"
            />

            <Metric
              label="분석 실패"
              value={`${number(summary.failed)}건`}
              hint={percent(summary.failed, summary.total)}
            />

            <Metric
              label="평균 처리시간"
              value={duration(summary.averageDurationMs)}
              hint="신규 계측 기준"
            />

            <Metric
              label="P95 처리시간"
              value={duration(summary.p95DurationMs)}
              hint="신규 계측 기준"
            />

            <Metric
              label="진행 / 대기"
              value={`${number(
                summary.processing,
              )} / ${number(summary.pending)}`}
              hint="메시지 현재 상태"
            />
          </section>

          {/* ========================================
              13. 시간대별 그래프
          ======================================== */}
          <section className="monitor-panel">
            <h2>{range === "24h" ? "시간대별" : "일별"} 채팅 처리량</h2>

            <p className="monitor-note">
              파랑: 전체 · 주황: 진도 제한 · 빨강: 분석 실패
            </p>

            <ChatChart timeline={timeline} range={range} />
          </section>

          {/* ========================================
              14. 최근 채팅 판정
          ======================================== */}
          <section className="monitor-panel">
            <h2>
              최근 채팅 판정 <span className="monitor-subtitle">최대 20건</span>
            </h2>

            <div className="monitor-table-wrap">
              <table className="monitor-table">
                <thead>
                  <tr>
                    <th>메시지 ID</th>
                    <th>작품</th>
                    <th>판정</th>
                    <th>정책 사유</th>
                    <th>처리시간</th>
                    <th>등록시각</th>
                    <th>추적</th>
                  </tr>
                </thead>

                <tbody>
                  {(chats.recent || []).length ? (
                    chats.recent.map((row) => (
                      <tr key={row.messageId}>
                        <td>#{row.messageId}</td>
                        <td>{row.dramaTitle}</td>

                        <td>
                          <StatusTag
                            value={row.analysisOutcome || row.analysisStatus}
                          />
                        </td>

                        <td>
                          <PolicyTag
                            code={row.policyCode ?? row.analysisPolicyCode}
                          />
                        </td>

                        <td>{duration(row.durationMs)}</td>

                        <td>{date(row.createdAt)}</td>

                        <td>
                          <button
                            type="button"
                            className="monitor-text-button"
                            onClick={() => openTrace(row.messageId)}
                          >
                            흐름 보기
                          </button>
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan={7} className="monitor-empty">
                        조회할 데이터가 없습니다.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>

          <MonitoringPolicyLog rows={chats.recent || []} onTrace={openTrace} />
        </>
      )}

      {/* ========================================
          15. 자막 및 영상 AI 분석
      ======================================== */}
      {advanced && (
        <section className="monitor-panel">
          <h2>자막·영상 AI 분석 작업</h2>

          <p className="monitor-note">
            선택한 기간에 분석을 요청한 회차 기준. 완료 후에도 최신 작업 상태가
            표시됩니다.
          </p>

          <div className="monitor-stats monitor-job-stats">
            <Metric label="전체 작업" value={`${number(jobs?.total)}건`} />

            <Metric
              label="대기 / 진행"
              value={`${number(jobs?.pending)} / ${number(jobs?.processing)}`}
            />

            <Metric label="완료" value={`${number(jobs?.completed)}건`} />

            <Metric label="실패" value={`${number(jobs?.failed)}건`} />
          </div>

          <div className="monitor-table-wrap">
            <table className="monitor-table">
              <thead>
                <tr>
                  <th>회차 ID</th>
                  <th>작품</th>
                  <th>분석 대상</th>
                  <th>상태</th>
                  <th>진행률</th>
                  <th>사건 수</th>
                  <th>요청시각</th>
                </tr>
              </thead>

              <tbody>
                {(advanced.recentJobs || []).length ? (
                  advanced.recentJobs.map((row) => (
                    <tr key={row.episodeId}>
                      <td>#{row.episodeId}</td>
                      <td>{row.dramaTitle}</td>

                      <td>{stateText(row.source)}</td>

                      <td>
                        <StatusTag value={row.status} />
                      </td>

                      <td>{row.progress == null ? "-" : `${row.progress}%`}</td>

                      <td>{row.eventCount ?? "-"}</td>

                      <td>{date(row.requestedAt)}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={7} className="monitor-empty">
                      분석 작업 기록이 없습니다.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {/* ========================================
          16. 메시지별 AI 분석 흐름 추적
      ======================================== */}
      <section
        ref={traceSectionRef}
        className="monitor-panel"
        style={{ scrollMarginTop: "110px" }}
        aria-label="메시지별 AI 분석 흐름 추적"
      >
        <h2>채팅 메시지 분석 흐름 추적</h2>

        <p className="monitor-note">
          메시지 ID로 AI 판정 → DB 반영 → Redis 알림을 확인합니다. 기존 기록에는
          단계 로그가 없을 수 있습니다.
        </p>

        <form
          className="monitor-trace-form"
          onSubmit={(e) => {
            e.preventDefault();

            openTrace(messageInput);
          }}
        >
          <label htmlFor="trace-message-id">메시지 ID</label>

          <input
            id="trace-message-id"
            type="number"
            min="1"
            step="1"
            placeholder="예: 125"
            value={messageInput}
            onChange={(e) => {
              setMessageInput(e.target.value);
              setTraceInputError("");
            }}
          />

          <button type="submit" className="button button-outline">
            조회
          </button>

          <button
            type="button"
            className="button button-outline"
            onClick={() => {
              setMessageInput("");
              setTargetMessageId("");
              setTraceLoadedId("");
              setTraceErrorId("");
              setTraceInputError("");
            }}
          >
            초기화
          </button>
        </form>

        {traceInputError && (
          <p className="monitor-alert" role="alert">
            {traceInputError}
          </p>
        )}

        {targetMessageId &&
          traceLoadedId === targetMessageId &&
          advanced?.selectedPolicy && (
            <div className="monitor-health-item" style={{ marginBottom: 12 }}>
              <div>
                <strong>
                  메시지 #{advanced.selectedPolicy.messageId} 판정 결과
                </strong>
                <PolicyTag
                  code={
                    advanced.selectedPolicy.policyCode ??
                    advanced.selectedPolicy.analysisPolicyCode
                  }
                />
              </div>
              <small>
                최종 상태:{" "}
                {stateText(
                  advanced.selectedPolicy.analysisOutcome ||
                    advanced.selectedPolicy.analysisStatus,
                )}
              </small>
            </div>
          )}

        {!targetMessageId ? (
          <p className="monitor-empty">
            메시지 ID를 입력하거나 최근 판정 내역에서 '흐름 보기'를 누르세요.
          </p>
        ) : traceLoadedId !== targetMessageId ? (
          <p className="monitor-empty" role="status">
            메시지 #{targetMessageId} 분석 흐름을 조회하고 있습니다…
          </p>
        ) : traceErrorId === targetMessageId || !advanced ? (
          <p className="monitor-alert" role="alert">
            조회에 실패했습니다. 새로고침 후 다시 시도해 주세요.
          </p>
        ) : Object.prototype.hasOwnProperty.call(advanced, "selectedPolicy") &&
          !advanced.selectedPolicy ? (
          <p className="monitor-empty">해당 메시지 ID가 존재하지 않습니다.</p>
        ) : (advanced.trace || []).length ? (
          <ol className="monitor-trace-list">
            {advanced.trace.map((item, i) => (
              <li key={`${item.stage}-${i}`}>
                <div>
                  <strong>{stateText(item.stage)}</strong>

                  <StatusTag value={item.stage} />
                </div>

                <small>
                  {date(item.recordedAt)}

                  {item.durationMs != null
                    ? ` · ${duration(item.durationMs)}`
                    : ""}
                </small>
              </li>
            ))}
          </ol>
        ) : (
          <p className="monitor-empty">
            해당 메시지에 저장된 처리 단계가 없습니다. V21 적용 전 기록이거나
            아직 분석 단계 저장이 완료되지 않았을 수 있습니다.
          </p>
        )}
      </section>

      {/* ========================================
          17. 모니터링 안내
      ======================================== */}
      <p className="monitor-footnote">
        유의: 이 화면은 서버 로그 원문을 제공하지 않으며, 장애 발생 횟수의 영구
        이력은 별도 계측이 필요합니다. 503 오류의 원인이 FastAPI 비가용성인지
        API 인증·내부 오류인지는 서버 로그와 함께 확인해야 합니다.
      </p>
    </main>
  );
}
