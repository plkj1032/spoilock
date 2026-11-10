import "../../styles/Brand.css";

export default function Brand() {
  return (
    <span className="brand brand--playlock">
      <span className="brand-mark" aria-hidden="true">
        <svg
          width="34"
          height="38"
          viewBox="0 0 64 72"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          focusable="false"
        >
          {/* Rounded shackle: no filters or shared SVG IDs. */}
          <path
            d="M19 30V21a13 13 0 0 1 26 0v9"
            stroke="currentColor"
            strokeWidth="7"
            strokeLinecap="round"
          />
          <rect
            x="8"
            y="30"
            width="48"
            height="34"
            rx="9"
            stroke="currentColor"
            strokeWidth="7"
          />
          {/* Video play symbol replaces the keyhole. */}
          <path
            d="M27 40L39 47L27 54Z"
            fill="currentColor"
            stroke="currentColor"
            strokeWidth="2.5"
            strokeLinejoin="round"
          />
        </svg>
      </span>

      <span className="brand-wordmark">
        SPOI<span className="brand-accent">LOCK</span>
      </span>
    </span>
  );
}
