import "../../styles/DefaultRoomCrown.css";

export default function DefaultRoomCrown() {
  return (
    <span className="default-room-crown" role="img" aria-label="기본 토론방">
      <svg
        viewBox="0 0 24 24"
        width="24"
        height="24"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
      >
        <path
          d="m3 6 5 4 4-7 4 7 5-4-2 12H5L3 6Z"
          fill="currentColor"
          fillOpacity=".18"
        />
        <path d="M6 21h12" />
      </svg>
    </span>
  );
}
