"""음력 공휴일 변환표 생성.

설날(음력 1/1) · 부처님오신날(4/8) · 추석(8/15)만 양력으로 바꾼다.
연휴 앞뒤 하루는 앱에서 양력 기준으로 더한다. 음력 12월 말일은 변환하지 않는다.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from kasi.client import KasiClient, KasiError, read_service_key  # noqa: E402
from kasi.paths import LUNAR_TABLE_PATH, YEARS  # noqa: E402

LUNAR_HOLIDAYS = {"seollal": (1, 1), "buddha": (4, 8), "chuseok": (8, 15)}


def _solar_date(item: dict) -> dt.date:
    return dt.date(int(item["solYear"]), int(item["solMonth"]), int(item["solDay"]))


def build_table(client: KasiClient, years=YEARS) -> dict:
    table: dict[str, dict] = {}
    for year in years:
        entry: dict = {}
        for name, (month, day) in LUNAR_HOLIDAYS.items():
            entry[name] = _solar_date(client.lunar_to_solar(year, month, day)).isoformat()
        # 검증용: 설 전날의 음력 날짜(12월이 29일로 끝나는 해 확인)
        eve = dt.date.fromisoformat(entry["seollal"]) - dt.timedelta(days=1)
        lunar = client.solar_to_lunar(eve.year, eve.month, eve.day)
        entry["seollalEveLunarDay"] = int(lunar["lunDay"])
        table[str(year)] = entry
    return {
        "schemaVersion": 1,
        "source": "KASI LrsrCldInfoService",
        "years": table,
    }


def write_json(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="음력 공휴일 변환표 생성")
    parser.add_argument("--out", type=Path, default=LUNAR_TABLE_PATH)
    args = parser.parse_args(argv)

    client = KasiClient(read_service_key())
    try:
        data = build_table(client)
    except KasiError as e:
        print(f"변환표를 만들지 못했어요. {e}", file=sys.stderr)
        return 2
    write_json(args.out, data)
    short = [y for y, e in data["years"].items() if e["seollalEveLunarDay"] == 29]
    print(f"{args.out} 저장 · {len(data['years'])}개 연도 · 섣달그믐이 29일인 해: {', '.join(short) or '없음'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
