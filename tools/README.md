# tools

Python 3.11 이상. 가상환경은 `tools/.venv` (git 제외).

```
python -m venv tools/.venv
tools/.venv/Scripts/pip install -r tools/requirements.txt   # Windows
```

| 폴더 | 용도 | 단계 |
|---|---|---|
| `lunar_table/` | 음력 공휴일 변환표 생성 | Phase 1 |
| `holiday_sync/` | 특일정보 API와 번들 비교 | Phase 1 |
| `sfx/` | 효과음 합성 | Phase 4 |
| `release_check/` | 릴리스 전 검사 | Phase 6 |
