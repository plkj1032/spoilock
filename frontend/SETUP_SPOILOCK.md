# SPOILOCK 프론트 MVP 사용법

## 1) 기존 프로젝트에 적용

압축파일의 `frontend` 폴더에서 **src, vite.config.js, index.html**을 기존 `C:\SPOILOCK\frontend`에 복사하세요.
기존 node_modules는 지우거나 복사할 필요가 없습니다.

## 2) 실행

Spring Boot(`localhost:8080`) 실행 후 PowerShell에서:

```powershell
cd C:\SPOILOCK\frontend
npm install
npm run dev
```

브라우저에서 `http://localhost:5173`로 접속하세요.

## 3) 실제 연결된 기능

- `POST /api/auth/signup`: 이메일/비밀번호/닉네임 가입
- `POST /api/auth/login`: JWT HttpOnly 쿠키 로그인
- `GET /api/auth/me`: 현재 로그인 사용자 확인
- `POST /api/auth/refresh`: Access/Refresh Token 회전
- `POST /api/auth/logout`: 로그아웃 및 쿠키 폐기
- `GET /api/auth/csrf`: XSRF-TOKEN 쿠키 준비

`vite.config.js`가 개발 요청 `/api`를 Spring Boot로 프록시하므로 React 파일에 서버 주소를 적을 필요가 없습니다.
Spring Security에는 CookieCsrfTokenRepository.withHttpOnlyFalse() 와 CSRF SPA 설정이 필요합니다.

## 4) 테스트

1. 우측 상단 로그인 -> 회원가입 -> 이메일/비밀번호/닉네임 입력 -> 가입 성공
2. 로그인 -> 닉네임/로그아웃 표시 확인
3. 새로고침 -> 내 정보 유지 확인(만료 시 Refresh API 호출)
4. 로그아웃 -> 내 정보가 사라지는지 확인

## 5) 아직 연동하지 않은 기능

드라마 카드, 평점, 채팅방, SNS는 가상 데이터 기반 데모 화면이며 **실제 DB 데이터가 아닙니다**.
백엔드 `GET /api/dramas`, 사용자 진도, 채팅 REST/WebSocket 등은 팀원 작업과 API 계약이 확정된 뒤 연결합니다.
Google/Kakao OAuth는 미구현이므로 버튼을 넣지 않았습니다.

## 6) 팀 협업

로컬 JWT Secret은 각자 생성합니다. `.env`와 실제 토큰은 Git/단체 채팅에 공유하지 마세요.
`node_modules`는 업로드하거나 Git에 커밋하지 마세요.

## 7) 참고

기존 Postman 테스트에서 보였던 `refreshToken`과 현재 구현한 `refresh_token`은 서로 다른 쿠키입니다.
Refresh Session이 만료/폐기된 상태에서 로그아웃 API는 401을 반환할 수 있습니다.
이 경우 현재 프론트는 오류를 표시하며, 추후 서버가 만료된 Refresh Token에서도 쿠키 삭제를 지원하도록 고도화하면 좋습니다.
