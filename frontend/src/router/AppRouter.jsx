import { Navigate, Route, Routes } from "react-router-dom";

import HomePage from "../pages/HomePage";
import DramaListPage from "../pages/DramaListPage";
import ChatPage from "../pages/ChatPage";
import ChatRoomPage from "../pages/ChatRoomPage";
import CommunityPage from "../pages/CommunityPage";
import AdminDramaPage from "../pages/AdminDramaPage";

export default function AppRouter({ user, pending, onLogin, onNotice }) {
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

      <Route path="*" element={<Navigate to="/" replace />} />
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
    </Routes>
  );
}
