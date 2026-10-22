import { useEffect, useState } from "react";
import { NavLink, Navigate, Route, Routes } from "react-router-dom";
import { authApi } from "./api/authApi";
import "./App.css";

// 관리자 작품 등록 API가 붙기 전까지 사용하는 가상 드라마 데모 데이터입니다.
const demoDramas = [
  {
    id: 1,
    title: "심야의 목격자",
    genre: "미스터리 · 스릴러",
    watched: 4,
    total: 12,
    cover: "night",
    rating: "9.4",
  },
  {
    id: 2,
    title: "푸른 골목의 우리",
    genre: "청춘 · 로맨스",
    watched: 7,
    total: 16,
    cover: "alley",
    rating: "9.1",
  },
  {
    id: 3,
    title: "서울, 제로시",
    genre: "SF · 액션",
    watched: 0,
    total: 10,
    cover: "zero",
    rating: "8.9",
  },
  {
    id: 4,
    title: "사라진 파동",
    genre: "범죄 · 드라마",
    watched: 3,
    total: 8,
    cover: "wave",
    rating: "9.2",
  },
  {
    id: 5,
    title: "유리로 된 밤",
    genre: "추리 · 미스터리",
    watched: 10,
    total: 12,
    cover: "glass",
    rating: "8.8",
  },
  {
    id: 6,
    title: "마지막 플랫폼",
    genre: "판타지 · 로맨스",
    watched: 2,
    total: 16,
    cover: "platform",
    rating: "9.0",
  },
];

const demoRooms = [
  {
    id: "room-1",
    title: "4화까지 본 사람들 모여요",
    description: "자유 채팅 · 스포일러 자동 차단",
    members: 128,
    tag: "1~4화",
  },
  {
    id: "room-2",
    title: "떡밥 추리방",
    description: "추리 · 미시청 회차 보호",
    members: 86,
    tag: "전 회차",
  },
  {
    id: "room-3",
    title: "팬들의 수다방",
    description: "자유 대화 · 회차별 검열",
    members: 43,
    tag: "전체",
  },
];

function Brand() {
  return (
    <span className="brand">
      <span className="brand-mark" aria-hidden="true">
        🔒
      </span>
      <span>
        SPOI<span className="brand-accent">LOCK</span>
      </span>
    </span>
  );
}

function Header({ user, pending, onLogin, onLogout, busy }) {
  return (
    <header className="site-header">
      <div className="header-content">
        <NavLink to="/" className="logo-link" aria-label="SPOILOCK 홈">
          <Brand />
        </NavLink>
        <nav className="nav-links" aria-label="주 메뉴">
          <NavLink to="/" end>
            드라마
          </NavLink>
          <NavLink to="/chat">채팅</NavLink>
          <NavLink to="/community">커뮤니티</NavLink>
        </nav>
        <div className="header-actions">
          {pending ? (
            <span className="auth-loading">로그인 확인 중...</span>
          ) : user ? (
            <>
              <span className="user-greeting">
                <strong>{user.nickname}</strong>님
              </span>
              <button
                type="button"
                className="button button-subtle"
                disabled={busy}
                onClick={onLogout}
              >
                로그아웃
              </button>
            </>
          ) : (
            <button
              type="button"
              className="button button-primary"
              onClick={onLogin}
            >
              로그인
            </button>
          )}
        </div>
      </div>
    </header>
  );
}

function DramaCard({ drama, onAction }) {
  return (
    <article className="drama-card">
      <div className={`drama-cover cover-${drama.cover}`}>
        <span className="demo-mark">DEMO</span>
        <span className="rating">★ {drama.rating}</span>
      </div>
      <div className="drama-description">
        <p className="eyebrow muted">{drama.genre}</p>
        <h3>{drama.title}</h3>
        <div className="progress-label">
          <span>
            {drama.watched ? `${drama.watched}화 시청` : "진도 미설정"}
          </span>
          <span>총 {drama.total}화</span>
        </div>
        <div className="progress-track" aria-label={`${drama.title} 시청 진도`}>
          <span style={{ width: `${(drama.watched / drama.total) * 100}%` }} />
        </div>
        <button type="button" className="card-action" onClick={onAction}>
          시청 진도 설정 <span aria-hidden="true">›</span>
        </button>
      </div>
    </article>
  );
}

function HomePage({ user, onLogin, onNotice }) {
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
      <div className="drama-grid">
        {demoDramas.slice(0, 4).map((drama) => (
          <DramaCard
            key={drama.id}
            drama={drama}
            onAction={() =>
              onNotice("드라마·시청 진도 API 연결 전의 데모 카드입니다.")
            }
          />
        ))}
      </div>
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

function DramaListPage({ onNotice }) {
  return (
    <main className="container main-content">
      <div className="section-top">
        <div>
          <span className="section-overline">EXPLORE</span>
          <h1>드라마 둘러보기</h1>
          <p className="page-subtitle">
            저작권 또는 이용 허락을 확인한 작품을 등록할 예정입니다.
          </p>
        </div>
      </div>
      <div className="drama-grid all-dramas">
        {demoDramas.map((d) => (
          <DramaCard
            key={d.id}
            drama={d}
            onAction={() =>
              onNotice("백엔드 드라마 API 연결 후 이용할 수 있습니다.")
            }
          />
        ))}
      </div>
    </main>
  );
}

function ChatPage({ user, onLogin, onNotice }) {
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

function CommunityPage({ user, onLogin, onNotice }) {
  const action = () =>
    user
      ? onNotice("게시글 API 연결 뒤 작성 기능을 사용할 수 있습니다.")
      : onLogin();
  return (
    <main className="container main-content community-page">
      <span className="section-overline">COMMUNITY</span>
      <h1>모두의 이야기</h1>
      <button type="button" className="new-post" onClick={action}>
        <span className="avatar">S</span>
        <span>드라마에 대한 생각을 나눠보세요.</span>
        <strong>게시하기</strong>
      </button>
      <article className="sample-post">
        <div className="post-author">
          <span className="avatar">D</span>
          <strong>데모 사용자</strong>
          <span className="demo-label">화면 미리보기</span>
        </div>
        <p>스포일러 걱정 없이 작품 이야기를 나누는 공간이 될 거예요.</p>
        <div className="post-poster">SPOILOCK COMMUNITY</div>
      </article>
      <p className="demo-disclaimer">
        커뮤니티는 현재 정적 미리보기이며 게시글은 DB에 저장되지 않습니다.
      </p>
    </main>
  );
}

function AuthModal({
  mode,
  setMode,
  onClose,
  onLogin,
  onSignup,
  busy,
  error,
  setError,
}) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [nickname, setNickname] = useState("");
  const [message, setMessage] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const signup = mode === "signup";

  useEffect(() => {
    const onKey = (event) => {
      if (event.key === "Escape" && !busy) onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [busy, onClose]);

  async function submit(event) {
    event.preventDefault();
    setError("");
    setMessage("");
    if (signup) {
      const ok = await onSignup(email.trim(), password, nickname.trim());
      if (ok) {
        setPassword("");
        setMode("login");
        setMessage(
          "가입이 완료되었습니다. 방금 만든 계정으로 로그인해 주세요.",
        );
      }
    } else {
      await onLogin(email.trim(), password);
    }
  }

  function switchMode() {
    setMode(signup ? "login" : "signup");
    setError("");
    setMessage("");
  }

  return (
    <div
      className="modal-backdrop"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget && !busy) onClose();
      }}
    >
      <section
        className="auth-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="auth-title"
      >
        <button
          type="button"
          className="modal-close"
          aria-label="닫기"
          onClick={onClose}
          disabled={busy}
        >
          ×
        </button>
        <div className="dialog-brand">
          <Brand />
        </div>
        <h2 id="auth-title">
          {signup ? "SPOILOCK 시작하기" : "다시 만나서 반가워요"}
        </h2>
        <p className="dialog-description">
          {signup
            ? "안전한 드라마 커뮤니티에 참여하세요."
            : "로그인하고 안전한 대화를 이어가세요."}
        </p>
        <form onSubmit={submit} className="auth-form">
          <label htmlFor="auth-email">이메일</label>
          <input
            id="auth-email"
            type="email"
            autoComplete="email"
            placeholder="name@example.com"
            required
            maxLength={255}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <label htmlFor="auth-password">비밀번호</label>
          <div className="password-input">
            <input
              id="auth-password"
              type={showPassword ? "text" : "password"}
              autoComplete={signup ? "new-password" : "current-password"}
              placeholder="8자 이상 입력해 주세요"
              minLength={signup ? 8 : undefined}
              maxLength={signup ? 64 : undefined}
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            <button
              type="button"
              className="password-toggle"
              onClick={() => setShowPassword((s) => !s)}
              aria-label={showPassword ? "비밀번호 숨기기" : "비밀번호 표시"}
            >
              {showPassword ? "숨김" : "보기"}
            </button>
          </div>
          {signup && (
            <>
              <label htmlFor="auth-nickname">닉네임</label>
              <input
                id="auth-nickname"
                type="text"
                placeholder="사용할 닉네임을 입력해 주세요"
                required
                maxLength={50}
                value={nickname}
                onChange={(e) => setNickname(e.target.value)}
              />
            </>
          )}
          {message && (
            <p className="form-success" role="status">
              {message}
            </p>
          )}
          {error && (
            <p className="form-error" role="alert">
              {error}
            </p>
          )}
          <button
            type="submit"
            className="button button-primary submit-button"
            disabled={busy}
          >
            {busy ? "처리 중..." : signup ? "회원가입" : "로그인"}
          </button>
        </form>
        <p className="switch-auth">
          {signup ? "이미 계정이 있나요?" : "아직 회원이 아니신가요?"}{" "}
          <button type="button" onClick={switchMode} disabled={busy}>
            {signup ? "로그인" : "회원가입"}
          </button>
        </p>
        <p className="oauth-later">소셜 로그인은 추후 연동할 예정입니다.</p>
      </section>
    </div>
  );
}

function App() {
  const [user, setUser] = useState(null);
  const [pending, setPending] = useState(true);
  const [busy, setBusy] = useState(false);
  const [mode, setMode] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    let active = true;
    authApi
      .restore()
      .then((me) => {
        if (active) setUser(me);
      })
      .catch(() => {
        if (active) setNotice("백엔드 연결 상태를 확인해 주세요.");
      })
      .finally(() => {
        if (active) setPending(false);
      });
    return () => {
      active = false;
    };
  }, []);

  function openModal(which = "login") {
    setMode(which);
    setError("");
  }
  function closeModal() {
    if (!busy) {
      setMode(null);
      setError("");
    }
  }

  async function handleLogin(email, password) {
    setBusy(true);
    setError("");
    try {
      const me = await authApi.login(email, password);
      setUser(me);
      setMode(null);
      setNotice(`${me.nickname}님, 환영합니다!`);
      return true;
    } catch (err) {
      setError(err.message);
      return false;
    } finally {
      setBusy(false);
    }
  }

  async function handleSignup(email, password, nickname) {
    setBusy(true);
    setError("");
    try {
      await authApi.signup(email, password, nickname);
      return true;
    } catch (err) {
      setError(err.message);
      return false;
    } finally {
      setBusy(false);
    }
  }

  async function handleLogout() {
    setBusy(true);
    try {
      await authApi.logout();
      setUser(null);
      setNotice("로그아웃되었습니다.");
    } catch (err) {
      // 서버가 실패하면 HttpOnly 쿠키를 JS에서 삭제할 수 없으므로 로그인 상태를 유지합니다.
      setNotice(`로그아웃 실패: ${err.message}`);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <Header
        user={user}
        pending={pending}
        busy={busy}
        onLogin={() => openModal("login")}
        onLogout={handleLogout}
      />
      <Routes>
        <Route
          path="/"
          element={
            <HomePage
              user={user}
              onLogin={() => openModal("login")}
              onNotice={setNotice}
            />
          }
        />
        <Route
          path="/dramas"
          element={<DramaListPage onNotice={setNotice} />}
        />
        <Route
          path="/chat"
          element={
            <ChatPage
              user={user}
              onLogin={() => openModal("login")}
              onNotice={setNotice}
            />
          }
        />
        <Route
          path="/community"
          element={
            <CommunityPage
              user={user}
              onLogin={() => openModal("login")}
              onNotice={setNotice}
            />
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      {notice && (
        <div className="toast" role="status">
          <span>{notice}</span>
          <button
            type="button"
            aria-label="알림 닫기"
            onClick={() => setNotice("")}
          >
            ×
          </button>
        </div>
      )}
      {mode && (
        <AuthModal
          mode={mode}
          setMode={setMode}
          onClose={closeModal}
          onLogin={handleLogin}
          onSignup={handleSignup}
          busy={busy}
          error={error}
          setError={setError}
        />
      )}
    </>
  );
}

export default App;
