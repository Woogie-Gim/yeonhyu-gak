import string

import pytest
from fontTools.ttLib import TTFont

from fetch import FetchError, fetch_source
from sources import FONT_OUT_DIR, LICENSE_DIR, OUTPUTS, SOURCES, Download, FontSource
from subset import DIGITS, app_text_chars, ks_x_1001_hangul

MAX_BYTES = {"body": 400_000, "display": 150_000, "digits": 20_000}
RESERVED_NAME_IDS = (1, 4, 6, 16)


def _cmap(res_name: str) -> set[str]:
    return {chr(c) for c in TTFont(FONT_OUT_DIR / res_name).getBestCmap()}


def _hangul_and_alnum(chars: set[str]) -> set[str]:
    return {c for c in chars if "가" <= c <= "힣" or c in string.ascii_letters + string.digits}


@pytest.mark.parametrize("out", OUTPUTS, ids=lambda o: o.res_name)
def test_bundled_font_exists_and_small(out):
    path = FONT_OUT_DIR / out.res_name
    assert path.exists(), "tools/fonts/subset.py 를 실행해 주세요"
    assert path.stat().st_size <= MAX_BYTES[out.charset]


@pytest.mark.parametrize("out", [o for o in OUTPUTS if o.charset != "digits"], ids=lambda o: o.res_name)
def test_app_text_covered(out):
    # 문구가 바뀌었는데 서브셋을 다시 만들지 않으면 실패
    missing = _hangul_and_alnum(app_text_chars()) - _cmap(out.res_name)
    assert missing == set(), f"빠진 글자: {''.join(sorted(missing))}"


@pytest.mark.parametrize("out", [o for o in OUTPUTS if o.charset == "body"], ids=lambda o: o.res_name)
def test_body_covers_ks_x_1001(out):
    hangul = ks_x_1001_hangul()
    assert len(hangul) == 2350
    assert set(hangul) <= _cmap(out.res_name)


def test_dot_font_has_digits():
    assert set(DIGITS) <= _cmap("yh_dot.ttf")


@pytest.mark.parametrize("out", OUTPUTS, ids=lambda o: o.res_name)
def test_reserved_names_not_used(out):
    # OFL 예약 이름(Pretendard · Nanum)은 수정본 이름에 쓰지 않는다
    names = TTFont(FONT_OUT_DIR / out.res_name)["name"]
    for rec in names.names:
        if rec.nameID in RESERVED_NAME_IDS:
            value = rec.toUnicode()
            for src in SOURCES.values():
                if src.reserved_name:
                    assert src.reserved_name not in value, value


def test_licenses_present():
    for src in SOURCES.values():
        text = (LICENSE_DIR / src.license_file).read_text(encoding="utf-8")
        assert "SIL Open Font License" in text or "SIL OPEN FONT LICENSE" in text
    assert (LICENSE_DIR / "README.md").exists()


def test_fetch_rejects_wrong_hash(tmp_path):
    src = FontSource(
        key="fake",
        name="Fake",
        version="0",
        homepage="https://example.invalid",
        download=Download(url="https://example.invalid/f.ttf", file="f.ttf", sha256="0" * 64),
        members={"f.ttf": "f.ttf"},
        license_member="https://example.invalid/OFL.txt",
        license_file="Fake-OFL.txt",
        reserved_name=None,
    )
    with pytest.raises(FetchError):
        fetch_source(src, cache=tmp_path / "cache", licenses=tmp_path / "lic", get=lambda url: b"tampered")
    assert not (tmp_path / "cache" / "f.ttf").exists()
