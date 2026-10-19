# 번들 폰트 라이선스

모든 폰트는 SIL Open Font License 1.1. 원문은 이 폴더의 `*-OFL.txt`.
앱에는 `tools/fonts/subset.py`로 필요한 글자만 남긴 서브셋을 넣는다. 서브셋은 OFL상 수정본이므로
예약 글꼴 이름(Pretendard · Nanum 등)을 쓰지 않도록 내부 글꼴 이름을 `Yh*`로 바꿨다.
원본 저작권 표기와 라이선스 항목은 폰트 name 테이블에 그대로 남아 있다.

| 앱 리소스 | 내부 이름 | 원본 | 버전 | 공식 배포처 | 라이선스 |
|---|---|---|---|---|---|
| `yh_body_regular.otf` · `yh_body_medium.otf` · `yh_body_bold.otf` | YhBody | Pretendard (Kil Hyung-jin) | 1.3.9 | https://github.com/orioncactus/pretendard/releases/tag/v1.3.9 | `Pretendard-OFL.txt` |
| `yh_pen.ttf` | YhPen | Nanum Pen Script (NAVER · Sandoll) | google/fonts@ab596b6 | https://github.com/google/fonts/tree/ab596b6b0b79ed3932d43911364a597e9d27c873/ofl/nanumpenscript (네이버 나눔글꼴 OFL 배포본) | `NanumPenScript-OFL.txt` |
| `yh_pixel.ttf` · `yh_pixel_bold.ttf` | YhPixel | Galmuri11 (Lee Minseo) | 2.40.4 | https://github.com/quiple/galmuri/releases/tag/v2.40.4 | `Galmuri-OFL.txt` |
| `yh_dot.ttf` | YhDot | Doto (Oliver Lalan) · wght 800 · ROND 100 고정 | google/fonts@7380f7c | https://github.com/google/fonts/tree/7380f7cebbf2250460c23fd301b51bcbab162edf/ofl/doto | `Doto-OFL.txt` |

다시 만들기:
```
tools/.venv/Scripts/python tools/fonts/fetch.py    # 받기 + SHA-256 검증 + 라이선스 복사
tools/.venv/Scripts/python tools/fonts/subset.py   # 서브셋 → core/ui/src/main/res/font
```

본문 서브셋은 KS X 1001 한글 2,350자 + 호환 자모 + 라틴을 담는다. 그 밖의 글자(메모에 쓴 드문 한글 등)는
안드로이드 시스템 글꼴로 대체되어 표시된다.
