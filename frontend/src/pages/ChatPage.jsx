import { demoRooms } from "../data/demoData";

export default function ChatPage({ user, onLogin, onNotice }) {
  const enter = () =>
    user
      ? onNotice(
          "채팅 데이터는 준호의 WebSocket / REST API 연결을 기다리고 있어요.",
        )
      : onLogin();
  return (
    <main className="container main-content">
      <span className="section-overline">LIVE ROOMS</span>
      <h1>채팅방</h1>
      <p className="page-subtitle">
        화면은 먼저 준비하고, 실제 입장·메시지는 채팅 API 연결 후 활성화합니다.
      </p>
      <div className="room-list standalone-rooms">
        {demoRooms.map((room) => (
          <button
            type="button"
            className="room-card"
            key={room.id}
            onClick={enter}
          >
            <span className="room-icon" aria-hidden="true">
              ▢
            </span>
            <span className="room-copy">
              <strong>{room.title}</strong>
              <small>{room.description}</small>
            </span>
            <span className="room-enter">입장 ›</span>
          </button>
        ))}
      </div>
    </main>
  );
}
