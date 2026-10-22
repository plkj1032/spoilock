import { NavLink } from "react-router-dom";
import { demoRooms } from "../data/demoData";
import DramaGallery from "../components/drama/DramaGallery";

export default function HomePage({ user, onLogin, onNotice }) {
  const viewRoom = () => {
    if (!user) onLogin();
    else
      onNotice(
        "채팅방 입장은 준호의 채팅 API를 연결한 뒤 활성화할 예정입니다.",
      );
  };
  return (
    <main className="container main-content">
      <div className="section-top">
        <div>
          <span className="section-overline">NOW STREAMING</span>
          <h1>지금 시청중인 드라마</h1>
        </div>
        <NavLink to="/dramas" className="section-link">
          전체 보기 ›
        </NavLink>
      </div>
      <DramaGallery
        key={user?.id ?? "guest"}
        user={user}
        onLogin={onLogin}
        onNotice={onNotice}
        limit={4}
      />
      <div className="section-top rooms-title">
        <div>
          <span className="section-overline">LIVE ROOMS</span>
          <h2>심야의 목격자 채팅방</h2>
        </div>
        <button
          type="button"
          className="button button-outline"
          onClick={viewRoom}
        >
          ＋ 채팅방 만들기
        </button>
      </div>
      <div className="room-layout">
        <div className="room-list">
          {demoRooms.map((room) => (
            <button
              type="button"
              className="room-card"
              key={room.id}
              onClick={viewRoom}
            >
              <span className="room-icon" aria-hidden="true">
                ▢
              </span>
              <span className="room-copy">
                <strong>
                  {room.title} <em>{room.tag}</em>
                </strong>
                <small>{room.description}</small>
              </span>
              <span className="room-members">♧ {room.members}명</span>
              <span className="room-enter">입장 ›</span>
            </button>
          ))}
        </div>
        <aside className="safety-card">
          <span className="safety-icon" aria-hidden="true">
            ♢
          </span>
          <span className="section-overline">SPOILOCK SAFE</span>
          <h3>안심하고 대화하세요</h3>
          <p>
            설정한 시청 진도 이후의 내용은 AI가 자동으로 감지하고 보호하는
            서비스를 만들고 있어요.
          </p>
          <div className="safety-bars">
            <span />
            <span />
            <span />
          </div>
          <small>
            현재 보호 수준 <strong>개발 중</strong>
          </small>
        </aside>
      </div>
      <p className="demo-disclaimer">
        화면의 드라마·평점·채팅방은 프론트엔드 테스트용 가상 데이터입니다. 실제
        데이터 연동 전까지 저장되지 않습니다.
      </p>
    </main>
  );
}
