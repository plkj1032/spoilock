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
// 2. AI 정책 코드 한글 표시
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
  return POLICY_LABELS[code] ?? "기타 정책 판정";
}

function policyTone(code) {
  if (!code) return "neutral";

  if (code === "GUESS_UNKNOWN") return "good";

  if (/FAILED|ERROR|INVALID|EMPTY|UNKNOWN_VERDICT/.test(code)) {
    return "danger";
  }

  if (/HELD|REVIEW|UNCERTAIN|UNVERIFIED|NO_EVENTS|GUESS_KNOWN/.test(code)) {
    return "warn";
  }

  return "good";
}

// ========================================
// 3. 공통 포맷 함수
// ========================================
function number(value) {
  return Number(value ?? 0).toLocaleString("ko-KR");
}

function percent(part, total) {
  return total ? `${((part / total) * 100).toFixed(1)}%` : "0.0%";
}

function duration(value) {
  if (value == null) return "측정값 없음";

  const ms = Number(value);

  return ms >= 1000 ? `${(ms / 1000).toFixed(2)}초` : `${ms}ms`;
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
// 4. 공통 컴포넌트
// ========================================
function StatusTag({ value }) {
  return (
    <span className={`monitor-tag monitor-tag-${tone(value)}`}>
      {stateText(value)}
    </span>
  );
}

function PolicyTag({ code, detail }) {
  // V24: 안전하게 저장된 상세 정책을 우선 표시
  return (
    <span
      className={`monitor-tag monitor-tag-${policyTone(code)}`}
      style={{
        whiteSpace: "normal",
        lineHeight: 1.6,
        overflowWrap: "anywhere",
      }}
    >
      {detail || policyLabel(code)}
    </span>
  );
}

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
// 5. 채팅 처리량 차트
// ========================================
function ChatChart({ timeline, range }) {
  const peak = Math.max(1, ...timeline.map((row) => row.total ?? 0));

  if (!timeline.length) {
    return <p className="monitor-empty">선택한 기간에 채팅 기록이 없습니다.</p>;
  }

  return (
    <div className="monitor-chart" role="img" aria-label="채팅 처리량">
      {timeline.map((row) => (
        <div className="monitor-bar-row" key={row.period}>
          <span className="monitor-bar-label">
            {row.period.slice(range === "24h" ? 11 : 5)}
          </span>

          <div className="monitor-bars">
            <div
              className="monitor-bar monitor-bar-total"
              style={{
                width: `${((row.total ?? 0) / peak) * 100}%`,
              }}
              title={`채팅 ${row.total}건`}
            />

            <div
              className="monitor-bar monitor-bar-gated"
              style={{
                width: `${((row.episodeGated ?? 0) / peak) * 100}%`,
              }}
              title={`진도 제한 ${row.episodeGated}건`}
            />

            <div
              className="monitor-bar monitor-bar-failed"
              style={{
                width: `${((row.failed ?? 0) / peak) * 100}%`,
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
// 6. 공통 페이징 컴포넌트
// ========================================
function MonitoringPagination({ data, page, onChange, label }) {
  if (!data) return null;

  const totalPages = Number(data.totalPages ?? 0);
  const total = Number(data.total ?? 0);

  return (
    <nav className="monitor-pagination" aria-label={`${label} 페이지 이동`}>
      <span>총 {number(total)}건 · 페이지당 20건</span>

      <div>
        <button
          type="button"
          disabled={page <= 0}
          onClick={() => onChange(page - 1)}
        >
          이전
        </button>

        <span>
          {totalPages === 0 ? 0 : page + 1}
          {" / "}
          {totalPages}
        </span>

        <button
          type="button"
          disabled={totalPages === 0 || page >= totalPages - 1}
          onClick={() => onChange(page + 1)}
        >
          다음
        </button>
      </div>
    </nav>
  );
}

// ========================================
// 7. 채팅 원문 컴포넌트
// ========================================
function ChatContent({ content }) {
  if (!content) return "-";

  if (content.length <= 80) {
    return <span style={{ overflowWrap: "anywhere" }}>{content}</span>;
  }

  return (
    <details className="monitor-message-details">
      <summary>{content.slice(0, 80)}…</summary>
      <div>{content}</div>
    </details>
  );
}

// ========================================
// 8. 메인 페이지
// ========================================
export default function AdminMonitoringPage() {
  // 통계 조회 조건
  const [range, setRange] = useState("24h");
  const [dramaId, setDramaId] = useState("");

  // 각각 독립적인 페이징
  const [chatPage, setChatPage] = useState(0);
  const [jobPage, setJobPage] = useState(0);

  // 기존 통계 데이터
  const [chats, setChats] = useState(null);
  const [advanced, setAdvanced] = useState(null);

  // V24 전체 이력 데이터
  const [chatHistory, setChatHistory] = useState(null);
  const [jobHistory, setJobHistory] = useState(null);

  // 메시지 추적
  const [messageInput, setMessageInput] = useState("");
  const [targetMessageId, setTargetMessageId] = useState("");
  const [traceLoadedId, setTraceLoadedId] = useState("");
  const [traceErrorId, setTraceErrorId] = useState("");
  const [traceInputError, setTraceInputError] = useState("");
  const [selectedTraceRow, setSelectedTraceRow] = useState(null);

  const traceSectionRef = useRef(null);

  // 화면 상태
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [updated, setUpdated] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);

  // ========================================
  // 9. API 데이터 조회 및 자동 갱신
  // ========================================
  useEffect(() => {
    let mounted = true;
    let currentController = null;

    async function reload() {
      if (document.hidden) return;

      currentController?.abort();

      const controller = new AbortController();
      currentController = controller;

      const base = {
        range,
        dramaId,
        signal: controller.signal,
      };

      const responses = await Promise.allSettled([
        // 기존 채팅 통계
        adminMonitoringApi.chats(base),

        // 기존 확장 모니터링 및 흐름 추적
        adminMonitoringApi.advanced({
          ...base,
          messageId: targetMessageId,
        }),

        // V24 채팅 이력
        adminMonitoringApi.chatHistory({
          page: chatPage,
          dramaId,
          signal: controller.signal,
        }),

        // V24 자막/영상 분석 이력
        adminMonitoringApi.jobHistory({
          page: jobPage,
          dramaId,
          signal: controller.signal,
        }),
      ]);

      if (!mounted || controller.signal.aborted) {
        return;
      }

      const [chatResult, advancedResult, historyResult, jobsResult] = responses;

      const failed = [];

      // 채팅 통계
      if (chatResult.status === "fulfilled") {
        setChats(chatResult.value);
      } else {
        setChats(null);
        failed.push(`채팅 통계: ${chatResult.reason?.message || "조회 실패"}`);
      }

      // 확장 모니터링
      if (advancedResult.status === "fulfilled") {
        setAdvanced(advancedResult.value);
        setTraceLoadedId(targetMessageId);
        setTraceErrorId("");
      } else {
        setAdvanced(null);
        setTraceLoadedId(targetMessageId);
        setTraceErrorId(targetMessageId);

        failed.push(
          `확장 모니터링: ${advancedResult.reason?.message || "조회 실패"}`,
        );
      }

      // 채팅 전체 이력
      if (historyResult.status === "fulfilled") {
        setChatHistory(historyResult.value);
      } else {
        setChatHistory(null);

        failed.push(
          `채팅 이력: ${historyResult.reason?.message || "조회 실패"}`,
        );
      }

      // 자막/영상 작업 전체 이력
      if (jobsResult.status === "fulfilled") {
        setJobHistory(jobsResult.value);
      } else {
        setJobHistory(null);

        failed.push(
          `분석 작업 이력: ${jobsResult.reason?.message || "조회 실패"}`,
        );
      }

      setError(failed.join(" / "));

      if (failed.length === 0) {
        setUpdated(new Date());
      }

      setLoading(false);
    }

    void reload();

    // 10초 자동 갱신
    const timer = window.setInterval(() => {
      void reload();
    }, 10000);

    // 탭 복귀 시 갱신
    const onVisible = () => {
      if (!document.hidden) {
        void reload();
      }
    };

    document.addEventListener("visibilitychange", onVisible);

    return () => {
      mounted = false;
      currentController?.abort();
      window.clearInterval(timer);
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, [range, dramaId, chatPage, jobPage, targetMessageId, reloadKey]);

  // ========================================
  // 10. 메시지 분석 흐름 열기
  // ========================================
  function openTrace(value) {
    const row = value && typeof value === "object" ? value : null;

    const id = String(row ? row.messageId : (value ?? "")).trim();

    if (!/^[1-9]\d*$/.test(id) || !Number.isSafeInteger(Number(id))) {
      setTraceInputError("메시지 ID에 1 이상의 정수를 입력해 주세요.");
      return;
    }

    setTraceInputError("");
    setMessageInput(id);
    setSelectedTraceRow(row);
    setTargetMessageId(id);
    setTraceLoadedId("");
    setTraceErrorId("");

    // 같은 메시지를 눌러도 재조회
    setReloadKey((key) => key + 1);

    window.requestAnimationFrame(() => {
      traceSectionRef.current?.scrollIntoView({
        behavior: "smooth",
        block: "start",
      });
    });
  }

  // ========================================
  // 11. 공통 파생 데이터
  // ========================================
  const summary = chats?.summary;
  const timeline = chats?.timeline || [];
  const jobs = advanced?.jobs;

  const chatRows = chatHistory?.items || [];
  const jobRows = jobHistory?.items || [];

  // ========================================
  // 12. 화면
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
          채팅 판정, 자막·영상 분석, AI 정책 사유, 처리시간 및 서버 연결 상태를
          조회합니다. 채팅 원문은 관리자에게만 표시됩니다.
        </p>
      </header>

      {/* ========================================
          13. 조회 필터
      ======================================== */}
      <section className="monitor-toolbar" aria-label="조회 필터">
        <label>
          통계 조회 기간
          <select value={range} onChange={(e) => setRange(e.target.value)}>
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

              // 작품 변경 시 두 목록 모두 첫 페이지로
              setChatPage(0);
              setJobPage(0);
            }}
          >
            <option value="">전체 작품</option>

            {(chats?.dramas || []).map((row) => (
              <option key={row.id} value={row.id}>
                {row.title}
              </option>
            ))}
          </select>
        </label>

        <button
          type="button"
          className="button button-outline"
          onClick={() => {
            setReloadKey((key) => key + 1);
          }}
        >
          새로고침
        </button>
      </section>

      <p className="monitor-note">
        통계 기간은 위의 요약 통계와 처리량에 적용됩니다. 아래 채팅 및 분석 작업
        이력은 전체 기간을 조회합니다.
      </p>

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
          14. 서버 연결 상태
      ======================================== */}
      {advanced && (
        <section className="monitor-panel">
          <h2>
            서비스 연결 상태{" "}
            <span className="monitor-subtitle">
              응답 서버: {advanced.nodeName || "확인 불가"}
            </span>
          </h2>

          <p className="monitor-note">
            FastAPI는 TCP 포트 연결 상태를 확인합니다. 실제 AI 판정 API 성공
            여부와는 다를 수 있습니다.
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

              <small>현재 응답한 Spring Boot 서버 기준</small>
            </div>
          </div>
        </section>
      )}

      {/* ========================================
          15. 채팅 통계
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
              hint="측정된 분석 기준"
            />

            <Metric
              label="P95 처리시간"
              value={duration(summary.p95DurationMs)}
              hint="측정된 분석 기준"
            />

            <Metric
              label="진행 / 대기"
              value={`${number(
                summary.processing,
              )} / ${number(summary.pending)}`}
              hint="현재 메시지 상태"
            />
          </section>

          <section className="monitor-panel">
            <h2>{range === "24h" ? "시간대별" : "일별"} 채팅 처리량</h2>

            <p className="monitor-note">
              파랑: 전체 · 주황: 진도 제한 · 빨강: 분석 실패
            </p>

            <ChatChart timeline={timeline} range={range} />
          </section>
        </>
      )}

      {/* ========================================
          16. 채팅 판정 전체 이력
      ======================================== */}
      <section className="monitor-panel">
        <h2>
          채팅 판정 이력{" "}
          <span className="monitor-subtitle">전체 기록 · 20건씩</span>
        </h2>

        <p className="monitor-note">
          채팅 원문, AI 판정 결과, 상세 정책 사유 및 처리시간을 확인합니다. 기존
          기록에는 정책 상세 정보가 없을 수 있습니다.
        </p>

        <div className="monitor-table-wrap">
          <table className="monitor-table">
            <thead>
              <tr>
                <th>메시지 ID</th>
                <th>작품</th>
                <th>채팅 내용</th>
                <th>판정</th>
                <th>정책 사유</th>
                <th>처리시간</th>
                <th>등록시각</th>
                <th>추적</th>
              </tr>
            </thead>

            <tbody>
              {chatRows.length > 0 ? (
                chatRows.map((row) => (
                  <tr key={row.messageId}>
                    <td>#{row.messageId}</td>

                    <td>{row.dramaTitle || "-"}</td>

                    <td className="monitor-chat-content">
                      <ChatContent content={row.content} />
                    </td>

                    <td>
                      <StatusTag
                        value={row.analysisOutcome || row.analysisStatus}
                      />
                    </td>

                    <td>
                      <PolicyTag
                        code={row.policyCode}
                        detail={row.policyDetail}
                      />
                    </td>

                    <td>{duration(row.durationMs)}</td>

                    <td>{date(row.createdAt)}</td>

                    <td>
                      <button
                        type="button"
                        className="monitor-text-button"
                        onClick={() => openTrace(row)}
                      >
                        흐름 보기
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={8} className="monitor-empty">
                    조회할 채팅 기록이 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <MonitoringPagination
          data={chatHistory}
          page={chatPage}
          onChange={setChatPage}
          label="채팅 판정"
        />
      </section>

      {/* ========================================
          17. 자막·영상 AI 분석 이력
      ======================================== */}
      <section className="monitor-panel">
        <h2>
          자막·영상 AI 분석 작업{" "}
          <span className="monitor-subtitle">전체 기록 · 20건씩</span>
        </h2>

        <p className="monitor-note">
          이전에 분석했던 회차까지 확인할 수 있습니다. 같은 회차를 재분석한
          경우에는 현재 DB에 남아 있는 최신 작업 상태를 표시합니다.
        </p>

        {jobs && (
          <div className="monitor-stats monitor-job-stats">
            <Metric
              label="선택 기간 전체 작업"
              value={`${number(jobs.total)}건`}
            />

            <Metric
              label="대기 / 진행"
              value={`${number(jobs.pending)} / ${number(jobs.processing)}`}
            />

            <Metric label="완료" value={`${number(jobs.completed)}건`} />

            <Metric label="실패" value={`${number(jobs.failed)}건`} />
          </div>
        )}

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
              {jobRows.length > 0 ? (
                jobRows.map((row) => (
                  <tr key={row.episodeId}>
                    <td>#{row.episodeId}</td>

                    <td>{row.dramaTitle || "-"}</td>

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

        <MonitoringPagination
          data={jobHistory}
          page={jobPage}
          onChange={setJobPage}
          label="자막 영상 분석"
        />
      </section>

      {/* ========================================
          18. 채팅 분석 흐름 추적
      ======================================== */}
      <section
        ref={traceSectionRef}
        className="monitor-panel"
        style={{ scrollMarginTop: "110px" }}
        aria-label="채팅 메시지 분석 흐름 추적"
      >
        <h2>채팅 메시지 분석 흐름 추적</h2>

        <p className="monitor-note">
          메시지별 AI 판정 → DB 반영 → Redis 알림을 확인합니다. 단계 기록이
          저장되지 않은 이전 메시지에는 이력이 없을 수 있습니다.
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
            placeholder="예: 458"
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
              setSelectedTraceRow(null);
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

        {targetMessageId && selectedTraceRow && (
          <div className="monitor-health-item" style={{ marginBottom: 14 }}>
            <div>
              <strong>메시지 #{selectedTraceRow.messageId}</strong>

              <StatusTag
                value={
                  selectedTraceRow.analysisOutcome ||
                  selectedTraceRow.analysisStatus
                }
              />
            </div>

            <small>
              정책 사유:{" "}
              {selectedTraceRow.policyDetail ||
                policyLabel(selectedTraceRow.policyCode)}
            </small>

            <small>처리시간: {duration(selectedTraceRow.durationMs)}</small>
          </div>
        )}

        {!targetMessageId ? (
          <p className="monitor-empty">
            메시지 ID를 입력하거나 채팅 판정 이력에서 '흐름 보기'를 클릭하세요.
          </p>
        ) : traceLoadedId !== targetMessageId ? (
          <p className="monitor-empty" role="status">
            메시지 #{targetMessageId} 분석 흐름 조회 중…
          </p>
        ) : traceErrorId === targetMessageId || !advanced ? (
          <p className="monitor-alert" role="alert">
            흐름 조회에 실패했습니다.
          </p>
        ) : (advanced.trace || []).length > 0 ? (
          <ol className="monitor-trace-list">
            {advanced.trace.map((item, index) => (
              <li key={`${item.stage}-${index}`}>
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
            해당 메시지에 저장된 단계별 처리 기록이 없습니다. V24 백엔드의
            MonitoringTraceService 기록 연결이 적용되었는지 확인해 주세요.
          </p>
        )}
      </section>

      {/* ========================================
          19. 안내
      ======================================== */}
      <p className="monitor-footnote">
        관리자 전용 모니터링입니다. AI 처리시간은 판정 구간의 측정값이며, 서버
        전체 응답시간과는 다를 수 있습니다. 서버 장애 분석 시에는 journalctl
        로그와 함께 확인해 주세요.
      </p>
    </main>
  );
}
