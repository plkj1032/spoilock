import { useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom";

const INVITE_PATH = /^\/chat\/invite\/[A-Z2-9]{5}(?:-[A-Z2-9]{5}){3}$/;

export default function RequireLogin({ user, pending, children }) {
  const { pathname } = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    // 로그인 복구 요청이 끝나기 전에는 이동하지 않음
    if (pending || user) return;

    // 초대 링크는 홈에서 로그인한 뒤 돌아올 수 있도록 보관
    if (INVITE_PATH.test(pathname)) {
      try {
        sessionStorage.setItem(
          "spoilock.inviteReturn",
          JSON.stringify({
            path: pathname,
            savedAt: Date.now(),
          }),
        );
      } catch {
        // 저장소 사용이 불가능해도 홈 이동은 진행
      }
    }

    navigate("/", { replace: true });
  }, [pending, user, pathname, navigate]);

  if (pending) {
    return (
      <main className="container main-content" role="status">
        로그인 확인 중…
      </main>
    );
  }

  if (!user) return null;

  return children;
}
