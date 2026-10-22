export default function CommunityPage({ user, onLogin, onNotice }) {
  const action = () =>
    user
      ? onNotice("게시글 API 연결 뒤 작성 기능을 사용할 수 있습니다.")
      : onLogin();
  return (
    <main className="container main-content community-page">
      <span className="section-overline">COMMUNITY</span>
      <h1>모두의 이야기</h1>
      <button type="button" className="new-post" onClick={action}>
        <span className="avatar">S</span>
        <span>드라마에 대한 생각을 나눠보세요.</span>
        <strong>게시하기</strong>
      </button>
      <article className="sample-post">
        <div className="post-author">
          <span className="avatar">D</span>
          <strong>데모 사용자</strong>
          <span className="demo-label">화면 미리보기</span>
        </div>
        <p>스포일러 걱정 없이 작품 이야기를 나누는 공간이 될 거예요.</p>
        <div className="post-poster">SPOILOCK COMMUNITY</div>
      </article>
      <p className="demo-disclaimer">
        커뮤니티는 현재 정적 미리보기이며 게시글은 DB에 저장되지 않습니다.
      </p>
    </main>
  );
}
