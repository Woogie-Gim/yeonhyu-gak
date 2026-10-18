# tools

Python 3.11 이상. 가상환경은 `tools/.venv` (git 제외).

```
python -m venv tools/.venv
tools/.venv/Scripts/pip install -r tools/requirements.txt   # Windows
```

| 폴더 | 용도 | 단계 |
|---|---|---|
| `kasi/` | 천문연구원 API 공용 클라이언트 | Phase 1 |
| `lunar_table/` | 음력 공휴일 변환표 생성 | Phase 1 |
| `holiday_sync/` | 특일정보 API와 번들 비교 | Phase 1 |
| `tests/` | pytest (네트워크 없이 가짜 응답) | Phase 1 |
| `sfx/` | 효과음 합성 | Phase 4 |
| `release_check/` | 릴리스 전 검사 | Phase 6 |

## 인증키
공공데이터포털 **Decoding** 키를 환경 변수 `KASI_SERVICE_KEY`에 둔다. 특일정보·음양력 API 공용.
코드·파일·커밋에 키 값을 쓰지 않는다. CI는 GitHub Secrets `KASI_SERVICE_KEY`.

```
[Environment]::SetEnvironmentVariable('KASI_SERVICE_KEY', '<Decoding 키>', 'User')   # 새 터미널부터 반영
```

## 실행
```
tools/.venv/Scripts/python tools/lunar_table/lunar_table.py          # → core/data/src/main/assets/holidays/lunar_table.json
tools/.venv/Scripts/python tools/holiday_sync/holiday_sync.py        # 번들과 비교. 0 동일 · 1 차이 · 2 오류
tools/.venv/Scripts/python tools/holiday_sync/holiday_sync.py --update   # 번들 + 엔진 골든 스냅샷 갱신
tools/.venv/Scripts/python -m pytest -c tools/pytest.ini tools/tests
```

- 설·추석은 음력 1/1 · 8/15만 변환하고 연휴 앞뒤 하루는 앱이 양력으로 더한다. 음력 12월 말일은 변환하지 않는다.
- 특일정보 API는 미발표 연도에 빈 응답을 준다. 정상이며 해당 연도는 예상 데이터로 남는다.
