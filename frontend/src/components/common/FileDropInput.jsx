import { useId, useRef, useState } from "react";
import "../../styles/FileDropInput.css";

export default function FileDropInput({
  file,
  accept,
  extension,
  maxBytes,
  description,
  disabled = false,
  inputRef,
  onSelect,
  onError,
}) {
  const id = useId();
  const ownInputRef = useRef(null);
  const actualInputRef = inputRef ?? ownInputRef;
  const dragDepthRef = useRef(0);
  const [dragging, setDragging] = useState(false);

  function chooseFiles(files) {
    if (disabled || !files?.length) return;

    if (files.length !== 1) {
      onError("파일은 한 번에 하나씩 첨부해 주세요.");
      return;
    }

    const selected = files[0];

    if (!selected.name.toLowerCase().endsWith(extension.toLowerCase())) {
      onError(`${extension.toUpperCase()} 파일만 첨부할 수 있습니다.`);
      return;
    }

    if (selected.size === 0 || selected.size > maxBytes) {
      const limit = maxBytes / (1024 * 1024);
      onError(`${limit}MB 이하의 비어 있지 않은 파일을 첨부해 주세요.`);
      return;
    }

    onSelect(selected);
  }

  function handleDrop(event) {
    event.preventDefault();
    event.stopPropagation();

    dragDepthRef.current = 0;
    setDragging(false);

    if (disabled) return;

    chooseFiles(event.dataTransfer.files);
  }

  return (
    <div
      className={[
        "file-dropzone",
        dragging && !disabled ? "is-dragging" : "",
        disabled ? "is-disabled" : "",
      ]
        .filter(Boolean)
        .join(" ")}
      onDragEnter={(event) => {
        event.preventDefault();
        event.stopPropagation();

        if (disabled) return;
        if (!Array.from(event.dataTransfer.types).includes("Files")) return;

        dragDepthRef.current += 1;
        setDragging(true);
      }}
      onDragOver={(event) => {
        event.preventDefault();
        event.stopPropagation();
        event.dataTransfer.dropEffect = disabled ? "none" : "copy";
      }}
      onDragLeave={(event) => {
        event.preventDefault();
        event.stopPropagation();

        dragDepthRef.current = Math.max(0, dragDepthRef.current - 1);

        if (dragDepthRef.current === 0) {
          setDragging(false);
        }
      }}
      onDrop={handleDrop}
    >
      <span className="file-dropzone__icon" aria-hidden="true">
        ↑
      </span>

      <strong className="file-dropzone__title">
        {dragging && !disabled
          ? "여기에 파일을 놓아주세요"
          : "파일을 여기로 끌어다 놓으세요"}
      </strong>

      <p id={`${id}-description`} className="file-dropzone__description">
        {description}
      </p>

      <input
        ref={actualInputRef}
        id={id}
        type="file"
        accept={accept}
        disabled={disabled}
        hidden
        onChange={(event) => {
          chooseFiles(event.currentTarget.files);
          event.currentTarget.value = "";
        }}
      />

      <button
        type="button"
        className="file-dropzone__button"
        disabled={disabled}
        aria-describedby={`${id}-description`}
        onClick={() => actualInputRef.current?.click()}
      >
        파일 선택
      </button>

      <span className="file-dropzone__filename" role="status">
        {file
          ? `${file.name} · ${(file.size / (1024 * 1024)).toFixed(2)} MB`
          : "선택된 파일이 없습니다."}
      </span>
    </div>
  );
}
