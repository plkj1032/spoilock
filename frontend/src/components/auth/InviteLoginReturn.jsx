import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

const STORAGE_KEY = "spoilock.inviteReturn";
const RETURN_LIMIT_MS = 30 * 60 * 1000;

// 서버에서 발급하는 XXXXX-XXXXX-XXXXX-XXXXX 형식만 허용
const INVITE_PATH = /^\/chat\/invite\/[A-Z2-9]{5}(?:-[A-Z2-9]{5}){3}$/;

export default function InviteLoginReturn({ userId }) {
  const navigate = useNavigate();

  useEffect(() => {
    if (userId == null) return;

    try {
      const raw = sessionStorage.getItem(STORAGE_KEY);

      if (!raw) return;

      // 한 번만 사용
      sessionStorage.removeItem(STORAGE_KEY);

      const saved = JSON.parse(raw);

      if (typeof saved.path !== "string" || typeof saved.savedAt !== "number") {
        return;
      }

      const elapsed = Date.now() - saved.savedAt;

      // 임의의 외부 URL로 이동하지 않도록 초대 경로만 허용
      if (
        !INVITE_PATH.test(saved.path) ||
        !Number.isFinite(elapsed) ||
        elapsed < 0 ||
        elapsed >= RETURN_LIMIT_MS
      ) {
        return;
      }

      navigate(saved.path, {
        replace: true,
      });
    } catch {
      // 잘못된 저장값이나 브라우저 저장소 오류는 무시
    }
  }, [userId, navigate]);

  return null;
}
