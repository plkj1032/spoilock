import { useEffect, useRef, useState } from "react";
import { adminCopyrightEvidenceApi } from "../../api/adminCopyrightEvidenceApi";

const MAX_FILE_SIZE = 10 * 1024 * 1024;

function formatSize(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }

  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

export default function CopyrightEvidencePanel({ licenseId }) {
  const [file, setFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [downloadingId, setDownloadingId] = useState(null);

  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const [query, setQuery] = useState({
    page: 0,
    revision: 0,
  });
  const [result, setResult] = useState(null);

  const inputRef = useRef(null);
  const uploadLock = useRef(false);
  const downloadLock = useRef(false);
  const mounted = useRef(false);
  const downloadController = useRef(null);

  useEffect(() => {
    mounted.current = true;

    return () => {
      mounted.current = false;
      downloadController.current?.abort();
    };
  }, []);

  useEffect(() => {
    let active = true;
    const controller = new AbortController();

    adminCopyrightEvidenceApi
      .getEvidences(licenseId, {
        page: query.page,
        size: 10,
        signal: controller.signal,
      })
      .then((data) => {
        if (active) {
          setResult({ query, data, error: "" });
        }
      })
      .catch((err) => {
        if (!active || err.name === "AbortError") return;

        setResult({
          query,
          data: null,
          error: err.message || "증빙 목록을 불러오지 못했습니다.",
        });
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [licenseId, query]);

  const current = result?.query === query ? result : null;
  const data = current?.data;

  function refreshList() {
    setQuery((previous) => ({
      ...previous,
      revision: previous.revision + 1,
    }));
  }

  function selectFile(event) {
    const selected = event.target.files?.[0] ?? null;

    setFile(null);
    setError("");
    setNotice("");

    if (!selected) return;

    if (!/\.(pdf|jpe?g|png)$/i.test(selected.name)) {
      setError("PDF, JPG, PNG 파일만 첨부할 수 있습니다.");
      event.target.value = "";
      return;
    }

    if (selected.size === 0 || selected.size > MAX_FILE_SIZE) {
      setError("빈 파일은 첨부할 수 없으며, 최대 크기는 10MB입니다.");
      event.target.value = "";
      return;
    }

    setFile(selected);
  }

  async function upload(event) {
    event.preventDefault();

    if (!file || uploadLock.current) return;

    uploadLock.current = true;
    setUploading(true);
    setError("");
    setNotice("");

    try {
      await adminCopyrightEvidenceApi.upload(licenseId, file);

      if (!mounted.current) return;

      setFile(null);

      if (inputRef.current) {
        inputRef.current.value = "";
      }

      setNotice("증빙 파일을 첨부했습니다.");
      setQuery((previous) => ({
        page: 0,
        revision: previous.revision + 1,
      }));
    } catch (err) {
      if (!mounted.current) return;

      setError(
        `${err.message || "첨부하지 못했습니다."} ` +
          "응답이 끊긴 경우에는 목록을 새로고침해 등록 여부를 확인해 주세요.",
      );
    } finally {
      uploadLock.current = false;

      if (mounted.current) {
        setUploading(false);
      }
    }
  }

  async function download(item) {
    if (downloadLock.current) return;

    downloadLock.current = true;
    setDownloadingId(item.id);
    setError("");

    const controller = new AbortController();
    downloadController.current = controller;

    try {
      const blob = await adminCopyrightEvidenceApi.download(
        licenseId,
        item.id,
        { signal: controller.signal },
      );

      if (!mounted.current) return;

      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");

      anchor.href = url;
      anchor.download = item.originalFilename;
      document.body.appendChild(anchor);

      try {
        anchor.click();
      } finally {
        anchor.remove();

        // 브라우저가 다운로드를 시작한 뒤 임시 URL 정리
        window.setTimeout(() => URL.revokeObjectURL(url), 10000);
      }
    } catch (err) {
      if (!mounted.current || err.name === "AbortError") return;

      setError(err.message || "파일을 다운로드하지 못했습니다.");
    } finally {
      downloadLock.current = false;
      downloadController.current = null;

      if (mounted.current) {
        setDownloadingId(null);
      }
    }
  }

  return (
    <section className="copyright-evidence">
      <div className="copyright-evidence-heading">
        <h3>증빙 파일</h3>

        <button type="button" onClick={refreshList} disabled={uploading}>
          목록 새로고침
        </button>
      </div>

      <p className="copyright-evidence-help">
        이용 허락서·계약서 등의 증빙을 첨부하세요. PDF·JPG·PNG / 파일당 최대
        10MB
      </p>

      <form className="copyright-evidence-upload" onSubmit={upload}>
        <label>
          <span>첨부할 파일</span>

          <input
            ref={inputRef}
            type="file"
            accept=".pdf,.jpg,.jpeg,.png"
            onChange={selectFile}
            disabled={uploading}
          />
        </label>

        {file && (
          <p className="copyright-evidence-selected">
            {file.name} · {formatSize(file.size)}
          </p>
        )}

        <button type="submit" disabled={!file || uploading}>
          {uploading ? "첨부 중..." : "증빙 첨부"}
        </button>
      </form>

      {error && (
        <p className="copyright-error" role="alert">
          {error}
        </p>
      )}

      {notice && (
        <p className="copyright-save-notice" role="status">
          {notice}
        </p>
      )}

      {!current && <p role="status">증빙 목록을 불러오는 중...</p>}

      {current?.error && (
        <p className="copyright-error" role="alert">
          {current.error}
        </p>
      )}

      {data && (
        <>
          <p className="copyright-list-summary">
            첨부된 증빙 {data.totalElements}개
          </p>

          {data.items.length === 0 ? (
            <p className="copyright-empty">첨부된 증빙 파일이 없습니다.</p>
          ) : (
            <ul className="copyright-evidence-list">
              {data.items.map((item) => (
                <li key={item.id} className="copyright-evidence-item">
                  <div className="copyright-evidence-info">
                    <strong>{item.originalFilename}</strong>

                    <span>
                      {formatSize(item.sizeBytes)}
                      {" · "}
                      첨부자: {item.uploadedByNickname || "이름 없음"}
                    </span>
                  </div>

                  <button
                    type="button"
                    onClick={() => download(item)}
                    disabled={downloadingId !== null}
                    aria-label={`${item.originalFilename} 다운로드`}
                  >
                    {downloadingId === item.id ? "다운로드 중..." : "다운로드"}
                  </button>
                </li>
              ))}
            </ul>
          )}

          {data.totalPages > 1 && (
            <nav className="copyright-pagination" aria-label="증빙 목록 페이지">
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
