import DramaGallery from "../components/drama/DramaGallery";

export default function DramaListPage({ user, onLogin, onNotice }) {
  return (
    <main className="container main-content">
      <div className="section-top">
        <div>
          <span className="section-overline">EXPLORE</span>
          <h1>드라마 둘러보기</h1>
          <p className="page-subtitle">
            드라마를 선택하여 시청 진도를 관리하세요.
          </p>
        </div>
      </div>

      <DramaGallery
        key={user?.id ?? "guest"}
        user={user}
        onLogin={onLogin}
        onNotice={onNotice}
      />
    </main>
  );
}
