import { useRef } from "react";

// React 19: ref를 props로 받아 실제 dialog/div에 전달
export default function Modal({
  as: Element = "dialog",
  ref,
  children,
  onCancel,
  onDismiss,
  onClick,
  onPointerDown,
  onPointerCancel,
  ...props
}) {
  const startedOutsideRef = useRef(false);

  function isOutside(event) {
    // 내부 요소나 중첩 모달에서 발생한 이벤트 제외
    if (event.target !== event.currentTarget) return false;

    // div는 그 자체가 모달 바깥 배경
    if (Element !== "dialog") return true;

    // dialog 내부 패딩은 바깥 클릭으로 취급하지 않음
    const rect = event.currentTarget.getBoundingClientRect();

    return (
      event.clientX < rect.left ||
      event.clientX > rect.right ||
      event.clientY < rect.top ||
      event.clientY > rect.bottom
    );
  }

  function handlePointerDown(event) {
    startedOutsideRef.current = event.button === 0 && isOutside(event);

    onPointerDown?.(event);
  }

  function handlePointerCancel(event) {
    startedOutsideRef.current = false;
    onPointerCancel?.(event);
  }

  function handleClick(event) {
    // 중첩 모달을 닫을 때 뒤쪽 모달까지 닫히지 않도록 처리
    event.stopPropagation();

    const shouldDismiss = startedOutsideRef.current && isOutside(event);

    startedOutsideRef.current = false;

    if (!shouldDismiss) {
      onClick?.(event);
      return;
    }

    event.preventDefault();

    if (onDismiss) {
      onDismiss(event);
    } else {
      // 기존 Esc 닫기 처리와 저장 중 제한 조건을 그대로 사용
      onCancel?.(event);
    }
  }

  function handleCancel(event) {
    event.preventDefault();
    event.stopPropagation();

    if (onCancel) {
      onCancel(event);
    } else {
      onDismiss?.(event);
    }
  }

  return (
    <Element
      {...props}
      ref={ref}
      onPointerDown={handlePointerDown}
      onPointerCancel={handlePointerCancel}
      onClick={handleClick}
      onCancel={handleCancel}
    >
      {children}
    </Element>
  );
}
