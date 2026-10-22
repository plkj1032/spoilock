import DramaCard from "../components/drama/DramaCard";
import { demoDramas } from "../data/demoData";

export default function DramaListPage({ onNotice }) {
  return (
    <main className="container main-content">
      <div className="section-top">
        <div>
          <span className="section-overline">EXPLORE</span>
          <h1>드라마 둘러보기</h1>
          <p className="page-subtitle">
            저작권 또는 이용 허락을 확인한 작품을 등록할 예정입니다.
          </p>
        </div>
      </div>
      <div className="drama-grid all-dramas">
        {demoDramas.map((d) => (
          <DramaCard
            key={d.id}
            drama={d}
            onAction={() =>
              onNotice("백엔드 드라마 API 연결 후 이용할 수 있습니다.")
            }
          />
        ))}
      </div>
    </main>
  );
}
