"""번들 폰트 출처. 버전·커밋을 고정하고 SHA-256으로 검증한다.

전부 SIL OFL 1.1. 서브셋은 OFL상 수정본이라 예약 이름(Pretendard · Nanum)을 쓰지 않도록
내부 이름을 Yh*로 바꾼다. 원래 이름·저작권은 라이선스 파일과 name 테이블 설명에 남긴다.
"""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
CACHE_DIR = Path(__file__).resolve().parent / ".cache"
FONT_OUT_DIR = REPO_ROOT / "core" / "ui" / "src" / "main" / "res" / "font"
LICENSE_DIR = REPO_ROOT / "licenses"

GOOGLE_FONTS = "https://raw.githubusercontent.com/google/fonts"
NANUM_COMMIT = "ab596b6b0b79ed3932d43911364a597e9d27c873"
DOTO_COMMIT = "7380f7cebbf2250460c23fd301b51bcbab162edf"


@dataclass(frozen=True)
class Download:
    """받을 파일 하나. zip이면 members에서 꺼낸다."""

    url: str
    file: str
    sha256: str


@dataclass(frozen=True)
class FontSource:
    key: str
    name: str
    version: str
    homepage: str
    download: Download
    # zip 안 경로 또는 단일 파일명 → 캐시에 풀 이름
    members: dict[str, str]
    license_member: str
    license_file: str
    # 예약 이름이 있으면 서브셋 이름을 바꾼다
    reserved_name: str | None


SOURCES: dict[str, FontSource] = {
    "pretendard": FontSource(
        key="pretendard",
        name="Pretendard",
        version="1.3.9",
        homepage="https://github.com/orioncactus/pretendard",
        download=Download(
            url="https://github.com/orioncactus/pretendard/releases/download/v1.3.9/Pretendard-1.3.9.zip",
            file="Pretendard-1.3.9.zip",
            sha256="04be351a74d6bf7d60c480a3087e51d185485d35a52023142af1df19eb8c428a",
        ),
        members={
            "public/static/Pretendard-Regular.otf": "Pretendard-Regular.otf",
            "public/static/Pretendard-Medium.otf": "Pretendard-Medium.otf",
            "public/static/Pretendard-Bold.otf": "Pretendard-Bold.otf",
        },
        license_member="LICENSE.txt",
        license_file="Pretendard-OFL.txt",
        reserved_name="Pretendard",
    ),
    "galmuri": FontSource(
        key="galmuri",
        name="Galmuri11",
        version="2.40.4",
        homepage="https://github.com/quiple/galmuri",
        download=Download(
            url="https://github.com/quiple/galmuri/releases/download/v2.40.4/Galmuri-v2.40.4.zip",
            file="Galmuri-v2.40.4.zip",
            sha256="c8b3d9861a62ae73c8b1178091401cd79994812437ef386413f6dd54856e60e7",
        ),
        members={
            "Galmuri11.ttf": "Galmuri11.ttf",
            "Galmuri11-Bold.ttf": "Galmuri11-Bold.ttf",
        },
        license_member="LICENSE.txt",
        license_file="Galmuri-OFL.txt",
        reserved_name=None,
    ),
    "nanumpen": FontSource(
        key="nanumpen",
        name="Nanum Pen Script",
        version=f"google/fonts@{NANUM_COMMIT[:7]}",
        homepage="https://hangeul.naver.com/font",
        download=Download(
            url=f"{GOOGLE_FONTS}/{NANUM_COMMIT}/ofl/nanumpenscript/NanumPenScript-Regular.ttf",
            file="NanumPenScript-Regular.ttf",
            sha256="6f0d1ab29c7894010dc88831fb7a0a51edb79136e450344183de5b1a8b52bd43",
        ),
        members={"NanumPenScript-Regular.ttf": "NanumPenScript-Regular.ttf"},
        license_member=f"{GOOGLE_FONTS}/{NANUM_COMMIT}/ofl/nanumpenscript/OFL.txt",
        license_file="NanumPenScript-OFL.txt",
        reserved_name="Nanum",
    ),
    "doto": FontSource(
        key="doto",
        name="Doto",
        version=f"google/fonts@{DOTO_COMMIT[:7]}",
        homepage="https://github.com/oliverlalan/Doto",
        download=Download(
            url=f"{GOOGLE_FONTS}/{DOTO_COMMIT}/ofl/doto/Doto%5BROND%2Cwght%5D.ttf",
            file="Doto-Variable.ttf",
            sha256="6f4fe7d37853b91df3698daa84cde2dbe1c9695d88c986e6510134910337d426",
        ),
        members={"Doto-Variable.ttf": "Doto-Variable.ttf"},
        license_member=f"{GOOGLE_FONTS}/{DOTO_COMMIT}/ofl/doto/OFL.txt",
        license_file="Doto-OFL.txt",
        reserved_name=None,
    ),
}

# 라이선스 원문 해시 (원격 OFL.txt 검증용)
LICENSE_SHA256 = {
    "NanumPenScript-OFL.txt": "eeacf16032901d0ed0456876ec77b8f0fda6b3fecec7d972f8543eb602e6c30f",
    "Doto-OFL.txt": "26a7b58bdba6cda8a78ca6e8b3791d8013b8abc6d5e6519f84193893aee02020",
}


@dataclass(frozen=True)
class Output:
    """번들 폰트 하나. charset: body = 본문 전체 · display = 앱 문구만 · digits = 숫자만."""

    res_name: str
    source: str
    member: str
    family: str
    style: str
    charset: str
    # 가변 폰트 고정 값
    axes: dict[str, float] | None = None


OUTPUTS: list[Output] = [
    Output("yh_body_regular.otf", "pretendard", "Pretendard-Regular.otf", "YhBody", "Regular", "body"),
    Output("yh_body_medium.otf", "pretendard", "Pretendard-Medium.otf", "YhBody", "Medium", "body"),
    Output("yh_body_bold.otf", "pretendard", "Pretendard-Bold.otf", "YhBody", "Bold", "body"),
    Output("yh_pen.ttf", "nanumpen", "NanumPenScript-Regular.ttf", "YhPen", "Regular", "display"),
    Output("yh_pixel.ttf", "galmuri", "Galmuri11.ttf", "YhPixel", "Regular", "display"),
    Output("yh_pixel_bold.ttf", "galmuri", "Galmuri11-Bold.ttf", "YhPixel", "Bold", "display"),
    # 전광판 전구 느낌: 둥근 점 · 굵게
    Output("yh_dot.ttf", "doto", "Doto-Variable.ttf", "YhDot", "Regular", "digits", {"wght": 800, "ROND": 100}),
]
