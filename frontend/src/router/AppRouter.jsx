import { Navigate, Route, Routes } from "react-router-dom";

import HomePage from "../pages/HomePage";
import DramaListPage from "../pages/DramaListPage";
import ChatPage from "../pages/ChatPage";
import ChatRoomPage from "../pages/ChatRoomPage";
import CommunityPage from "../pages/CommunityPage";
import AdminDramaPage from "../pages/AdminDramaPage";
import AdminSubtitlesPage from "../pages/AdminSubtitlesPage";
import MyPage from "../pages/MyPage";
import UserFeedPage from "../pages/UserFeedPage";
import AdminTermsPage from "../pages/AdminTermsPage";
import AdminPage from "../pages/AdminPage";
import AdminChatPage from "../pages/AdminChatPage";
import ChatInvitePage from "../pages/ChatInvitePage";
import DramaWatchPage from "../pages/DramaWatchPage";
import AdminMonitoringPage from "../pages/AdminMonitoringPage";
import KakaoSignupPage from "../pages/KakaoSignupPage";
import RequireLogin from "../components/auth/RequireLogin";

export default function AppRouter({
  user,
  pending,
  onLogin,
  onNotice,
  onProfileUpdated,
}) {
  return (
    <Routes>
      <Route path="/auth/kakao/complete" element={<KakaoSignupPage />} />
      <Route
        path="/"
        element={<HomePage user={user} onLogin={onLogin} onNotice={onNotice} />}
      />

      <Route
        path="/dramas"
        element={
          <DramaListPage user={user} onLogin={onLogin} onNotice={onNotice} />
        }
      />
      <Route
        path="/dramas/:dramaId/watch"
        element={
          <RequireLogin user={user} pending={pending}>
            <DramaWatchPage key={user?.id} />
          </RequireLogin>
        }
      />

      <Route
        path="/chat"
        element={<ChatPage user={user} onLogin={onLogin} onNotice={onNotice} />}
      />

      <Route
        path="/chat/invite/:code"
        element={
          <RequireLogin user={user} pending={pending}>
            <ChatInvitePage
              key={user?.id ?? "guest"}
              user={user}
              pending={pending}
              onLogin={onLogin}
            />
          </RequireLogin>
        }
      />

      <Route
        path="/chat/:roomId"
        element={
          <RequireLogin user={user} pending={pending}>
            <ChatRoomPage
              key={`${user?.id}:${window.location.pathname}`}
              user={user}
              onNotice={onNotice}
            />
          </RequireLogin>
        }
      />

      <Route
        path="/community"
        element={
          <CommunityPage user={user} onLogin={onLogin} onNotice={onNotice} />
        }
      />
      <Route
        path="/community/users/:userId"
        element={<UserFeedPage user={user} onLogin={onLogin} />}
      />
      <Route
        path="/admin"
        element={
          pending ? (
            <main className="container main-content">로그인 확인 중...</main>
          ) : user?.role === "ADMIN" ? (
            <AdminPage />
          ) : (
            <Navigate to="/" replace />
          )
        }
      />
      {/* 관리자 AI 모니터링 */}
      <Route
        path="/admin/monitoring"
        element={
          pending ? (
            <main className="container main-content">로그인 확인 중...</main>
          ) : user?.role === "ADMIN" ? (
            <AdminMonitoringPage />
          ) : (
            <Navigate to="/" replace />
          )
        }
      />

      <Route
        path="/admin/chats"
        element={
          pending ? (
            <main className="container main-content">로그인 확인 중...</main>
          ) : user?.role === "ADMIN" ? (
            <AdminChatPage key={user.id} user={user} />
          ) : (
            <Navigate to="/" replace />
          )
        }
      />

      {/* 관리자 드라마 관리 */}
      <Route
        path="/admin/dramas"
        element={
          pending ? (
            <main className="container main-content">로그인 확인 중...</main>
          ) : user?.role === "ADMIN" ? (
            <AdminDramaPage />
          ) : (
            <Navigate to="/" replace />
          )
        }
      />
      {/* 관리자 자막 및 AI 관리 */}
      <Route
        path="/admin/dramas/:dramaId/subtitles"
        element={
          pending ? (
            <main className="container main-content">로그인 확인 중...</main>
          ) : user?.role === "ADMIN" ? (
            <AdminSubtitlesPage />
          ) : (
            <Navigate to="/" replace />
          )
        }
      />
      <Route
        path="/mypage"
        element={
          <RequireLogin user={user} pending={pending}>
            <MyPage key={user?.id} onProfileUpdated={onProfileUpdated} />
          </RequireLogin>
        }
      />
      <Route
        path="/admin/terms"
        element={
          pending ? (
            <main className="container main-content">로그인 확인 중...</main>
          ) : user?.role === "ADMIN" ? (
            <AdminTermsPage />
          ) : (
            <Navigate to="/" replace />
          )
        }
      />
      {/* 존재하지 않는 URL 처리 - 가장 마지막 */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
