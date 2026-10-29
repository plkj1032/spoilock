import { useEffect, useId, useRef } from "react";
import { createPortal } from "react-dom";

import { FeedComposer } from "../../pages/CommunityPage";

import "../../styles/HomeFeedComposeModal.css";

export default function HomeFeedComposeModal({
  user,
  drama,
  onLogin,
  onNotice,
  onClose,
  onCreated,
}) {
  const dialogRef = useRef(null);
  const titleId = useId();

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
      className="home-feed-compose-dialog"
      aria-labelledby={titleId}
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
    >
      <header className="home-feed-compose-header">
        <div>
          <span>NEW STORY</span>
          <h2 id={titleId}>글 작성하기</h2>

          {drama && <p>{drama.title}</p>}
        </div>

        <button
          type="button"
          aria-label="글 작성창 닫기"
          onClick={onClose}
          autoFocus
        >
          ×
        </button>
      </header>

      <div className="home-feed-compose-body">
        <FeedComposer
          user={user}
          onLogin={onLogin}
          onNotice={onNotice}
          initialDramaId={drama?.id ?? null}
          onCreated={onCreated}
        />
      </div>
    </dialog>,
    document.body,
  );
}
