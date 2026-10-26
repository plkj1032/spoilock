import "../../styles/Pagination.css";

export default function Pagination({
  page,
  totalPages,
  onPageChange,
  disabled = false,
  label = "목록 페이지",
}) {
  if (totalPages <= 1) return null;

  const current = Math.min(Math.max(page, 0), totalPages - 1);

  // 현재 페이지 주변 번호를 최대 5개 표시
  const start = Math.max(0, Math.min(current - 2, totalPages - 5));

  const end = Math.min(totalPages, start + 5);

  const pages = Array.from(
    { length: end - start },
    (_, index) => start + index,
  );

  function move(nextPage) {
    if (
      disabled ||
      nextPage === current ||
      nextPage < 0 ||
      nextPage >= totalPages
    ) {
      return;
    }

    onPageChange(nextPage);
  }

  return (
    <nav className="pagination" aria-label={label}>
      <button
        type="button"
        aria-label="첫 페이지"
        disabled={disabled || current === 0}
        onClick={() => move(0)}
      >
        «
      </button>

      <button
        type="button"
        aria-label="이전 페이지"
        disabled={disabled || current === 0}
        onClick={() => move(current - 1)}
      >
        ‹
      </button>

      {pages.map((number) => (
        <button
          key={number}
          type="button"
          aria-label={`${number + 1}페이지`}
          aria-current={number === current ? "page" : undefined}
          disabled={disabled}
          onClick={() => move(number)}
        >
          {number + 1}
        </button>
      ))}

      <button
        type="button"
        aria-label="다음 페이지"
        disabled={disabled || current === totalPages - 1}
        onClick={() => move(current + 1)}
      >
        ›
      </button>

      <button
        type="button"
        aria-label="마지막 페이지"
        disabled={disabled || current === totalPages - 1}
        onClick={() => move(totalPages - 1)}
      >
        »
      </button>
    </nav>
  );
}
