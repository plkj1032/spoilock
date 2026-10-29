import { NavLink } from "react-router-dom";
import Brand from "./Brand";
import Button from "./Button";
import "../../styles/Header.css";
import NotificationBell from "./NotificationBell";

export default function Header({ user, pending, onLogin, onLogout, busy }) {
  return (
    <header className="site-header">
      <div className="header-content">
        {/* 로고 */}
        <NavLink to="/" className="logo-link" aria-label="SPOILOCK 홈">
          <Brand />
        </NavLink>

        {/* 주 메뉴 */}
        <nav className="nav-links" aria-label="주 메뉴">
          <NavLink to="/dramas" end>
            드라마
          </NavLink>

          <NavLink to="/chat">채팅</NavLink>

          <NavLink to="/community">커뮤니티</NavLink>
        </nav>

        {/* 사용자 메뉴 */}
        <div className="header-actions">
          {pending ? (
            <span className="auth-loading">로그인 확인 중...</span>
          ) : user ? (
            <>
              {/* 마이페이지 */}
              <NavLink
                to="/mypage"
                className="user-greeting"
                aria-label={`${user.nickname}님 마이페이지`}
                title="마이페이지"
              >
                <strong>{user.nickname}</strong>님
              </NavLink>

              {/* 알림 */}
              <NotificationBell key={user.id} />

              {/* 관리자 전용 메뉴 */}
              {user.role === "ADMIN" && (
                <NavLink
                  to="/admin"
                  className="button button-outline admin-link"
                >
                  관리자
                </NavLink>
              )}

              {/* 로그아웃 */}
              <Button variant="subtle" disabled={busy} onClick={onLogout}>
                로그아웃
              </Button>
            </>
          ) : (
            <Button variant="primary" onClick={onLogin}>
              로그인
            </Button>
          )}
        </div>
      </div>
    </header>
  );
}
