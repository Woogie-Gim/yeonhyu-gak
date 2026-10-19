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
환경 변수 `KASI_SERVICE_KEY` 하나로 특일정보·음양력 API를 같이 쓴다. CI는 GitHub Secrets `KASI_SERVICE_KEY`.

> **키 값은 코드·문서·커밋·이슈·로그 어디에도 절대 넣지 않는다.** 환경 변수와 GitHub Secrets에만 둔다.

### Encoding 키를 디코딩해서 넣는다
공공데이터포털 마이페이지는 인증키를 **Encoding 형태(`%2B` `%2F` `%3D` 포함)로만** 보여준다.
클라이언트가 요청할 때 다시 URL 인코딩하므로 환경 변수에는 **URL 디코딩한 값**을 넣어야 한다.
그대로 넣으면 이중 인코딩돼서 `SERVICE_KEY_IS_NOT_REGISTERED_ERROR`가 난다.

Git Bash (입력이 화면·history에 남지 않게 `read -rs`로 받는다):
```
read -rs ENC                      # Encoding 키 붙여넣고 Enter
export KASI_SERVICE_KEY="$(printf '%s' "$ENC" | sed -e 's/%2B/+/g' -e 's/%2F/\//g' -e 's/%3D/=/g')"
unset ENC
```

계속 쓰려면 Windows 사용자 환경 변수로 저장한다 (새 터미널부터 반영):
```
powershell -NoProfile -Command "[Environment]::SetEnvironmentVariable('KASI_SERVICE_KEY', \$env:KASI_SERVICE_KEY, 'User')"
```

### 디코딩 확인 (키 값은 출력하지 않는다)
```
printf '%s' "$KASI_SERVICE_KEY" | wc -c                                      # 길이만. Encoding 키보다 짧아야 정상
case "$KASI_SERVICE_KEY" in *%*) echo "아직 인코딩된 값";; *) echo "디코딩됨";; esac
```
- 디코딩하면 `%2B`(3자) → `+`(1자)처럼 줄어든다. 길이가 Encoding 키와 같으면 디코딩이 안 된 것.
- 비어 있으면 `wc -c`가 `0`.

### 오류 진단
| API 응답 | 코드 | 원인 | 조치 |
|---|---|---|---|
| `SERVICE_KEY_IS_NOT_REGISTERED_ERROR` | 30 | 이중 인코딩. Encoding 키를 디코딩 없이 넣음 (또는 키 활성화 전) | 위 디코딩 명령으로 다시 설정. 디코딩했는데도 나면 포털에서 활성화 상태 확인 후 대기 |
| `SERVICE_KEY_IS_NULL` | 20 | 변수가 비어 있음. 새 터미널에 반영 안 됨 · 오타 | `wc -c`로 길이 확인 후 다시 설정 |

## 실행
```
tools/.venv/Scripts/python tools/lunar_table/lunar_table.py          # → core/data/src/main/assets/holidays/lunar_table.json
tools/.venv/Scripts/python tools/holiday_sync/holiday_sync.py        # 번들과 비교. 0 동일 · 1 차이 · 2 오류
tools/.venv/Scripts/python tools/holiday_sync/holiday_sync.py --update   # 번들 + 엔진 골든 스냅샷 갱신
tools/.venv/Scripts/python -m pytest -c tools/pytest.ini tools/tests
```

- 설·추석은 음력 1/1 · 8/15만 변환하고 연휴 앞뒤 하루는 앱이 양력으로 더한다. 음력 12월 말일은 변환하지 않는다.
- 특일정보 API는 미발표 연도에 빈 응답을 준다. 정상이며 해당 연도는 예상 데이터로 남는다.
