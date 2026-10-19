"""공식 배포처에서 폰트 원본을 받아 tools/fonts/.cache 에 푼다.

해시가 다르면 받은 파일을 지우고 실패한다. 라이선스 원문은 licenses/ 로 복사한다.
"""

from __future__ import annotations

import argparse
import hashlib
import sys
import zipfile
from pathlib import Path

import requests

sys.path.insert(0, str(Path(__file__).resolve().parent))

from sources import CACHE_DIR, LICENSE_DIR, LICENSE_SHA256, SOURCES, FontSource  # noqa: E402

TIMEOUT = 60


class FetchError(Exception):
    pass


def sha256_of(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def _get(url: str) -> bytes:
    res = requests.get(url, timeout=TIMEOUT)
    if res.status_code != 200:
        raise FetchError(f"{url} 응답 {res.status_code}")
    return res.content


def fetch_source(src: FontSource, cache: Path = CACHE_DIR, licenses: Path = LICENSE_DIR, get=_get) -> None:
    cache.mkdir(parents=True, exist_ok=True)
    archive = cache / src.download.file
    if not archive.exists() or sha256_of(archive.read_bytes()) != src.download.sha256:
        data = get(src.download.url)
        actual = sha256_of(data)
        if actual != src.download.sha256:
            raise FetchError(f"{src.name} 해시 불일치: {actual}")
        archive.write_bytes(data)

    if zipfile.is_zipfile(archive):
        with zipfile.ZipFile(archive) as z:
            for member, out in src.members.items():
                (cache / out).write_bytes(z.read(member))
            license_text = z.read(src.license_member)
    else:
        license_text = get(src.license_member)
        expected = LICENSE_SHA256[src.license_file]
        if sha256_of(license_text) != expected:
            raise FetchError(f"{src.name} 라이선스 해시 불일치")

    licenses.mkdir(parents=True, exist_ok=True)
    (licenses / src.license_file).write_bytes(license_text.replace(b"\r\n", b"\n"))


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="폰트 원본 받기")
    parser.add_argument("keys", nargs="*", default=list(SOURCES))
    args = parser.parse_args(argv)
    try:
        for key in args.keys:
            fetch_source(SOURCES[key])
            print(f"받음: {SOURCES[key].name} {SOURCES[key].version}")
    except (FetchError, requests.RequestException) as e:
        print(f"폰트를 받지 못했어요. {e}", file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
