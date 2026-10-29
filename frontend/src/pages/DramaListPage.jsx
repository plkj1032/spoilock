import { useState } from "react";
import DramaGallery from "../components/drama/DramaGallery";
import DramaDetailModal from "../components/drama/DramaDetailModal";

export default function DramaListPage({ user, onLogin, onNotice }) {
  const [detailDramaId, setDetailDramaId] = useState(null);

  return (
    <main className="container main-content">
      <div className="section-top">
        <div>
          <span className="section-overline">EXPLORE</span>
          <h1>드라마 둘러보기</h1>
          <p className="page-subtitle">
            작품 정보를 살펴보고, 시청 진도와 이야기를 이어가세요.
          </p>
        </div>
      </div>

      <DramaGallery
        key={user?.id ?? "guest"}
        user={user}
        onLogin={onLogin}
        onNotice={onNotice}
        onSelectDrama={(drama) => setDetailDramaId(drama.id)}
      />

      {detailDramaId !== null && (
        <DramaDetailModal
          key={detailDramaId}
          dramaId={detailDramaId}
          onClose={() => setDetailDramaId(null)}
        />
      )}
    </main>
  );
}
