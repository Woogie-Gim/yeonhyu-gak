# QA_LOG

| 날짜 | 단계 | 통과한 명령 | 추가 테스트 | 남은 이슈 |
|---|---|---|---|---|
| 2026-10-18 | Phase 0 | `ktlintCheck detekt lint` · `:core:engine:test` · `testDebugUnitTest` · `verifyRoborazziDebug` · `:app:assembleDebug :app:assembleRelease` | 4 (엔진 Kotest 2 · Roborazzi 스크린샷 2) | Maestro 미실행(로컬 maestro 미설치 · 에뮬레이터 없음). 릴리스 APK 미서명(keystore.properties 없음, 의도된 동작) |
| 2026-10-19 | Phase 1 (보류 해제) | `ktlintCheck detekt lint` · `:core:engine:test` (64개, 라인 97.4% · 분기 94.5%) · `testDebugUnitTest` · `verifyRoborazziDebug` · `pytest tools/tests` (27개) · `holiday_sync.py` 차이 0건(종료 코드 0) | 20 (엔진 14: 골든 6 · HolidayKey 3 · 노동절·제헌절 2 · 대체공휴일 2026~2028 3 / core:data HolidayLabels 5 / pytest 1) | KASI 키 활성화로 2020~2028 확정 번들·골든 스냅샷 생성, 2029~2035 예상 유지. 예상 계산 = 발표(선거·임시 제외) 2020~2028 전부 일치. 예상 대체공휴일 이름 형식 차이·표시 이름 화면 연결은 BACKLOG. Maestro 해당 없음(UI 변경 없음) |
