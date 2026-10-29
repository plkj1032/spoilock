import { useEffect, useState } from "react";
import { dramaApi } from "../../api/dramaApi";
import "../../styles/MyRoomFilters.css";

const ROOM_TYPES = [
  { value: "ALL", label: "전체" },
  { value: "DEFAULT", label: "토론방" },
  { value: "CUSTOM", label: "자유 채팅방" },
  { value: "PRIVATE", label: "비공개방" },
];

export default function MyRoomFilters({
  dramaId,
  roomType,
  onDramaChange,
  onTypeChange,
}) {
  const [dramas, setDramas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    let active = true;

    dramaApi
      .getDramas()
      .then((data) => {
        if (!active) return;

        setDramas(
          [...(data ?? [])].sort((a, b) =>
            a.title.localeCompare(b.title, "ko"),
          ),
        );
        setError("");
      })
      .catch(() => {
        if (active) {
          setError("드라마 목록을 불러오지 못했습니다.");
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [revision]);

  return (
    <div className="my-room-filters">
      <div className="my-room-filters-row">
        <select
          aria-label="참여 채팅방 드라마 선택"
          value={dramaId}
          disabled={loading}
          onChange={(event) => onDramaChange(event.target.value)}
        >
          <option value="">
            {loading ? "드라마 불러오는 중…" : "전체 드라마"}
          </option>

          {dramas.map((drama) => (
            <option key={drama.id} value={String(drama.id)}>
              {drama.title}
            </option>
          ))}
        </select>

        <div
          className="my-room-type-buttons"
          role="group"
          aria-label="참여 채팅방 종류"
        >
          {ROOM_TYPES.map((type) => (
            <button
              key={type.value}
              type="button"
              aria-pressed={roomType === type.value}
              onClick={() => onTypeChange(type.value)}
            >
              {type.label}
            </button>
          ))}
        </div>
      </div>

      {error && (
        <div className="my-room-filter-error" role="alert">
          <span>{error}</span>
          <button
            type="button"
            disabled={loading}
            onClick={() => {
              setLoading(true);
              setError("");
              setRevision((current) => current + 1);
            }}
          >
            다시 불러오기
          </button>
        </div>
      )}
    </div>
  );
}
