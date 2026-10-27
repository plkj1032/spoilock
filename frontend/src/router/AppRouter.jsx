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

export default function AppRouter({
  user,
  pending,
  onLogin,
  onNotice,
  onProfileUpdated,
}) {
  return (
    <Routes>
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
        path="/chat"
        element={<ChatPage user={user} onLogin={onLogin} onNotice={onNotice} />}
      />

      <Route
        path="/chat/:roomId"
        element={
          pending ? (
            <main className="container main-content">로그인 확인 중...</main>
          ) : user ? (
            <ChatRoomPage
              key={`${user.id}:${window.location.pathname}`}
              user={user}
            />
          ) : (
            <main className="container main-content">
              <p>로그인 후 채팅방을 이용할 수 있습니다.</p>

              <button
                type="button"
                className="button button-primary"
                onClick={onLogin}
              >
                로그인
              </button>
            </main>
          )
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
          pending ? (
            <main className="container main-content">로그인 확인 중...</main>
          ) : user ? (
            <MyPage key={user.id} onProfileUpdated={onProfileUpdated} />
          ) : (
            <main className="container main-content">
              <p>로그인 후 마이페이지를 이용할 수 있습니다.</p>
              <button
                type="button"
                className="button button-primary"
                onClick={onLogin}
              >
                로그인
              </button>
            </main>
          )
        }
      />
      {/* 존재하지 않는 URL 처리 - 가장 마지막 */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
