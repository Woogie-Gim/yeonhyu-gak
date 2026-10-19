# BACKLOG

범위 밖이라 구현하지 않은 항목. 단계 시작 전에 확인한다.

| 등록 | 항목 | 이유·메모 | 대상 단계 |
|---|---|---|---|
| Phase 0 | 테마 토큰(`YhTheme`) 모듈 위치 결정 | 지금은 `:app/ui/theme`에 있음. feature 모듈이 토큰을 쓰려면 공용 모듈(예: `:core:ui`)이 필요. SPEC 모듈 목록에 없어 추가 전 확인 필요 | Phase 2 시작 전 |
| Phase 0 | 기본 노트 다크 토큰 확정 | 현재 임시값. 대비 4.5:1 테스트로 확정 | Phase 4 |
| Phase 0 | 템플릿 시절 오래된 의존성 버전 갱신 | core-ktx 1.10.1 · activity-compose 1.8.0 · lifecycle 2.6.1 | 별도 정리 |
| Phase 0 | AGP 9 `optimization {}` DSL 전환 | `android.r8.gradual.support` 실험 플래그 필요해 `isMinifyEnabled` 유지 | AGP 안정화 후 |
| Phase 1 | 공휴일 표시 이름 화면 연결 | `HolidayLabels.displayName`(`:core:data`)을 달력·추천 화면에서 사용 | Phase 2 |
| Phase 1 | 예상 연도 대체공휴일 이름 형식 | 예상 계산은 `대체공휴일`, KASI 2026+는 `대체공휴일(원래 이름)`. 날짜·종류는 같음. 표시 형식 통일 여부 결정 | Phase 2 |
