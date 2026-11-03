import { useState } from "react";
import "../../styles/PosterPreview.css";

function isValidImageUrl(value) {
  try {
    const parsed = new URL(value);
    return ["http:", "https:"].includes(parsed.protocol);
  } catch {
    return false;
  }
}

export default function PosterPreview({ url, title }) {
  const source = url.trim();

  if (!source) return null;

  const valid = isValidImageUrl(source);

  if (!valid) {
    return (
      <p className="poster-preview__warning" role="status">
        http:// 또는 https://로 시작하는 이미지 URL을 입력해 주세요.
      </p>
    );
  }

  return <PosterPreviewImage key={source} source={source} title={title} />;
}

function PosterPreviewImage({ source, title }) {
  const [size, setSize] = useState(null);
  const [failed, setFailed] = useState(false);

  // 2:3 비율에서 약 1% 이내 차이는 허용
  const matchesRatio =
    size !== null && Math.abs(size.width / size.height / (2 / 3) - 1) <= 0.01;

  return (
    <section className="poster-preview" aria-label="포스터 미리보기">
      <div className="poster-preview__card">
        <div className="poster-preview__image">
          {!failed && (
            <img
              src={source}
              alt={`${title.trim() || "작품"} 포스터 미리보기`}
              onLoad={(event) => {
                const image = event.currentTarget;

                setSize({
                  width: image.naturalWidth,
                  height: image.naturalHeight,
                });
              }}
              onError={() => setFailed(true)}
            />
          )}

          {(failed || !size) && (
            <span className="poster-preview__placeholder">
              {failed ? "이미지를 불러올 수 없어요" : "포스터 확인 중…"}
            </span>
          )}
        </div>

        <div className="poster-preview__caption">
          <span>DRAMA</span>
          <strong>{title.trim() || "작품 제목"}</strong>
        </div>
      </div>

      <div className="poster-preview__info" aria-live="polite">
        <strong>실제 카드 표시 미리보기</strong>

        <p>
          포스터는 세로 2:3 비율로 표시됩니다. 권장 크기는 600 × 900 또는 1000 ×
          1500px입니다.
        </p>

        {failed ? (
          <p className="poster-preview__warning">
            이미지 주소가 올바른지 확인해 주세요. 외부 사이트에서 이미지 표시를
            차단한 경우에도 미리보기가 나오지 않을 수 있습니다.
          </p>
        ) : size ? (
          <>
            <p>
              원본 크기: {size.width} × {size.height}px
            </p>

            <p
              className={
                matchesRatio
                  ? "poster-preview__success"
                  : "poster-preview__warning"
              }
            >
              {matchesRatio
                ? "권장 비율에 맞는 포스터입니다."
                : "권장 비율과 달라 이미지 일부가 잘립니다. 왼쪽 미리보기를 확인하거나 세로 포스터로 교체해 주세요."}
            </p>
          </>
        ) : (
          <p>이미지 크기를 확인하고 있습니다.</p>
        )}
      </div>
    </section>
  );
}
