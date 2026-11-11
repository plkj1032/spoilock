import { useEffect, useRef, useState } from "react";
import { adminCopyrightApi } from "../../api/adminCopyrightApi";
import { adminDramaApi } from "../../api/adminDramaApi";

const STATUS_OPTIONS = [
  ["DRAFT", "작성 중"],
  ["REVIEWING", "검토 중"],
  ["CONFIRMED", "확인 완료"],
  ["REVOKED", "철회·해지"],
];

const PERMISSION_OPTIONS = [
  ["UNCONFIRMED", "미확인"],
  ["ALLOWED", "허용"],
  ["NOT_ALLOWED", "미허용"],
];

function initialValues(item) {
  return {
    dramaId: item?.dramaId ?? "",
    licenseName: item?.licenseName ?? "",
    rightsHolder: item?.rightsHolder ?? "",
    contactName: item?.contactName ?? "",
    contactEmail: item?.contactEmail ?? "",
    reviewStatus: item?.reviewStatus ?? "DRAFT",
    videoPermission: item?.videoPermission ?? "UNCONFIRMED",
    subtitlePermission: item?.subtitlePermission ?? "UNCONFIRMED",
    aiPermission: item?.aiPermission ?? "UNCONFIRMED",
    startsOn: item?.startsOn ?? "",
    endsOn: item?.endsOn ?? "",
    scopeNotes: item?.scopeNotes ?? "",
    adminNotes: item?.adminNotes ?? "",
    version: item?.version,
  };
}

export default function CopyrightLicenseForm({
  initialItem = null,
  onCancel,
  onSaved,
}) {
  const editing = initialItem != null;

  const [form, setForm] = useState(() => initialValues(initialItem));
  const [dramas, setDramas] = useState(null);
  const [dramaError, setDramaError] = useState("");
  const [loadRevision, setLoadRevision] = useState(0);

  const [busy, setBusy] = useState(false);
  const [dirty, setDirty] = useState(false);
  const [error, setError] = useState("");
  const [conflict, setConflict] = useState(false);

  // 연속 클릭에 의한 중복 요청 방지
  const requestLock = useRef(false);

  useEffect(() => {
    if (editing) return;

    let active = true;

    adminDramaApi
      .getAll()
      .then((data) => {
        if (!active) return;

        setDramas(data.filter((drama) => drama.status !== "DELETED"));
        setDramaError("");
      })
      .catch((err) => {
        if (!active) return;

        setDramaError(err.message || "작품 목록을 불러오지 못했습니다.");
      });

    return () => {
      active = false;
    };
  }, [editing, loadRevision]);

  // 입력 중 새로고침하거나 페이지를 닫는 경우 안내
  useEffect(() => {
    if (!dirty && !busy) return;

    function preventUnload(event) {
      event.preventDefault();
      event.returnValue = "";
    }

    window.addEventListener("beforeunload", preventUnload);
    return () => window.removeEventListener("beforeunload", preventUnload);
  }, [dirty, busy]);

  function change(event) {
    const { name, value } = event.target;

    setForm((previous) => ({ ...previous, [name]: value }));
    setDirty(true);
  }

  function cancel() {
    if (requestLock.current) return;

    if (dirty && !window.confirm("입력한 내용을 버리고 닫을까요?")) {
      return;
    }

    onCancel();
  }

  async function submit(event) {
    event.preventDefault();

    if (requestLock.current || conflict) return;

    if (!form.licenseName.trim() || !form.rightsHolder.trim()) {
      setError("이용 허락 건 이름과 권리자를 입력해 주세요.");
      return;
    }

    if (!editing && !form.dramaId) {
      setError("작품을 선택해 주세요.");
      return;
    }

    if (form.startsOn && form.endsOn && form.endsOn < form.startsOn) {
      setError("종료일은 시작일보다 빠를 수 없습니다.");
      return;
    }

    requestLock.current = true;
    setBusy(true);
    setError("");

    let saved;

    try {
      saved = editing
        ? await adminCopyrightApi.updateLicense(initialItem.id, form)
        : await adminCopyrightApi.createLicense(form);
    } catch (err) {
      setError(err.message || "저장하지 못했습니다.");

      if (err.status === 409) {
        setConflict(true);
      }

      return;
    } finally {
      requestLock.current = false;
      setBusy(false);
    }

    setDirty(false);
    onSaved(saved);
  }

  async function reloadLatest() {
    if (requestLock.current) return;

    const approved = window.confirm(
      "현재 입력한 내용을 버리고 서버의 최신 내용으로 다시 불러올까요?",
    );

    if (!approved) return;

    requestLock.current = true;
    setBusy(true);

    try {
      const latest = await adminCopyrightApi.getLicense(initialItem.id);

      setForm(initialValues(latest));
      setDirty(false);
      setConflict(false);
      setError("");
    } catch (err) {
      setError(err.message || "최신 내용을 불러오지 못했습니다.");
    } finally {
      requestLock.current = false;
      setBusy(false);
    }
  }

  const textFields = [
    ["licenseName", "이용 허락 건 이름", 200, true],
    ["rightsHolder", "권리자", 200, true],
    ["contactName", "연락 담당자", 100, false],
    ["contactEmail", "연락 이메일", 255, false],
  ];

  const permissionFields = [
    ["videoPermission", "영상 제공"],
    ["subtitlePermission", "자막 사용"],
    ["aiPermission", "AI 분석"],
  ];

  return (
    <form className="copyright-editor" onSubmit={submit}>
      <h3>{editing ? "이용 권한 수정" : "이용 권한 등록"}</h3>

      <fieldset disabled={busy}>
        <div className="copyright-editor-grid">
          <label className="copyright-editor-wide">
            <span>작품 *</span>

            {editing ? (
              <input value={initialItem.dramaTitle} readOnly />
            ) : (
              <select
                name="dramaId"
                value={form.dramaId}
                onChange={change}
                required
                disabled={!dramas}
              >
                <option value="">
                  {!dramas ? "작품을 불러오는 중..." : "작품 선택"}
                </option>

                {(dramas ?? []).map((drama) => (
                  <option key={drama.id} value={drama.id}>
                    {drama.title}
                  </option>
                ))}
              </select>
            )}
          </label>

          {textFields.map(([name, label, maxLength, required]) => (
            <label key={name}>
              <span>
                {label}
                {required ? " *" : ""}
              </span>

              <input
                name={name}
                type={name === "contactEmail" ? "email" : "text"}
                value={form[name]}
                onChange={change}
                maxLength={maxLength}
                required={required}
              />
            </label>
          ))}

          <label>
            <span>확인 상태</span>
            <select
              name="reviewStatus"
              value={form.reviewStatus}
              onChange={change}
            >
              {STATUS_OPTIONS.map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>

          {permissionFields.map(([name, label]) => (
            <label key={name}>
              <span>{label}</span>
              <select name={name} value={form[name]} onChange={change}>
                {PERMISSION_OPTIONS.map(([value, text]) => (
                  <option key={value} value={value}>
                    {text}
                  </option>
                ))}
              </select>
            </label>
          ))}

          <label>
            <span>이용 시작일</span>
            <input
              type="date"
              name="startsOn"
              value={form.startsOn}
              onChange={change}
              max={form.endsOn || undefined}
            />
          </label>

          <label>
            <span>이용 종료일</span>
            <input
              type="date"
              name="endsOn"
              value={form.endsOn}
              onChange={change}
              min={form.startsOn || undefined}
            />
          </label>

          <label className="copyright-editor-wide">
            <span>이용 범위 및 제한</span>
            <textarea
              name="scopeNotes"
              value={form.scopeNotes}
              onChange={change}
              rows={3}
              maxLength={5000}
              placeholder="허락받은 사용 범위와 제한 조건을 입력하세요."
            />
          </label>

          <label className="copyright-editor-wide">
            <span>관리자 메모</span>
            <textarea
              name="adminNotes"
              value={form.adminNotes}
              onChange={change}
              rows={3}
              maxLength={5000}
            />
          </label>
        </div>

        <p className="copyright-editor-help">
          날짜를 비워두면 기간 미확인으로 기록됩니다. 영상·자막·AI 분석 허용
          여부는 각각 선택해 주세요.
        </p>

        {!editing && dramaError && (
          <div className="copyright-error" role="alert">
            <p>{dramaError}</p>
            <button
              type="button"
              onClick={() => {
                setDramaError("");
                setLoadRevision((value) => value + 1);
              }}
            >
              작품 다시 불러오기
            </button>
          </div>
        )}

        {!editing && dramas?.length === 0 && (
          <p role="status">등록할 수 있는 작품이 없습니다.</p>
        )}

        {error && (
          <div className="copyright-error" role="alert">
            <p>{error}</p>

            {conflict && (
              <button type="button" onClick={reloadLatest}>
                최신 내용으로 다시 불러오기
              </button>
            )}
          </div>
        )}

        <div className="copyright-editor-actions">
          <button type="button" onClick={cancel}>
            취소
          </button>

          <button
            type="submit"
            disabled={
              conflict || (!editing && (!dramas?.length || Boolean(dramaError)))
            }
          >
            {busy ? "처리 중..." : editing ? "수정 저장" : "등록"}
          </button>
        </div>
      </fieldset>
    </form>
  );
}
