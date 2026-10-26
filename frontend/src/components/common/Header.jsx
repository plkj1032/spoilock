import { NavLink } from "react-router-dom";
import Brand from "./Brand";
import Button from "./Button";
import "../../styles/Header.css";
import NotificationBell from "./NotificationBell";

export default function Header({ user, pending, onLogin, onLogout, busy }) {
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

              <NotificationBell key={user.id} />
              {/* ADMIN에게만 관리자 버튼 표시 */}
              {user.role === "ADMIN" && (
                <NavLink
                  to="/admin/dramas"
                  className="button button-outline admin-link"
                >
                  관리자
                </NavLink>
              )}
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
