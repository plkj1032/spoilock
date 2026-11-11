import { useEffect, useState } from "react";
import { adminCopyrightApi } from "../../api/adminCopyrightApi";

const FIELDS = [
  ["licenseName", "이용 허락 건 이름"],
  ["rightsHolder", "권리자"],
  ["contactName", "연락 담당자"],
  ["contactEmail", "연락 이메일"],
  ["reviewStatus", "확인 상태"],
  ["videoPermission", "영상 제공"],
  ["subtitlePermission", "자막 사용"],
  ["aiPermission", "AI 분석"],
  ["startsOn", "이용 시작일"],
  ["endsOn", "이용 종료일"],
  ["scopeNotes", "이용 범위 및 제한"],
  ["adminNotes", "관리자 메모"],
];

const STATUS_LABELS = {
  DRAFT: "작성 중",
  REVIEWING: "검토 중",
  CONFIRMED: "확인 완료",
  REVOKED: "철회·해지",
};

const PERMISSION_LABELS = {
  UNCONFIRMED: "미확인",
  ALLOWED: "허용",
  NOT_ALLOWED: "미허용",
};

function displayValue(key, value) {
  if (value == null || value === "") return "미등록";

  if (key === "reviewStatus") {
    return STATUS_LABELS[value] ?? String(value);
  }

  if (
    key === "videoPermission" ||
    key === "subtitlePermission" ||
    key === "aiPermission"
  ) {
    return PERMISSION_LABELS[value] ?? String(value);
  }

  return String(value);
}

function parseSnapshot(value) {
  const parsed = JSON.parse(value);

  if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
    throw new Error("이력 형식이 올바르지 않습니다.");
  }

  return parsed;
}

function HistoryChanges({ item }) {
  let before;
  let after;

  try {
    after = parseSnapshot(item.afterSnapshot);
    before =
      item.action === "CREATED" ? {} : parseSnapshot(item.beforeSnapshot);

    // 지원하는 항목이 누락되면 잘못된 비교 결과를 표시하지 않음
    for (const [key] of FIELDS) {
      if (!Object.hasOwn(after, key)) {
        throw new Error("변경 후 항목이 누락되었습니다.");
      }

      if (item.action !== "CREATED" && !Object.hasOwn(before, key)) {
        throw new Error("변경 전 항목이 누락되었습니다.");
      }
    }
  } catch {
    return (
      <p className="copyright-error" role="alert">
        저장된 변경 내용을 표시할 수 없습니다.
      </p>
    );
  }

  const created = item.action === "CREATED";

  const rows = created
    ? FIELDS
    : FIELDS.filter(([key]) => (before[key] ?? "") !== (after[key] ?? ""));

  if (rows.length === 0) {
    return (
      <p className="copyright-history-help">
        저장은 수행됐지만 이용 권한 입력 항목의 변경은 없습니다.
      </p>
    );
  }

  return (
    <div className="copyright-history-table-wrap">
      <table className="copyright-history-table">
        <caption className="copyright-history-caption">
          {created ? "최초 등록 내용" : "변경된 항목 비교"}
        </caption>

        <thead>
          <tr>
            <th scope="col">항목</th>
            {!created && <th scope="col">변경 전</th>}
            <th scope="col">{created ? "등록 내용" : "변경 후"}</th>
          </tr>
        </thead>

        <tbody>
          {rows.map(([key, label]) => (
            <tr key={key}>
              <th scope="row">{label}</th>

              {!created && <td>{displayValue(key, before[key])}</td>}

              <td>{displayValue(key, after[key])}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default function CopyrightHistoryPanel({ licenseId }) {
  const [query, setQuery] = useState({
    page: 0,
    revision: 0,
  });
  const [result, setResult] = useState(null);

  useEffect(() => {
    let active = true;
    const controller = new AbortController();

    adminCopyrightApi
      .getHistories(licenseId, {
        page: query.page,
        size: 5,
        signal: controller.signal,
      })
      .then((data) => {
        if (active) {
          setResult({ query, data, error: "" });
        }
      })
      .catch((error) => {
        if (!active || error.name === "AbortError") return;

        setResult({
          query,
          data: null,
          error: error.message || "변경 이력을 불러오지 못했습니다.",
        });
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [licenseId, query]);

  const current = result?.query === query ? result : null;
  const data = current?.data;

  function refresh() {
    setQuery((previous) => ({
      page: 0,
      revision: previous.revision + 1,
    }));
  }

  return (
    <section className="copyright-history">
      <div className="copyright-evidence-heading">
        <h3>이용 권한 변경 이력</h3>

        <button type="button" onClick={refresh}>
          새로고침
        </button>
      </div>

      <p className="copyright-history-help">
        이력 기능 적용 이후의 등록·수정 내용을 보여줍니다. 증빙 파일 첨부 내역은
        위 증빙 목록에서 확인할 수 있습니다.
      </p>

      {!current && <p role="status">변경 이력을 불러오는 중...</p>}

      {current?.error && (
        <p className="copyright-error" role="alert">
          {current.error}
        </p>
      )}

      {data && (
        <>
          <p className="copyright-list-summary">총 {data.totalElements}건</p>

          {data.items.length === 0 ? (
            <p className="copyright-empty">기록된 변경 이력이 없습니다.</p>
          ) : (
            <div className="copyright-history-records">
              {data.items.map((item) => (
                <details className="copyright-history-record" key={item.id}>
                  <summary>
                    <strong>
                      {item.action === "CREATED" ? "최초 등록" : "정보 수정"}
                    </strong>

                    <span>{item.actorNickname || "이름 없음"}</span>

                    <span className="copyright-history-time">
                      기록 시각:{" "}
                      {item.createdAt
                        ? item.createdAt.replace("T", " ").split(".")[0]
                        : "미등록"}
                    </span>
                  </summary>

                  <HistoryChanges item={item} />
                </details>
              ))}
            </div>
          )}

          {data.totalPages > 1 && (
            <nav className="copyright-pagination" aria-label="변경 이력 페이지">
              <button
                type="button"
                disabled={data.page === 0}
                onClick={() =>
                  setQuery((previous) => ({
                    ...previous,
                    page: data.page - 1,
                  }))
                }
              >
                이전
              </button>

              <span>
                {data.page + 1} / {data.totalPages}
              </span>

              <button
                type="button"
                disabled={!data.hasNext}
                onClick={() =>
                  setQuery((previous) => ({
                    ...previous,
                    page: data.page + 1,
                  }))
                }
              >
                다음
              </button>
            </nav>
          )}
        </>
      )}
    </section>
  );
}
