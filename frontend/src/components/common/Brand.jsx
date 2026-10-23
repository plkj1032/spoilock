import "../../styles/Brand.css";

export default function Brand() {
  return (
    <span className="brand">
      <span className="brand-mark" aria-hidden="true">
        <svg
          width="28"
          height="28"
          viewBox="0 0 32 32"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
        >
          <defs>
            {/* 방패 메인 그라데이션 */}
            <linearGradient
              id="shieldGrad"
              x1="16"
              y1="2"
              x2="16"
              y2="30"
              gradientUnits="userSpaceOnUse"
            >
              <stop offset="0%" stopColor="#4A8AFF" />
              <stop offset="100%" stopColor="#1E52DE" />
            </linearGradient>

            {/* 방패 테두리 하이라이트 */}
            <linearGradient
              id="shieldStroke"
              x1="16"
              y1="2"
              x2="16"
              y2="30"
              gradientUnits="userSpaceOnUse"
            >
              <stop offset="0%" stopColor="#82B3FF" />
              <stop offset="100%" stopColor="#255EE9" />
            </linearGradient>

            {/* 방패 은은한 입체 드롭 섀도우 */}
            <filter
              id="shieldGlow"
              x="-20%"
              y="-20%"
              width="140%"
              height="140%"
            >
              <feDropShadow
                dx="0"
                dy="2"
                stdDeviation="2.5"
                floodColor="#255EE9"
                floodOpacity="0.35"
              />
            </filter>
          </defs>

          {/* 1. 외곽 방패 프레임 */}
          <path
            d="M16 2.5L5 6.8C5 17.5 10.5 25.8 16 29.5C21.5 25.8 27 17.5 27 6.8L16 2.5Z"
            fill="url(#shieldGrad)"
            stroke="url(#shieldStroke)"
            strokeWidth="1.2"
            filter="url(#shieldGlow)"
          />

          {/* 2. 내부 말풍선 (화이트) */}
          <path
            d="M9.5 9.5H22.5C23.6 9.5 24.5 10.4 24.5 11.5V17.5C24.5 18.6 23.6 19.5 22.5 19.5H16.8L12.5 22.8V19.5H9.5C8.4 19.5 7.5 18.6 7.5 17.5V11.5C7.5 10.4 8.4 9.5 9.5 9.5Z"
            fill="white"
          />

          {/* 3. 자물쇠 고리 */}
          <path
            d="M13.5 13.5V12.2C13.5 10.8 14.6 9.7 16 9.7C17.4 9.7 18.5 10.8 18.5 12.2V13.5"
            stroke="#2B62EE"
            strokeWidth="1.8"
            strokeLinecap="round"
          />

          {/* 4. 자물쇠 몸체 */}
          <rect x="12" y="13" width="8" height="5.5" rx="1.3" fill="#2B62EE" />

          {/* 5. 열쇠 구멍 */}
          <circle cx="16" cy="15.2" r="0.8" fill="white" />
          <path
            d="M16 15.6V16.8"
            stroke="white"
            strokeWidth="0.9"
            strokeLinecap="round"
          />
        </svg>
      </span>

      <span>
        SPOI<span className="brand-accent">LOCK</span>
      </span>
    </span>
  );
}
