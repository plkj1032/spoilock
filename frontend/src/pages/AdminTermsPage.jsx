import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { termsApi } from "../api/termsApi";
import "../styles/AdminTermsPage.css";

const EMPTY_FORM = {
  termsType: "TERMS",
  version: "1.0",
  title: "",
  content: "",
};

const TYPE_NAMES = {
  TERMS: "서비스 이용약관",
  PRIVACY_NOTICE: "개인정보 처리방침",
  MARKETING: "마케팅 수신 동의",
};

const STATUS_NAMES = {
  DRAFT: "초안",
  PUBLISHED: "게시 중",
  ARCHIVED: "이전 버전",
};

export default function AdminTermsPage() {
  const [documents, setDocuments] = useState([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [editingId, setEditingId] = useState(null);

  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  async function loadDocuments() {
    const data = await termsApi.all();
    setDocuments(Array.isArray(data) ? data : []);
  }

  useEffect(() => {
    let active = true;

    termsApi
      .all()
      .then((data) => {
        if (active) {
          setDocuments(Array.isArray(data) ? data : []);
        }
      })
      .catch((err) => {
        if (active) setError(err.message);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  function updateField(name, value) {
    setForm((current) => ({
      ...current,
      [name]: value,
    }));
  }

  function resetForm() {
    setForm(EMPTY_FORM);
    setEditingId(null);
  }

  function editDocument(document) {
    if (document.status !== "DRAFT") {
      return;
    }

    setEditingId(document.id);

    setForm({
      termsType: document.termsType,
      version: document.version,
      title: document.title,
      content: document.content,
    });

    setError("");
    setMessage("");

    window.scrollTo({
      top: 0,
      behavior: "smooth",
    });
  }

  async function handleSave(event) {
    event.preventDefault();

    if (busy) return;

    setBusy(true);
    setError("");
    setMessage("");

    try {
      if (editingId === null) {
        await termsApi.create({
          termsType: form.termsType,
          version: form.version.trim(),
          title: form.title.trim(),
          content: form.content.trim(),
        });

        setMessage("약관 초안을 등록했습니다.");
      } else {
        await termsApi.update(editingId, {
          title: form.title.trim(),
          content: form.content.trim(),
        });

        setMessage("약관 초안을 수정했습니다.");
      }

      await loadDocuments();
      resetForm();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  async function handlePublish(document) {
    if (busy) return;

    const confirmed = window.confirm(
      `${document.title} (${document.version})을 게시하시겠습니까?\n\n` +
        "기존에 게시 중인 같은 종류의 약관은 이전 버전으로 보관됩니다.",
    );

    if (!confirmed) return;

    setBusy(true);
    setError("");
    setMessage("");

    try {
      await termsApi.publish(document.id);

      await loadDocuments();

      setMessage(
        `${document.title} ${document.version} 버전이 게시되었습니다.`,
      );
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="container main-content admin-terms-page">
      <header className="admin-terms-page__header">
        <Link to="/admin/dramas">← 관리자 작품 관리</Link>

        <h1>약관 관리</h1>

        <p>
          약관을 작성한 뒤 검토하여 게시하세요. 게시된 약관은 직접 수정하지 않고
          새로운 버전으로 개정합니다.
        </p>
      </header>

      {/* 약관 작성/수정 */}
      <section className="admin-terms-card">
        <h2>{editingId === null ? "새 약관 초안 등록" : "약관 초안 수정"}</h2>

        <form onSubmit={handleSave} className="admin-terms-form">
          <label htmlFor="terms-type">약관 종류</label>

          <select
            id="terms-type"
            value={form.termsType}
            disabled={busy || editingId !== null}
            onChange={(event) => updateField("termsType", event.target.value)}
          >
            <option value="TERMS">서비스 이용약관 (필수)</option>

            <option value="PRIVACY_NOTICE">개인정보 처리방침 (고지)</option>

            <option value="MARKETING">마케팅 수신 동의 (선택)</option>
          </select>

          <label htmlFor="terms-version">버전</label>

          <input
            id="terms-version"
            value={form.version}
            maxLength={20}
            required
            disabled={busy || editingId !== null}
            placeholder="예: 1.0"
            onChange={(event) => updateField("version", event.target.value)}
          />

          <label htmlFor="terms-title">약관 제목</label>

          <input
            id="terms-title"
            value={form.title}
            maxLength={200}
            required
            disabled={busy}
            placeholder="예: SPOILOCK 서비스 이용약관"
            onChange={(event) => updateField("title", event.target.value)}
          />

          <label htmlFor="terms-content">약관 본문</label>

          <textarea
            id="terms-content"
            value={form.content}
            required
            maxLength={200000}
            rows={12}
            disabled={busy}
            placeholder="검토된 약관 전문을 입력하세요."
            onChange={(event) => updateField("content", event.target.value)}
          />

          <div className="admin-terms-actions">
            <button type="submit" disabled={busy}>
              {busy
                ? "처리 중..."
                : editingId !== null
                  ? "초안 수정"
                  : "초안 저장"}
            </button>

            {editingId !== null && (
              <button
                type="button"
                className="secondary"
                onClick={resetForm}
                disabled={busy}
              >
                수정 취소
              </button>
            )}
          </div>
        </form>
      </section>

      {/* 상태 안내 */}
      {message && (
        <p className="admin-terms-success" role="status">
          {message}
        </p>
      )}

      {error && (
        <p className="admin-terms-error" role="alert">
          {error}
        </p>
      )}

      {/* 약관 목록 */}
      <section className="admin-terms-card">
        <h2>등록된 약관</h2>

        {loading ? (
          <p>불러오는 중...</p>
        ) : documents.length === 0 ? (
          <p>아직 등록된 약관이 없습니다.</p>
        ) : (
          <div className="admin-terms-list">
            {documents.map((document) => (
              <article key={document.id} className="admin-terms-item">
                <div className="admin-terms-item__top">
                  <div>
                    <strong>{document.title}</strong>

                    <p>
                      {TYPE_NAMES[document.termsType]}
                      {" · "}
                      버전 {document.version}
                    </p>
                  </div>

                  <span className="admin-terms-status">
                    {STATUS_NAMES[document.status]}
                  </span>
                </div>

                <details>
                  <summary>약관 본문 보기</summary>
                  <pre>{document.content}</pre>
                </details>

                {document.status === "DRAFT" && (
                  <div className="admin-terms-actions">
                    <button
                      type="button"
                      className="secondary"
                      disabled={busy}
                      onClick={() => editDocument(document)}
                    >
                      수정
                    </button>

                    <button
                      type="button"
                      disabled={busy}
                      onClick={() => handlePublish(document)}
                    >
                      게시
                    </button>
                  </div>
                )}
              </article>
            ))}
          </div>
        )}
      </section>
    </main>
  );
}
