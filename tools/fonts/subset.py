"""캐시의 폰트 원본을 앱에 필요한 글자만 남겨 core/ui/res/font 로 내보낸다.

- body(본문 Pretendard): KS X 1001 한글 2,350자 + 호환 자모 + 라틴 + 앱 문구 글자
- display(테마 제목·숫자): 앱 문구(strings.xml) + 공휴일 이름 + ASCII 글자만
- digits(전광판 숫자): 숫자와 몇 가지 기호만
앱 문구가 바뀌면 다시 실행한다. tools/tests/test_fonts.py 가 빠진 글자를 잡는다.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

sys.path.insert(0, str(Path(__file__).resolve().parent))

from sources import CACHE_DIR, FONT_OUT_DIR, OUTPUTS, REPO_ROOT, SOURCES, Output  # noqa: E402

HOLIDAY_JSON = REPO_ROOT / "core" / "data" / "src" / "main" / "assets" / "holidays" / "kr_holidays.json"
ENGINE_SRC = REPO_ROOT / "core" / "engine" / "src" / "main" / "kotlin"

DIGITS = "0123456789"
DIGIT_EXTRAS = " .,:-/~+%"
# 본문에 자주 쓰는 기호
BODY_SYMBOLS = "·…‘’“”–—•→←↑↓▾▴×÷±≥≤※○●◎◇◆□■△▲▽▼☆★♡♥∼〈〉《》「」『』【】"

_HANGUL = re.compile(r"[가-힣]")
_STRING_LITERAL = re.compile(r'"([^"\\]*(?:\\.[^"\\]*)*)"')


def ks_x_1001_hangul() -> str:
    """EUC-KR 한글 영역(0xB0A1~0xC8FE) 완성형 2,350자."""
    chars = []
    for lead in range(0xB0, 0xC9):
        for trail in range(0xA1, 0xFF):
            try:
                chars.append(bytes([lead, trail]).decode("euc-kr"))
            except UnicodeDecodeError:
                continue
    return "".join(chars)


def strings_xml_files(root: Path = REPO_ROOT) -> list[Path]:
    return sorted(
        p
        for p in root.glob("**/src/main/res/values*/strings.xml")
        if "build" not in p.parts and ".venv" not in p.parts
    )


def app_text_chars(root: Path = REPO_ROOT) -> set[str]:
    """화면에 나올 수 있는 고정 문구 글자: strings.xml · 공휴일 이름 · 엔진 문자열 상수."""
    chars: set[str] = set()
    for path in strings_xml_files(root):
        text = path.read_text(encoding="utf-8")
        text = re.sub(r"<!--.*?-->", "", text, flags=re.S)
        chars.update(re.sub(r"<[^>]+>", "", text))
    bundle = json.loads((root / HOLIDAY_JSON.relative_to(REPO_ROOT)).read_text(encoding="utf-8"))
    for year in bundle["years"].values():
        for h in year["holidays"]:
            chars.update(h["name"])
    for kt in (root / ENGINE_SRC.relative_to(REPO_ROOT)).glob("**/*.kt"):
        for literal in _STRING_LITERAL.findall(kt.read_text(encoding="utf-8")):
            chars.update(_HANGUL.findall(literal))
    return {c for c in chars if c.isprintable() and not c.isspace()} | {" "}


def ascii_printable() -> set[str]:
    return {chr(c) for c in range(0x20, 0x7F)}


def charset(kind: str, root: Path = REPO_ROOT) -> set[str]:
    if kind == "digits":
        return set(DIGITS + DIGIT_EXTRAS)
    display = app_text_chars(root) | ascii_printable()
    if kind == "display":
        return display
    if kind == "body":
        jamo = {chr(c) for c in range(0x3131, 0x318F)}
        latin1 = {chr(c) for c in range(0xA0, 0x100)}
        return display | set(ks_x_1001_hangul()) | jamo | latin1 | set(BODY_SYMBOLS)
    raise ValueError(kind)


def _rename(font: TTFont, out: Output) -> None:
    """OFL 예약 이름 회피: 이름 표의 글꼴 이름을 Yh*로. 저작권·라이선스 항목은 그대로 둔다."""
    src = SOURCES[out.source]
    full = f"{out.family} {out.style}"
    ps = f"{out.family}-{out.style}"
    names = {
        1: out.family,
        2: out.style,
        3: f"{ps};subset of {src.name} {src.version}",
        4: full,
        6: ps,
        10: f"Subset of {src.name} {src.version} ({src.homepage}) for YeonhyuGak. SIL OFL 1.1.",
        16: out.family,
        17: out.style,
    }
    table = font["name"]
    for rec in list(table.names):
        if rec.nameID in (1, 2, 3, 4, 6, 16, 17, 21, 22, 25):
            table.removeNames(nameID=rec.nameID)
    for name_id, value in names.items():
        table.setName(value, name_id, 3, 1, 0x409)
    if "CFF " in font:
        cff = font["CFF "].cff
        cff.fontNames = [ps]
        top = cff.topDictIndex[0]
        top.FullName = full
        top.FamilyName = out.family


def build(out: Output, cache: Path = CACHE_DIR, dest: Path = FONT_OUT_DIR, root: Path = REPO_ROOT) -> Path:
    font = TTFont(cache / out.member)
    if out.axes:
        font = instancer.instantiateVariableFont(font, out.axes, updateFontNames=False)
        # 한 굵기만 쓰므로 Regular로 표시 (fsSelection·macStyle 일치)
        font["OS/2"].fsSelection = (font["OS/2"].fsSelection & ~0b100001) | 0b1000000
        font["head"].macStyle &= ~1
    cmap = font.getBestCmap()
    wanted = sorted(ord(c) for c in charset(out.charset, root) if ord(c) in cmap)

    options = subset.Options()
    options.hinting = False
    options.name_IDs = ["*"]
    options.name_languages = ["*"]
    options.notdef_outline = True
    # 날짜 숫자 폭 고정(tnum)
    options.layout_features = subset.Options().layout_features + ["tnum"]
    subsetter = subset.Subsetter(options)
    subsetter.populate(unicodes=wanted)
    subsetter.subset(font)
    _rename(font, out)

    dest.mkdir(parents=True, exist_ok=True)
    path = dest / out.res_name
    font.save(path)
    return path


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="폰트 서브셋 만들기")
    parser.parse_args(argv)
    missing = [o.member for o in OUTPUTS if not (CACHE_DIR / o.member).exists()]
    if missing:
        print(f"원본이 없어요: {', '.join(missing)}. 먼저 tools/fonts/fetch.py 를 실행해 주세요", file=sys.stderr)
        return 2
    for out in OUTPUTS:
        path = build(out)
        print(f"{path.name}: {path.stat().st_size:,} B")
    return 0


if __name__ == "__main__":
    sys.exit(main())
