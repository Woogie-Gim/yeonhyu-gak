import pytest
import requests

from fakes import FAKE_KEY, XML_KEY_ERROR, FakeResponse, Recorder, empty, ok
from kasi.client import ENV_KEY, KasiClient, KasiError, normalize_items, read_service_key, redact


def test_missing_key_exits_with_friendly_message(capsys):
    with pytest.raises(SystemExit) as e:
        read_service_key({})
    assert e.value.code == 2
    err = capsys.readouterr().err
    assert ENV_KEY in err
    assert "Decoding" in err


def test_blank_key_is_missing():
    with pytest.raises(SystemExit):
        read_service_key({ENV_KEY: "   "})


def test_key_is_read_from_env_only():
    assert read_service_key({ENV_KEY: FAKE_KEY}) == FAKE_KEY


def test_empty_items_string_is_normal():
    assert normalize_items(empty().json()) == []


def test_missing_items_is_normal():
    payload = {"response": {"header": {"resultCode": "00"}, "body": {"totalCount": 0}}}
    assert normalize_items(payload) == []


def test_single_item_dict_becomes_list():
    item = {"locdate": 20250101, "dateName": "1월1일", "isHoliday": "Y"}
    assert normalize_items(ok({"item": item}, 1).json()) == [item]


def test_item_list_kept():
    items = [{"locdate": 20250101}, {"locdate": 20250301}]
    assert normalize_items(ok({"item": items}, 2).json()) == items


def test_error_result_code_raises():
    payload = {"response": {"header": {"resultCode": "22", "resultMsg": "LIMITED"}, "body": {}}}
    with pytest.raises(KasiError, match="22"):
        normalize_items(payload)


def test_rest_days_empty_year_returns_empty_list():
    client = KasiClient(FAKE_KEY, get=Recorder(lambda u, p: empty()))
    assert client.rest_days(2034) == []


def test_key_passed_as_param_not_in_url():
    rec = Recorder(lambda u, p: empty())
    KasiClient(FAKE_KEY, get=rec).rest_days(2025)
    url, params = rec.calls[0]
    assert FAKE_KEY not in url
    assert params["ServiceKey"] == FAKE_KEY
    assert params["_type"] == "json"


def test_xml_error_is_explained_without_key():
    client = KasiClient(FAKE_KEY, get=Recorder(lambda u, p: XML_KEY_ERROR))
    with pytest.raises(KasiError) as e:
        client.rest_days(2025)
    assert "SERVICE_KEY_IS_NOT_REGISTERED_ERROR" in str(e.value)
    assert ENV_KEY in str(e.value)
    assert FAKE_KEY not in str(e.value)


def test_network_error_message_hides_key():
    def boom(url, params, timeout):
        raise requests.ConnectionError(f"failed {url}?ServiceKey={FAKE_KEY}&solYear=2025")

    with pytest.raises(KasiError) as e:
        KasiClient(FAKE_KEY, get=boom).rest_days(2025)
    assert FAKE_KEY not in str(e.value)
    assert "ServiceKey=***" in str(e.value)


def test_http_error_hides_key():
    client = KasiClient(FAKE_KEY, get=Recorder(lambda u, p: FakeResponse(f"bad {FAKE_KEY}", 500)))
    with pytest.raises(KasiError) as e:
        client.rest_days(2025)
    assert FAKE_KEY not in str(e.value)


def test_redact_encoded_key():
    encoded = requests.utils.quote(FAKE_KEY, safe="")
    assert FAKE_KEY not in redact(f"x {encoded} y", FAKE_KEY)
    assert encoded not in redact(f"x {encoded} y", FAKE_KEY)


def test_lunar_to_solar_picks_plain_month():
    items = [
        {"solYear": "2033", "solMonth": "09", "solDay": "23", "lunLeapmonth": "평"},
        {"solYear": "2033", "solMonth": "10", "solDay": "22", "lunLeapmonth": "윤"},
    ]
    client = KasiClient(FAKE_KEY, get=Recorder(lambda u, p: ok({"item": items}, 2)))
    assert client.lunar_to_solar(2033, 8, 15)["solMonth"] == "09"
