import { useId, useState } from "react";

export default function StarRatingPicker({
  value,
  onChange,
  disabled = false,
}) {
  const groupName = useId();
  const [hovered, setHovered] = useState(null);

  const preview = disabled ? value : (hovered ?? value);

  function choose(score) {
    if (disabled) return;

    setHovered(null);
    onChange(score);
  }

  return (
    <fieldset
      className="rating-picker"
      disabled={disabled}
      onPointerLeave={() => setHovered(null)}
    >
      <legend>별을 눌러 평가해 주세요</legend>

      <div className="rating-picker__row">
        <div className="rating-picker__stars">
          {[0, 1, 2, 3, 4].map((index) => {
            const filled = Math.max(0, Math.min(2, (preview ?? 0) - index * 2));

            return (
              <div className="rating-picker__star" key={index}>
                <svg
                  viewBox="0 0 24 24"
                  aria-hidden="true"
                  className="rating-picker__base"
                >
                  <path d="M12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26Z" />
                </svg>

                <svg
                  viewBox="0 0 24 24"
                  aria-hidden="true"
                  className="rating-picker__fill"
                  style={{
                    clipPath: `inset(0 ${100 - filled * 50}% 0 0)`,
                  }}
                >
                  <path d="M12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26Z" />
                </svg>

                {[1, 2].map((half) => {
                  const score = index * 2 + half;

                  return (
                    <label
                      key={half}
                      className={
                        half === 1
                          ? "rating-picker__half is-left"
                          : "rating-picker__half is-right"
                      }
                      onPointerEnter={(event) => {
                        if (!disabled && event.pointerType === "mouse") {
                          setHovered(score);
                        }
                      }}
                    >
                      <input
                        type="radio"
                        name={groupName}
                        value={score}
                        aria-label={`${(score / 2).toFixed(1)}점`}
                        checked={value === score}
                        onChange={() => choose(score)}
                        onFocus={() => setHovered(null)}
                      />
                      <span className="rating-picker__focus" />
                    </label>
                  );
                })}
              </div>
            );
          })}
        </div>

        <span className="rating-picker__value">
          {preview == null ? "미선택" : `${(preview / 2).toFixed(1)}점`}
        </span>

        <label
          className="rating-picker__zero"
          onPointerEnter={() => setHovered(null)}
        >
          <input
            type="radio"
            name={groupName}
            value={0}
            checked={value === 0}
            onChange={() => choose(0)}
            onFocus={() => setHovered(null)}
          />
          <span>0점</span>
        </label>
      </div>

      <p className="rating-picker__hint">
        반 칸씩 선택할 수 있어요. 선택 후 저장해 주세요.
      </p>
    </fieldset>
  );
}
