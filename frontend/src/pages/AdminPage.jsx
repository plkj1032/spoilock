import { Link } from "react-router-dom";
import "../styles/AdminCenter.css";

const CENTERS = [
  {
    number: "01",
    title: "콘텐츠 관리센터",
    description: "작품 등록부터 회차, 자막 및 AI 분석까지 관리합니다.",
    to: "/admin/dramas",
    label: "콘텐츠 관리",
  },
  {
    number: "02",
    title: "채팅방 관리센터",
    description: "드라마별 기본 토론방과 참여자를 관리합니다.",
    to: "/admin/chats",
    label: "토론방 관리",
  },
  {
    number: "03",
    title: "약관 관리센터",
    description: "서비스 약관을 작성하고 게시 버전을 관리합니다.",
    to: "/admin/terms",
    label: "약관 관리",
  },
];

export default function AdminPage() {
  return (
    <main className="container main-content admin-center">
      <header className="admin-center-heading">
        <span className="section-overline">SPOILOCK ADMIN</span>
        <h1>관리자 센터</h1>
        <p>관리할 서비스를 선택해 주세요.</p>
      </header>

      <div className="admin-center-grid">
        {CENTERS.map((center) => (
          <Link key={center.to} to={center.to} className="admin-center-card">
            <span className="admin-center-number">{center.number}</span>
            <h2>{center.title}</h2>
            <p>{center.description}</p>
            <span className="admin-center-card-link">
              {center.label}
              <span aria-hidden="true">↗</span>
            </span>
          </Link>
        ))}
      </div>
    </main>
  );
}
