import "../../styles/MonitoringMessageDetail.css";
import MonitoringAiEvidence from "./MonitoringAiEvidence";

const ROOM_LABELS = {
  DEFAULT: "기본 토론방",
  CUSTOM: "자유 채팅방",
  PRIVATE: "비밀 채팅방",
};

const ANALYSIS_LABELS = {
  PENDING: "분석 대기",
  PROCESSING: "분석 중",
  COMPLETED: "분석 완료",
  FAILED: "분석 실패",
  SKIPPED: "분석 제외",
};

function formatTime(value) {
  if (!value) return "기록 없음";
  return String(value).replace("T", " ").slice(0, 19);
}

function publicationLabel(message) {
  if (message.moderationStatus === "HIDDEN") {
    return "관리자 숨김";
  }

  if (message.moderationStatus === "REMOVED") {
    return "관리자 삭제";
  }

  if (message.analysisStatus !== "COMPLETED") {
    return ANALYSIS_LABELS[message.analysisStatus] ?? "상태 확인 필요";
  }

  if (message.moderationStatus !== "VISIBLE") {
    return "검토 대기";
  }

  if (message.requiredUntilFinale) {
    return message.requiredEpisode != null
      ? `마지막 회차 기준 · ${message.requiredEpisode}화 이상 공개`
      : "마지막 회차 기준 · 회차 확인 필요";
  }

  if (message.requiredEpisode == null) {
    return "전체 공개";
  }

  if (message.requiredEpisode <= 0) {
    return "공개 기준 확인 필요";
  }

  return `${message.requiredEpisode}화 이상 시청자에게 공개`;
}

function Info({ label, children }) {
  return (
    <div className="monitor-detail__field">
      <dt>{label}</dt>
      <dd>{children}</dd>
    </div>
  );
}

export default function MonitoringMessageDetail({ message }) {
  return (
    <article className="monitor-detail">
      <header className="monitor-detail__header">
        <div>
          <span className="monitor-detail__eyebrow">MESSAGE DETAIL</span>
          <h3>메시지 #{message.messageId}</h3>
        </div>

        <span className="monitor-detail__badge">
          {publicationLabel(message)}
        </span>
      </header>
      <section className="monitor-detail__content">
        <h4>채팅 원문</h4>
        <p>{message.content || "(내용 없음)"}</p>
      </section>

      <dl className="monitor-detail__grid">
        <Info label="보낸 사람">
          {message.senderNickname} · ID {message.senderId}
        </Info>

        <Info label="작품">{message.dramaTitle}</Info>

        <Info label="채팅방">
          {message.roomName} · ID {message.roomId}
        </Info>

        <Info label="채팅방 종류">
          {ROOM_LABELS[message.roomType] ?? message.roomType}
        </Info>

        <Info label="전송 시각">{formatTime(message.createdAt)}</Info>

        <Info label="분석 완료 시각">{formatTime(message.analyzedAt)}</Info>
      </dl>

      <MonitoringAiEvidence
        json={message.aiResponseJson}
        rawVerdict={message.rawVerdict}
      />

      <section className="monitor-detail__decision">
        <h4>
          판정 근거 :
          <strong>
            {message?.policyDetail ||
              message?.policyCode ||
              "저장된 정책 사유가 없습니다."}
          </strong>
        </h4>

        <dl className="monitor-detail__grid">
          <Info label="작성 당시 시청 진도">
            {message.authorWatchedEpisode == null
              ? "기록 없음 · 이전 메시지"
              : message.authorWatchedEpisode === 0
                ? "0화 · 미시청"
                : `${message.authorWatchedEpisode}화`}
          </Info>

          <Info label="공개 기준">{publicationLabel(message)}</Info>

          <Info label="분석 상태">
            {ANALYSIS_LABELS[message.analysisStatus] ?? message.analysisStatus}
          </Info>

          <Info label="AI 판정 처리시간">
            {message.durationMs == null
              ? "기록 없음"
              : `${message.durationMs}ms`}
          </Info>
        </dl>

        <p className="monitor-detail__note"></p>
      </section>
    </article>
  );
}
