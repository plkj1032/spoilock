import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { useNavigate } from "react-router-dom";

import { dramaApi } from "../../api/dramaApi";
import { chatApi } from "../../api/chatApi";
import "../../styles/MyDramaActions.css";

function ProgressDialog({ drama, onClose, onSaved }) {
  const dialogRef = useRef(null);
  const activeRef = useRef(false);
  const savingRef = useRef(false);

  const [episodes, setEpisodes] = useState([]);
  const [current, setCurrent] = useState(0);
  const [selected, setSelected] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loaded, setLoaded] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const dialog = dialogRef.current;
    const previousFocus = document.activeElement;

    dialog.showModal();
    activeRef.current = true;

    return () => {
      activeRef.current = false;
      dialog.close();

      if (previousFocus instanceof HTMLElement && previousFocus.isConnected) {
        previousFocus.focus();
      }
    };
  }, []);

  useEffect(() => {
    let active = true;

    async function load() {
      try {
        const [episodeData, progressData] = await Promise.all([
          dramaApi.getEpisodes(drama.dramaId),
          dramaApi.getProgress(drama.dramaId),
        ]);

        if (!active) return;

        const watched = Number(progressData.watchedEpisodeNumber ?? 0);

        setEpisodes(episodeData ?? []);
        setCurrent(watched);
        setSelected(watched);
        setLoaded(true);
      } catch (err) {
        if (active) setError(err.message);
      } finally {
        if (active) setLoading(false);
      }
    }

    void load();

    return () => {
      active = false;
    };
  }, [drama.dramaId]);

  const validSelection =
    selected === 0 ||
    episodes.some((episode) => Number(episode.episodeNumber) === selected);

  async function save() {
    if (savingRef.current || !loaded || !validSelection) return;

    if (
      selected > current &&
      !(current === 0 && selected === 1) &&
      !window.confirm(
        `${current}화에서 ${selected}화로 변경하시겠습니까?\n\n` +
          `${selected}화까지의 스포일러가 표시될 수 있습니다.`,
      )
    ) {
      return;
    }

    savingRef.current = true;
    setSaving(true);
    setError("");

    try {
      const result = await dramaApi.updateProgress(drama.dramaId, selected);

      if (!activeRef.current) return;

      onSaved(drama.dramaId, result.watchedEpisodeNumber);
      onClose();
    } catch (err) {
      if (activeRef.current) setError(err.message);
    } finally {
      savingRef.current = false;

      if (activeRef.current) setSaving(false);
    }
  }

  return createPortal(
    <dialog
      ref={dialogRef}
      className="my-drama-dialog"
      aria-labelledby="my-drama-dialog-title"
      onCancel={(event) => {
        event.preventDefault();
        if (!savingRef.current) onClose();
      }}
    >
      <div className="my-drama-dialog-heading">
        <h2 id="my-drama-dialog-title">시청 진도 설정</h2>
        <button
          type="button"
          className="my-drama-close"
          aria-label="닫기"
          disabled={saving}
          onClick={onClose}
        >
          ×
        </button>
      </div>

      <p className="my-drama-dialog-title">{drama.title}</p>

      {loading ? (
        <p role="status">시청 정보를 불러오는 중입니다.</p>
      ) : loaded ? (
        <>
          <p className="my-drama-hint">현재 {current}화까지 시청했어요.</p>

          <div
            className="my-drama-episodes"
            role="group"
            aria-label="시청 회차"
          >
            <button
              type="button"
              aria-pressed={selected === 0}
              disabled={saving}
              onClick={() => setSelected(0)}
            >
              미시청
            </button>

            {episodes.map((episode) => {
              const number = Number(episode.episodeNumber);

              return (
                <button
                  type="button"
                  key={episode.id}
                  aria-pressed={selected === number}
                  disabled={saving}
                  onClick={() => setSelected(number)}
                >
                  {number}화
                </button>
              );
            })}
          </div>

          <p className="my-drama-hint">
            실제로 시청한 마지막 회차를 선택해 주세요. 진도를 올리면 해당 회차의
            스포일러가 표시될 수 있어요.
          </p>
        </>
      ) : null}

      {error && (
        <p className="my-drama-error" role="alert">
          {error}
        </p>
      )}

      <div className="my-drama-dialog-footer">
        <button type="button" disabled={saving} onClick={onClose}>
          취소
        </button>
        <button
          type="button"
          className="my-drama-save"
          disabled={loading || !loaded || !validSelection || saving}
          onClick={save}
        >
          {saving ? "저장 중…" : "진도 저장"}
        </button>
      </div>
    </dialog>,
    document.body,
  );
}

export default function MyDramaActions({ drama, onProgressSaved }) {
  const navigate = useNavigate();
  const activeRef = useRef(false);
  const enteringRef = useRef(false);

  const [progressOpen, setProgressOpen] = useState(false);
  const [entering, setEntering] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    activeRef.current = true;

    return () => {
      activeRef.current = false;
    };
  }, []);

  async function enterDiscussion() {
    if (enteringRef.current) return;

    enteringRef.current = true;
    setEntering(true);
    setError("");

    try {
      const rooms = await chatApi.getRooms();

      if (!activeRef.current) return;

      const room = rooms.find(
        (item) =>
          item.roomType === "DEFAULT" &&
          String(item.dramaId) === String(drama.dramaId),
      );

      if (!room) {
        setError("아직 이용할 수 있는 기본 토론방이 없습니다.");
        return;
      }

      await chatApi.enterRoom(room.roomId);

      if (activeRef.current) {
        navigate(`/chat/${room.roomId}`);
      }
    } catch (err) {
      if (activeRef.current) setError(err.message);
    } finally {
      enteringRef.current = false;

      if (activeRef.current) setEntering(false);
    }
  }

  return (
    <>
      <div className="my-drama-actions">
        <button
          type="button"
          disabled={entering}
          onClick={() => {
            setError("");
            setProgressOpen(true);
          }}
        >
          시청 진도 설정
        </button>

        <button type="button" disabled={entering} onClick={enterDiscussion}>
          {entering ? "입장 중…" : "토론방 이동 →"}
        </button>
      </div>

      {error && (
        <div className="my-drama-error" role="alert">
          {error}
        </div>
      )}

      {progressOpen && (
        <ProgressDialog
          drama={drama}
          onClose={() => setProgressOpen(false)}
          onSaved={onProgressSaved}
        />
      )}
    </>
  );
}
