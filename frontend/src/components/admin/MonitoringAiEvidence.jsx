const VERDICT_LABELS = {
  MATCH: "사건 일치",
  NO_MATCH: "일반 표현 사건 일치 없음",
  UNCERTAIN: "판단 불확실",
  ERROR: "분석 오류",
  SKIPPED_GENERAL: "일반 대화로 분석 생략",
  SKIPPED_UNRELATED: "관련 없는 대화로 분석 생략",
  UNKNOWN_VERDICT: "알 수 없는 판정",
};

function parseResponse(json) {
  if (!json) return { status: "missing", data: null };

  try {
    const data = JSON.parse(json);

    if (!data || typeof data !== "object" || Array.isArray(data)) {
      return { status: "invalid", data: null };
    }

    return { status: "ready", data };
  } catch {
    return { status: "invalid", data: null };
  }
}

function episodeList(values) {
  if (!Array.isArray(values)) return "응답에 목록 없음";

  const episodes = [...new Set(values)]
    .filter((value) => Number.isInteger(value) && value > 0)
    .sort((a, b) => a - b);

  return episodes.length
    ? episodes.map((episode) => `${episode}화`).join(", ")
    : "매칭 없음";
}

function basisLabel(value) {
  switch (value) {
    case "VERIFIED_FIRST_REVEAL":
      return "최초 공개 회차 검증됨";
    case "UNVERIFIED_SOURCE_EPISODE_FOR_DEMO_ONLY":
      return "검증되지 않은 데모용 회차";
    default:
      return value || "기록 없음";
  }
}

function Field({ label, children }) {
  return (
    <div className="monitor-detail__field">
      <dt>{label}</dt>
      <dd>{children}</dd>
    </div>
  );
}

export default function MonitoringAiEvidence({ json, rawVerdict }) {
  const { status, data } = parseResponse(json);
  const verdict = data?.analysis_verdict || rawVerdict;

  return (
    <section className="monitor-detail__decision">
      <h4>AI 원본 분석 결과</h4>

      <dl className="monitor-detail__grid">
        <Field label="AI 원본 판정">
          {verdict
            ? `${VERDICT_LABELS[verdict] ?? verdict} (${verdict})`
            : "기록 없음"}
        </Field>

        {status === "ready" && (
          <>
            <Field label="AI 검토 필요 표시">
              {data.needs_review === true
                ? "있음"
                : data.needs_review === false
                  ? "없음"
                  : "기록 없음"}
            </Field>

            <Field label="일반 표현 매칭 회차">
              {episodeList(data.matched_episodes)}
            </Field>

            <Field label="추측형 표현 매칭 회차">
              {episodeList(data.guess_episodes)}
            </Field>

            <Field label="일반 표현 대표 사건 ID">
              {data.matched_event_id || "없음"}
            </Field>

            <Field label="추측형 표현 사건 ID">
              {Array.isArray(data.guess_event_ids)
                ? data.guess_event_ids.join(", ") || "없음"
                : "응답에 목록 없음"}
            </Field>

            <Field label="일반 표현 회차 근거">
              {basisLabel(data.episode_basis)}
            </Field>

            <Field label="추측형 표현 회차 근거">
              {basisLabel(data.guess_basis)}
            </Field>
          </>
        )}
      </dl>

      {status === "missing" && (
        <p className="monitor-detail__note">
          저장된 AI 상세 응답이 없습니다. 상세 응답 저장 기능 적용 전
          메시지이거나, 분석 응답을 받지 못한 경우일 수 있습니다.
        </p>
      )}

      {status === "invalid" && (
        <p className="monitor-detail__note">
          저장된 AI 상세 응답을 읽을 수 없습니다. 응답 데이터 형식을 확인해
          주세요.
        </p>
      )}

      {status === "ready" && (
        <>
          <div className="monitor-detail__reason">
            <strong>AI 판정 이유</strong>
            <p>{data.reason || "응답에 판정 이유가 없습니다."}</p>
          </div>

          {data.error && (
            <div className="monitor-detail__reason">
              <strong>AI 오류 내용</strong>
              <p>{data.error}</p>
            </div>
          )}

          <p className="monitor-detail__note">
            일반 표현과 추측형 표현은 FastAPI의 응답 분류입니다. 최종 공개
            정책은 말투와 관계없이 매칭 회차와 작성 당시 시청 진도를 비교합니다.
            사건 ID 목록과 회차 목록은 같은 순서로 대응하는 목록이 아닙니다.
          </p>
        </>
      )}
    </section>
  );
}
