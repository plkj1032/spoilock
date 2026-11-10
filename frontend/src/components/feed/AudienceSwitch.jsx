import { useId } from "react";
import "../../styles/AudienceSwitch.css";

/**
 * 게시글 공개 범위를 선택하는 세그먼트 스위치.
 * PUBLIC: 전체 공개 / FOLLOWERS: 작성자의 팔로워에게만 공개
 */
export default function AudienceSwitch({
  value = "PUBLIC",
  onChange,
  disabled = false,
}) {
  const groupName = useId();

  return (
    <fieldset className="feed-audience" disabled={disabled}>
      <div className="feed-audience__options">
        <label
          className={`feed-audience__option ${
            value === "PUBLIC" ? "is-selected" : ""
          }`}
        >
          <input
            type="radio"
            name={groupName}
            value="PUBLIC"
            checked={value === "PUBLIC"}
            onChange={() => onChange("PUBLIC")}
          />
          <span aria-hidden="true">🌐</span>
          전체
        </label>

        <label
          className={`feed-audience__option ${
            value === "FOLLOWERS" ? "is-selected" : ""
          }`}
        >
          <input
            type="radio"
            name={groupName}
            value="FOLLOWERS"
            checked={value === "FOLLOWERS"}
            onChange={() => onChange("FOLLOWERS")}
          />
          <span aria-hidden="true">👥</span>
          팔로워
        </label>
      </div>
    </fieldset>
  );
}
