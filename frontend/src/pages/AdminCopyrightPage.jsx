import { Link } from "react-router-dom";
import CopyrightLicenseList from "../components/copyright/CopyrightLicenseList";

import "../styles/AdminCenter.css";
import "../styles/AdminCopyrightPage.css";

export default function AdminCopyrightPage() {
  return (
    <main className="container main-content admin-center copyright-page">
      <Link to="/admin" className="admin-center-back">
        ‹ 관리자 센터
      </Link>

      <header className="admin-center-heading">
        <span className="section-overline">SPOILOCK COPYRIGHT</span>
        <h1>저작권 관리센터</h1>
        <p>작품별 이용 권한과 증빙 자료, 변경 이력을 관리합니다.</p>
      </header>

      <section
        className="copyright-panel"
        aria-labelledby="copyright-section-title"
      >
        <header className="copyright-panel-heading">
          <h2 id="copyright-section-title">작품별 이용 권한</h2>
          <p>
            권리자, 영상·자막·AI 분석 이용 허락 범위와 이용 기간을 기록합니다.
          </p>
        </header>

        <CopyrightLicenseList />
      </section>
    </main>
  );
}
