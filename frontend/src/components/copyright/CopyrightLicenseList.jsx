import { useEffect, useState } from "react";
import { adminCopyrightApi } from "../../api/adminCopyrightApi";
import CopyrightLicenseForm from "./CopyrightLicenseForm";
import CopyrightEvidencePanel from "./CopyrightEvidencePanel";
import CopyrightHistoryPanel from "./CopyrightHistoryPanel";

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

function permissionLabel(value) {
  return PERMISSION_LABELS[value] ?? "미확인";
}

function periodLabel(item) {
  return `${item.startsOn || "시작일 미등록"} ~ ${
    item.endsOn || "종료일 미등록"
  }`;
}

// =========================================
// 상세 조회
// =========================================
function LicenseDetail({ licenseId, onClose, onEdit }) {
  const [result, setResult] = useState(null);
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    let active = true;
    const controller = new AbortController();

    adminCopyrightApi
      .getLicense(licenseId, { signal: controller.signal })
      .then((data) => {
        if (active) {
          setResult({ data, error: "" });
        }
      })
      .catch((error) => {
        if (!active || error.name === "AbortError") return;

        setResult({
          data: null,
          error: error.message || "상세 정보를 불러오지 못했습니다.",
        });
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [licenseId, revision]);

  const item = result?.data;

  return (
    <section
      className="copyright-detail"
      aria-labelledby="copyright-detail-title"
    >
      <div className="copyright-detail-heading">
        <h3 id="copyright-detail-title">이용 권한 상세</h3>

        <div className="copyright-editor-actions">
          {item && (
            <button type="button" onClick={() => onEdit(item)}>
              수정
            </button>
          )}

          <button type="button" onClick={onClose}>
            닫기
          </button>
        </div>
      </div>

      {!result && <p role="status">상세 정보를 불러오는 중...</p>}

      {result?.error && (
        <div className="copyright-error" role="alert">
          <p>{result.error}</p>

          <button
            type="button"
            onClick={() => {
              setResult(null);
              setRevision((current) => current + 1);
            }}
          >
            다시 불러오기
          </button>
        </div>
      )}

      {item && (
        <dl className="copyright-detail-grid">
          <div>
            <dt>작품</dt>
            <dd>{item.dramaTitle}</dd>
          </div>

          <div>
            <dt>이용 허락 건 이름</dt>
            <dd>{item.licenseName}</dd>
          </div>

          <div>
            <dt>권리자</dt>
            <dd>{item.rightsHolder}</dd>
          </div>

          <div>
            <dt>확인 상태</dt>
            <dd>{STATUS_LABELS[item.reviewStatus] ?? "알 수 없음"}</dd>
          </div>

          <div>
            <dt>연락 담당자</dt>
            <dd>{item.contactName || "미등록"}</dd>
          </div>

          <div>
            <dt>연락 이메일</dt>
            <dd>{item.contactEmail || "미등록"}</dd>
          </div>

          <div>
            <dt>영상 제공</dt>
            <dd>{permissionLabel(item.videoPermission)}</dd>
          </div>

          <div>
            <dt>자막 사용</dt>
            <dd>{permissionLabel(item.subtitlePermission)}</dd>
          </div>

          <div>
            <dt>AI 분석</dt>
            <dd>{permissionLabel(item.aiPermission)}</dd>
          </div>

          <div>
            <dt>이용 기간</dt>
            <dd>{periodLabel(item)}</dd>
          </div>

          <div className="copyright-detail-wide">
            <dt>이용 범위 및 제한</dt>
            <dd>{item.scopeNotes || "미등록"}</dd>
          </div>

          <div className="copyright-detail-wide">
            <dt>관리자 메모</dt>
            <dd>{item.adminNotes || "미등록"}</dd>
          </div>

          <div>
            <dt>등록 담당자</dt>
            <dd>{item.createdByNickname || "이름 없음"}</dd>
          </div>

          <div>
            <dt>마지막 수정 담당자</dt>
            <dd>{item.updatedByNickname || "이름 없음"}</dd>
          </div>
        </dl>
      )}

      {item && <CopyrightEvidencePanel key={item.id} licenseId={item.id} />}
      {item && <CopyrightHistoryPanel key={item.id} licenseId={item.id} />}
    </section>
  );
}

// =========================================
// 목록 + 검색 + 페이지 이동
// =========================================
export default function CopyrightLicenseList() {
  const [keyword, setKeyword] = useState("");
  const [reviewStatus, setReviewStatus] = useState("");

  // 검색 버튼을 누를 때 실제 조회 조건에 반영
  const [query, setQuery] = useState({
    keyword: "",
    reviewStatus: "",
    page: 0,
  });

  const [result, setResult] = useState(null);
  const [selectedId, setSelectedId] = useState(null);
  // null: 목록, { item: null }: 등록, { item }: 수정
  const [editor, setEditor] = useState(null);
  const [notice, setNotice] = useState("");

  useEffect(() => {
    let active = true;
    const controller = new AbortController();

    adminCopyrightApi
      .getLicenses({
        ...query,
        size: 10,
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
          error: error.message || "목록을 불러오지 못했습니다.",
        });
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [query]);

  // 검색 조건이 바뀌면 이전 조건의 결과를 표시하지 않음
  const current = result?.query === query ? result : null;
  const loading = current === null;
  const data = current?.data;
  const items = data?.items ?? [];

  function search(event) {
    event.preventDefault();

    setSelectedId(null);
    setQuery({
      keyword: keyword.trim(),
      reviewStatus,
      page: 0,
    });
  }

  function resetSearch() {
    setKeyword("");
    setReviewStatus("");
    setSelectedId(null);

    setQuery({
      keyword: "",
      reviewStatus: "",
      page: 0,
    });
  }

  function movePage(page) {
    setSelectedId(null);
    setQuery((previous) => ({ ...previous, page }));
  }

  function finishSave(saved) {
    const wasEditing = editor?.item != null;

    setEditor(null);
    setSelectedId(null);
    setKeyword("");
    setReviewStatus("");

    // 등록·수정으로 검색 조건에서 빠지는 경우를 피하도록 전체 목록 갱신
    setQuery({
      keyword: "",
      reviewStatus: "",
      page: 0,
    });

    setNotice(
      `"${saved.licenseName}" 이용 권한을 ${
        wasEditing ? "수정" : "등록"
      }했습니다.`,
    );
  }

  if (editor !== null) {
    return (
      <div className="copyright-license-list">
        <CopyrightLicenseForm
          key={editor.item?.id ?? "new"}
          initialItem={editor.item}
          onCancel={() => setEditor(null)}
          onSaved={finishSave}
        />
      </div>
    );
  }
  return (
    <div className="copyright-license-list">
      <div className="copyright-list-toolbar">
        <button
          type="button"
          onClick={() => {
            setNotice("");
            setEditor({ item: null });
          }}
        >
          + 이용 권한 등록
        </button>
      </div>

      {notice && (
        <p className="copyright-save-notice" role="status">
          {notice}
        </p>
      )}

      <form className="copyright-search" onSubmit={search}>
        <input
          type="search"
          aria-label="이용 권한 검색"
          placeholder="작품명 · 권리자 · 이용 허락 건 이름"
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          maxLength={200}
        />

        <select
          aria-label="확인 상태"
          value={reviewStatus}
          onChange={(event) => setReviewStatus(event.target.value)}
        >
          <option value="">모든 상태</option>

          {Object.entries(STATUS_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>

        <button type="submit">검색</button>

        <button type="button" onClick={resetSearch}>
          초기화
        </button>
      </form>

      {loading && <p role="status">이용 권한 목록을 불러오는 중...</p>}

      {current?.error && (
        <div className="copyright-error" role="alert">
          <p>{current.error}</p>

          <button
            type="button"
            onClick={() => setQuery((previous) => ({ ...previous }))}
          >
            다시 불러오기
          </button>
        </div>
      )}

      {data && (
        <>
          <p className="copyright-list-summary">총 {data.totalElements}건</p>

          {items.length === 0 ? (
            <p className="copyright-empty">
              {query.keyword || query.reviewStatus
                ? "검색 조건에 맞는 이용 권한 기록이 없습니다."
                : "등록된 이용 권한 기록이 없습니다."}
            </p>
          ) : (
            <div className="copyright-records">
              {items.map((item) => (
                <article className="copyright-record" key={item.id}>
                  <div className="copyright-record-heading">
                    <div>
                      <p className="copyright-record-drama">
                        {item.dramaTitle}
                      </p>
                      <h3>{item.licenseName}</h3>
                    </div>

                    <span
                      className="copyright-status"
                      data-status={item.reviewStatus}
                    >
                      {STATUS_LABELS[item.reviewStatus] ?? "알 수 없음"}
                    </span>
                  </div>

                  <p>권리자: {item.rightsHolder}</p>
                  <p>이용 기간: {periodLabel(item)}</p>

                  <div className="copyright-permissions">
                    <span>영상: {permissionLabel(item.videoPermission)}</span>
                    <span>
                      자막: {permissionLabel(item.subtitlePermission)}
                    </span>
                    <span>AI 분석: {permissionLabel(item.aiPermission)}</span>
                  </div>

                  <button
                    type="button"
                    onClick={() =>
                      setSelectedId((currentId) =>
                        currentId === item.id ? null : item.id,
                      )
                    }
                    aria-expanded={selectedId === item.id}
                  >
                    {selectedId === item.id ? "상세 닫기" : "상세 보기"}
                  </button>

                  {selectedId === item.id && (
                    <LicenseDetail
                      key={item.id}
                      licenseId={item.id}
                      onClose={() => setSelectedId(null)}
                      onEdit={(detail) => {
                        setNotice("");
                        setEditor({ item: detail });
                      }}
                    />
                  )}
                </article>
              ))}
            </div>
          )}

          <nav className="copyright-pagination" aria-label="목록 페이지">
            <button
              type="button"
              disabled={data.page === 0}
              onClick={() => movePage(data.page - 1)}
            >
              이전
            </button>

            <span>
              {data.totalPages === 0 ? 0 : data.page + 1}
              {" / "}
              {data.totalPages}
            </span>

            <button
              type="button"
              disabled={!data.hasNext}
              onClick={() => movePage(data.page + 1)}
            >
              다음
            </button>
          </nav>
        </>
      )}
    </div>
  );
}
