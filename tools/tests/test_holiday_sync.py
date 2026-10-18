import json

from fakes import FAKE_KEY, XML_KEY_ERROR, Recorder, empty, ok
from holiday_sync import EXIT_DIFF, EXIT_ERROR, EXIT_SAME, api_holidays, compare_year, kind_of, run
from kasi.client import KasiClient

ITEMS_2025 = [
    {"locdate": 20250101, "dateName": "1월1일", "isHoliday": "Y"},
    {"locdate": 20250128, "dateName": "설날", "isHoliday": "Y"},
    {"locdate": 20250127, "dateName": "임시공휴일", "isHoliday": "Y"},
    {"locdate": 20250506, "dateName": "대체공휴일", "isHoliday": "Y"},
    {"locdate": 20250603, "dateName": "대통령선거일", "isHoliday": "Y"},
    {"locdate": 20250405, "dateName": "식목일", "isHoliday": "N"},
]


def handler_by_year(data):
    def h(url, params):
        items = data.get(int(params["solYear"]))
        return ok({"item": items}, len(items)) if items else empty()

    return h


def client(data):
    return KasiClient(FAKE_KEY, get=Recorder(handler_by_year(data)))


def test_kind_mapping():
    assert kind_of("설날") == "LUNAR"
    assert kind_of("부처님오신날") == "LUNAR"
    assert kind_of("대체공휴일") == "SUBSTITUTE"
    assert kind_of("임시공휴일") == "TEMPORARY"
    assert kind_of("대통령선거일") == "ELECTION"
    assert kind_of("광복절") == "NATIONAL"


def test_non_holiday_dropped_and_sorted():
    hs = api_holidays(ITEMS_2025)
    assert [h["date"] for h in hs] == ["2025-01-01", "2025-01-27", "2025-01-28", "2025-05-06", "2025-06-03"]


def test_empty_future_year_is_expected_not_error():
    r = compare_year(2034, [], {"confirmed": False, "holidays": []})
    assert r.status == "expected"
    assert not r.is_diff


def test_newly_announced_year_is_diff():
    r = compare_year(2028, api_holidays(ITEMS_2025), {"confirmed": False, "holidays": []})
    assert r.status == "new"
    assert r.is_diff


def test_confirmed_year_vanishing_is_diff():
    assert compare_year(2025, [], {"confirmed": True, "holidays": [{"date": "2025-01-01", "name": "1월1일"}]}).is_diff


def test_changed_year_lists_added_and_removed():
    bundle = {"confirmed": True, "holidays": [{"date": "2025-01-01", "name": "1월1일"}, {"date": "2025-12-31", "name": "가짜"}]}
    r = compare_year(2025, api_holidays(ITEMS_2025[:1]), bundle)
    assert r.status == "changed"
    assert r.removed == [("2025-12-31", "가짜")]


def test_update_then_compare_is_same(tmp_path):
    bundle, golden = tmp_path / "b.json", tmp_path / "g.json"
    data = {2025: ITEMS_2025}
    assert run(client(data), bundle, golden, update=True, years=[2025, 2034]) == EXIT_SAME
    saved = json.loads(bundle.read_text(encoding="utf-8"))
    assert saved["years"]["2025"]["confirmed"] is True
    assert saved["years"]["2034"] == {"confirmed": False, "holidays": []}
    assert list(json.loads(golden.read_text(encoding="utf-8"))) == ["2025"]
    assert run(client(data), bundle, golden, update=False, years=[2025, 2034]) == EXIT_SAME


def test_new_announcement_returns_diff_code(tmp_path):
    bundle, golden = tmp_path / "b.json", tmp_path / "g.json"
    run(client({}), bundle, golden, update=True, years=[2025])
    assert run(client({2025: ITEMS_2025}), bundle, golden, update=False, years=[2025]) == EXIT_DIFF


def test_api_error_returns_error_code_and_hides_key(tmp_path, capsys):
    c = KasiClient(FAKE_KEY, get=Recorder(lambda u, p: XML_KEY_ERROR))
    assert run(c, tmp_path / "b.json", tmp_path / "g.json", update=False, years=[2025]) == EXIT_ERROR
    out = capsys.readouterr()
    assert FAKE_KEY not in out.err + out.out
