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
      <h4>
        AI 원본 분석 결과 :
        <strong>{data?.reason || "응답에 판정 이유가 없습니다."}</strong>
      </h4>

      <dl className="monitor-detail__grid">
        <Field label="AI 원본 판정">
          {verdict
            ? `${VERDICT_LABELS[verdict] ?? verdict} (${verdict})`
            : "기록 없음"}
        </Field>

        {status === "ready" && (
          <>
            <Field label="매칭 회차">
              {episodeList(data.matched_episodes)}
            </Field>

            <Field label="대표 사건 ID">
              {data.matched_event_id || "없음"}
            </Field>

            <Field label="회차 근거">{basisLabel(data.episode_basis)}</Field>
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
          {data.error && (
            <div className="monitor-detail__reason">
              <strong>AI 오류 내용</strong>
              <p>{data.error}</p>
            </div>
          )}

          <p className="monitor-detail__note"></p>
        </>
      )}
    </section>
  );
}
