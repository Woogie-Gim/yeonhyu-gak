import datetime as dt

import pytest

from fakes import FAKE_KEY, Recorder, ok
from kasi.client import SOL_CAL_URL, KasiClient
from lunar_table import build_table

# 음력 → 양력 (실제 값)
SOLAR = {
    (2024, 1, 1): "2024-02-10",
    (2024, 4, 8): "2024-05-15",
    (2024, 8, 15): "2024-09-17",
    (2025, 1, 1): "2025-01-29",
    (2025, 4, 8): "2025-05-05",
    (2025, 8, 15): "2025-10-06",
}
# 양력 설 전날 → 음력 일. 2025는 음력 12월이 29일로 끝난다
EVE_LUNAR_DAY = {"2024-02-09": 30, "2025-01-28": 29}


def handler(url, params):
    if url == SOL_CAL_URL:
        y, m, d = int(params["lunYear"]), int(params["lunMonth"]), int(params["lunDay"])
        if m == 12:
            pytest.fail(f"음력 12월 날짜를 변환하려 했어요: {params['lunMonth']}/{params['lunDay']}")
        sol = dt.date.fromisoformat(SOLAR[(y, m, d)])
        return ok({"item": {"solYear": str(sol.year), "solMonth": f"{sol.month:02d}", "solDay": f"{sol.day:02d}", "lunLeapmonth": "평"}}, 1)
    sol = dt.date(int(params["solYear"]), int(params["solMonth"]), int(params["solDay"]))
    return ok({"item": {"lunDay": str(EVE_LUNAR_DAY[sol.isoformat()]), "lunMonth": "12"}}, 1)


def test_table_has_only_new_year_buddha_chuseok():
    rec = Recorder(handler)
    table = build_table(KasiClient(FAKE_KEY, get=rec), years=[2024, 2025])
    assert table["years"]["2025"]["seollal"] == "2025-01-29"
    assert table["years"]["2025"]["buddha"] == "2025-05-05"
    assert table["years"]["2025"]["chuseok"] == "2025-10-06"
    asked = {(p["lunMonth"], p["lunDay"]) for u, p in rec.calls if u == SOL_CAL_URL}
    assert asked == {("01", "01"), ("04", "08"), ("08", "15")}


def test_year_with_29_day_last_month_is_recorded():
    table = build_table(KasiClient(FAKE_KEY, get=Recorder(handler)), years=[2024, 2025])
    assert table["years"]["2025"]["seollalEveLunarDay"] == 29
    assert table["years"]["2024"]["seollalEveLunarDay"] == 30
