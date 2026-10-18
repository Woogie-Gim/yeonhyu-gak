"""특일정보 API 확정 공휴일과 번들 JSON 비교.

미발표 연도는 API가 빈 응답을 준다. 오류가 아니라 '예상 유지'로 본다.
종료 코드: 0 동일 · 1 차이 있음 · 2 설정·네트워크 오류
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import sys
from dataclasses import dataclass, field
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from kasi.client import KasiClient, KasiError, read_service_key  # noqa: E402
from kasi.paths import BUNDLE_PATH, GOLDEN_PATH, YEARS  # noqa: E402

EXIT_SAME = 0
EXIT_DIFF = 1
EXIT_ERROR = 2

LUNAR_NAMES = ("설날", "추석", "부처님오신날")


def kind_of(name: str) -> str:
    if name.startswith("대체"):
        return "SUBSTITUTE"
    if "임시" in name:
        return "TEMPORARY"
    if "선거" in name:
        return "ELECTION"
    if name in LUNAR_NAMES:
        return "LUNAR"
    return "NATIONAL"


def to_date(locdate) -> str:
    s = str(locdate)
    return dt.date(int(s[:4]), int(s[4:6]), int(s[6:8])).isoformat()


def api_holidays(items: list[dict]) -> list[dict]:
    """isHoliday=Y만 남기고 번들 형식으로 바꾼다."""
    out = [
        {"date": to_date(i["locdate"]), "name": str(i["dateName"]).strip(), "kind": kind_of(str(i["dateName"]).strip())}
        for i in items
        if str(i.get("isHoliday", "Y")) == "Y"
    ]
    return sorted(out, key=lambda h: (h["date"], h["name"]))


@dataclass
class YearResult:
    year: int
    status: str  # same · expected · new · missing · changed
    added: list[tuple[str, str]] = field(default_factory=list)
    removed: list[tuple[str, str]] = field(default_factory=list)

    @property
    def is_diff(self) -> bool:
        return self.status in ("new", "missing", "changed")


def compare_year(year: int, api: list[dict], bundle_year: dict | None) -> YearResult:
    confirmed = bool(bundle_year and bundle_year.get("confirmed"))
    if not api:
        # 빈 응답 = 미발표. 번들에 확정이 있었다면 이상 신호
        return YearResult(year, "missing" if confirmed else "expected")
    if not confirmed:
        return YearResult(year, "new", added=[(h["date"], h["name"]) for h in api])
    a = {(h["date"], h["name"]) for h in api}
    b = {(h["date"], h["name"]) for h in bundle_year.get("holidays", [])}
    if a == b:
        return YearResult(year, "same")
    return YearResult(year, "changed", added=sorted(a - b), removed=sorted(b - a))


STATUS_LABEL = {
    "same": "동일",
    "expected": "미발표 · 예상 유지",
    "new": "새로 발표됨",
    "missing": "API에 없음(확인 필요)",
    "changed": "달라짐",
}


def render_table(results: list[YearResult]) -> str:
    lines = ["| 연도 | 상태 | 추가 | 삭제 |", "|---|---|---|---|"]
    for r in results:
        add = ", ".join(f"{d} {n}" for d, n in r.added) or "-"
        rem = ", ".join(f"{d} {n}" for d, n in r.removed) or "-"
        lines.append(f"| {r.year} | {STATUS_LABEL[r.status]} | {add} | {rem} |")
    return "\n".join(lines)


def load_json(path: Path) -> dict:
    if not path.exists():
        return {}
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def build_bundle(fetched: dict[int, list[dict]]) -> dict:
    years = {}
    for year, hs in fetched.items():
        years[str(year)] = {"confirmed": bool(hs), "holidays": hs}
    return {"schemaVersion": 1, "source": "KASI SpcdeInfoService", "years": years}


def build_golden(raw: dict[int, list[dict]]) -> dict:
    """원본 응답에서 날짜·이름만 남긴 스냅샷. 발표된 연도만."""
    return {
        str(y): [{"locdate": int(i["locdate"]), "dateName": str(i["dateName"]).strip()} for i in items if str(i.get("isHoliday", "Y")) == "Y"]
        for y, items in raw.items()
        if items
    }


def run(client: KasiClient, bundle_path: Path, golden_path: Path, update: bool, years=YEARS) -> int:
    raw: dict[int, list[dict]] = {}
    try:
        for year in years:
            raw[year] = client.rest_days(year)
    except KasiError as e:
        print(f"공휴일 정보를 받지 못했어요. {e}", file=sys.stderr)
        return EXIT_ERROR

    fetched = {y: api_holidays(items) for y, items in raw.items()}
    bundle_years = load_json(bundle_path).get("years", {})
    results = [compare_year(y, fetched[y], bundle_years.get(str(y))) for y in years]
    print(render_table(results))

    if update:
        write_json(bundle_path, build_bundle(fetched))
        write_json(golden_path, build_golden(raw))
        print(f"\n{bundle_path} · {golden_path} 갱신")
        return EXIT_SAME

    diffs = [r for r in results if r.is_diff]
    if diffs:
        print(f"\n차이 {len(diffs)}개 연도: {', '.join(str(r.year) for r in diffs)}")
        return EXIT_DIFF
    print("\n번들과 동일해요.")
    return EXIT_SAME


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="특일정보 API와 번들 공휴일 비교")
    parser.add_argument("--update", action="store_true", help="발표 데이터로 번들·골든 스냅샷 갱신")
    parser.add_argument("--bundle", type=Path, default=BUNDLE_PATH)
    parser.add_argument("--golden", type=Path, default=GOLDEN_PATH)
    args = parser.parse_args(argv)
    client = KasiClient(read_service_key())
    return run(client, args.bundle, args.golden, args.update)


if __name__ == "__main__":
    raise SystemExit(main())
