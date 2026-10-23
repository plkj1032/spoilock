import "../../styles/Toast.css";

export default function Toast({ message, onClose }) {
  if (!message) return null;

  return (
    <div className="toast" role="status">
      <span>{message}</span>

      <button type="button" aria-label="알림 닫기" onClick={onClose}>
        ×
      </button>
    </div>
  );
}
