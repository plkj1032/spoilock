import { useEffect, useId, useRef } from "react";
import { createPortal } from "react-dom";
import "../../styles/ProfileEditModal.css";

export default function ProfileEditModal({
  email,
  nickname,
  saving,
  error,
  message,
  onNicknameChange,
  onSubmit,
  onClose,
}) {
  const dialogRef = useRef(null);
  const titleId = useId();
  const nicknameId = useId();
  const helpId = useId();
  const emailId = useId();

  useEffect(() => {
    const dialog = dialogRef.current;
    const previousFocus = document.activeElement;

    dialog.showModal();

    return () => {
      dialog.close();

      if (previousFocus instanceof HTMLElement && previousFocus.isConnected) {
        previousFocus.focus();
      }
    };
  }, []);

  return createPortal(
    <dialog
      ref={dialogRef}
      className="profile-edit-dialog"
      aria-labelledby={titleId}
      onCancel={(event) => {
        event.preventDefault();
        if (!saving) onClose();
      }}
    >
      <header className="profile-edit-header">
        <div>
          <span className="profile-edit-overline">MY PROFILE</span>
          <h2 id={titleId}>프로필 수정</h2>
          <p>다른 사람에게 표시되는 닉네임을 변경해요.</p>
        </div>

        <button
          type="button"
          className="profile-edit-close"
          aria-label="프로필 수정 닫기"
          onClick={onClose}
          disabled={saving}
        >
          ×
        </button>
      </header>

      <form className="profile-edit-form" onSubmit={onSubmit}>
        <div className="profile-edit-field">
          <label htmlFor={emailId}>이메일</label>
          <input
            id={emailId}
            type="text"
            value={email || "등록된 이메일 없음"}
            readOnly
          />
          <p className="profile-edit-help">이메일은 변경할 수 없어요.</p>
        </div>

        <div className="profile-edit-field">
          <label htmlFor={nicknameId}>닉네임</label>
          <input
            id={nicknameId}
            name="nickname"
            autoComplete="nickname"
            value={nickname}
            maxLength={20}
            required
            disabled={saving}
            aria-describedby={helpId}
            onChange={(event) => onNicknameChange(event.target.value)}
            autoFocus
          />
          <p className="profile-edit-help" id={helpId}>
            앞뒤 공백을 제외하고 2~20자로 입력해 주세요.
          </p>
        </div>

        {error && (
          <p className="profile-edit-error" role="alert">
            {error}
          </p>
        )}

        {message && (
          <p className="profile-edit-success" role="status">
            {message}
          </p>
        )}

        <footer className="profile-edit-footer">
          <button
            type="button"
            className="profile-edit-secondary"
            onClick={onClose}
            disabled={saving}
          >
            닫기
          </button>

          <button
            type="submit"
            className="profile-edit-primary"
            disabled={saving}
          >
            {saving ? "저장 중…" : "변경사항 저장"}
          </button>
        </footer>
      </form>
    </dialog>,
    document.body,
  );
}
