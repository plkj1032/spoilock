import { useEffect, useRef } from "react";
import { createPortal } from "react-dom";
import { useBlocker } from "react-router-dom";

import "../../styles/UploadLeaveGuard.css";

export default function UploadLeaveGuard({ uploadCount }) {
  const dialogRef = useRef(null);
  const uploading = uploadCount > 0;

  // 다른 페이지로 이동할 때 차단.
  // 현재 페이지 안의 회차 선택은 라우터 이동이 아니므로 허용됨.
  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) =>
      uploading && currentLocation.pathname !== nextLocation.pathname,
  );

  // 새로고침, 탭 닫기, 외부 사이트 이동
  useEffect(() => {
    if (!uploading) return;

    function handleBeforeUnload(event) {
      event.preventDefault();
      event.returnValue = "";
    }

    window.addEventListener("beforeunload", handleBeforeUnload);

    return () => {
      window.removeEventListener("beforeunload", handleBeforeUnload);
    };
  }, [uploading]);

  // 사이트 내부 이동 시 중앙 확인창 표시
  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;

    if (blocker.state === "blocked" && uploading) {
      if (!dialog.open) {
        dialog.showModal();
      }
      return;
    }

    if (dialog.open) {
      dialog.close();
    }

    // 확인창을 띄운 사이 업로드가 모두 끝나면 현재 화면 유지
    if (blocker.state === "blocked" && !uploading) {
      blocker.reset();
    }
  }, [blocker, uploading]);

  function stayOnPage() {
    if (blocker.state === "blocked") {
      blocker.reset();
    }
  }

  function leavePage() {
    if (blocker.state === "blocked") {
      blocker.proceed();
    }
  }

  return createPortal(
    <dialog
      ref={dialogRef}
      className="upload-leave-dialog"
      aria-labelledby="upload-leave-title"
      aria-describedby="upload-leave-description"
      onCancel={(event) => {
        event.preventDefault();
        stayOnPage();
      }}
    >
      <span className="upload-leave-dialog__eyebrow">UPLOAD IN PROGRESS</span>

      <h2 id="upload-leave-title">영상 업로드가 진행 중이에요</h2>

      <p id="upload-leave-description">
        현재 {uploadCount}개의 영상을 업로드하고 있습니다.
        <br />이 페이지를 떠나면 업로드 진행 상태를 확인할 수 없으며, 업로드가
        완료되지 않을 수 있어요.
      </p>

      <p className="upload-leave-dialog__hint">
        다른 회차를 선택하는 것은 가능합니다.
      </p>

      <div className="upload-leave-dialog__actions">
        <button
          type="button"
          className="upload-leave-dialog__stay"
          onClick={stayOnPage}
          autoFocus
        >
          업로드 기다리기
        </button>

        <button
          type="button"
          className="upload-leave-dialog__leave"
          onClick={leavePage}
        >
          그래도 이동
        </button>
      </div>
    </dialog>,
    document.body,
  );
}
