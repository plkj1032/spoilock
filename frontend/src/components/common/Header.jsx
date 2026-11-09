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
            <svg className="nav-menu-icon" viewBox="0 0 24 24" width="20" height="20"
              fill="none" stroke="currentColor" strokeWidth="1.7"
              strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
              <rect x="6" y="3" width="15" height="16" rx="2" /><path d="M3 7v14h14" /><path d="m12 7 5 4-5 4Z" />
            </svg>
            <span>드라마</span>
          </NavLink>

          <NavLink to="/chat">
            <svg className="nav-menu-icon" viewBox="0 0 24 24" width="20" height="20"
              fill="none" stroke="currentColor" strokeWidth="1.7"
              strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
              <path d="M4 3h16v13H9l-5 5Z" /><path d="M8 7h8M8 11h6" />
            </svg>
            <span>채팅</span>
          </NavLink>

          <NavLink to="/community">
            <svg className="nav-menu-icon" viewBox="0 0 24 24" width="20" height="20"
              fill="none" stroke="currentColor" strokeWidth="1.7"
              strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" focusable="false">
              <rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" />
            </svg>
            <span>커뮤니티</span>
          </NavLink>
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
