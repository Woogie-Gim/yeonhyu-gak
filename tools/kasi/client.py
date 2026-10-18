"""특일정보 · 음양력정보 API 클라이언트.

인증키는 환경 변수 KASI_SERVICE_KEY(Decoding 키)에서만 읽는다.
키 값은 출력·저장하지 않는다.
"""

from __future__ import annotations

import os
import re
import sys
from dataclasses import dataclass
from typing import Any, Callable

import requests

ENV_KEY = "KASI_SERVICE_KEY"
BASE_URL = "https://apis.data.go.kr/B090041/openapi/service"
REST_DE_URL = f"{BASE_URL}/SpcdeInfoService/getRestDeInfo"
SOL_CAL_URL = f"{BASE_URL}/LrsrCldInfoService/getSolCalInfo"
LUN_CAL_URL = f"{BASE_URL}/LrsrCldInfoService/getLunCalInfo"

EXIT_CONFIG_ERROR = 2

MISSING_KEY_MESSAGE = (
    f"환경 변수 {ENV_KEY}가 없어요.\n"
    "공공데이터포털에서 받은 Decoding 인증키를 설정한 뒤 다시 실행해 주세요.\n"
    f"  PowerShell: [Environment]::SetEnvironmentVariable('{ENV_KEY}', '<Decoding 키>', 'User')\n"
    f"  bash:       export {ENV_KEY}='<Decoding 키>'\n"
    "설정 후 터미널을 새로 열어야 반영돼요."
)

_KEY_PATTERN = re.compile(r"(ServiceKey=)[^&\s]+", re.IGNORECASE)


class KasiError(Exception):
    """API 호출 실패. 메시지에 키가 들어가지 않는다."""


def redact(text: str, key: str | None = None) -> str:
    """URL·메시지 속 인증키를 가린다."""
    masked = _KEY_PATTERN.sub(r"\1***", text)
    if key:
        masked = masked.replace(key, "***")
        masked = masked.replace(requests.utils.quote(key, safe=""), "***")
    return masked


def read_service_key(environ: dict[str, str] | None = None) -> str:
    """키가 없으면 안내 후 종료 코드 2로 끝낸다."""
    env = os.environ if environ is None else environ
    key = env.get(ENV_KEY, "").strip()
    if not key:
        print(MISSING_KEY_MESSAGE, file=sys.stderr)
        raise SystemExit(EXIT_CONFIG_ERROR)
    return key


def normalize_items(payload: dict[str, Any]) -> list[dict[str, Any]]:
    """응답 JSON에서 item 목록만 꺼낸다. 빈 응답은 빈 리스트(정상)."""
    response = payload.get("response")
    if not isinstance(response, dict):
        raise KasiError("응답 형식이 예상과 달라요 (response 없음)")
    header = response.get("header") or {}
    code = str(header.get("resultCode", ""))
    if code not in ("00", "0000"):
        raise KasiError(f"API 오류 {code}: {header.get('resultMsg', '')}")
    body = response.get("body") or {}
    items = body.get("items")
    # 미발표 연도는 items가 "" 또는 없음
    if not items or not isinstance(items, dict):
        return []
    item = items.get("item")
    if item is None:
        return []
    if isinstance(item, dict):
        return [item]
    return list(item)


def _xml_error_message(text: str) -> str:
    auth = re.search(r"<returnAuthMsg>(.*?)</returnAuthMsg>", text)
    reason = re.search(r"<returnReasonCode>(.*?)</returnReasonCode>", text)
    msg = re.search(r"<resultMsg>(.*?)</resultMsg>", text)
    parts = [m.group(1) for m in (auth, reason, msg) if m]
    detail = " / ".join(parts) or "알 수 없는 XML 응답"
    hint = ""
    if auth and "SERVICE_KEY" in auth.group(1):
        hint = f" ({ENV_KEY}에 Decoding 키가 맞게 들어갔는지, 활용 신청이 승인됐는지 확인해 주세요)"
    return f"API가 오류를 돌려줬어요: {detail}{hint}"


Getter = Callable[..., requests.Response]


@dataclass
class KasiClient:
    service_key: str
    get: Getter = requests.get
    timeout: float = 15.0

    def _call(self, url: str, params: dict[str, Any]) -> list[dict[str, Any]]:
        query = {"ServiceKey": self.service_key, "_type": "json", **params}
        try:
            resp = self.get(url, params=query, timeout=self.timeout)
        except requests.RequestException as e:
            raise KasiError(f"네트워크 오류: {redact(str(e), self.service_key)}") from None
        text = resp.text or ""
        if text.lstrip().startswith("<"):
            raise KasiError(redact(_xml_error_message(text), self.service_key))
        if resp.status_code != 200:
            raise KasiError(f"HTTP {resp.status_code}: {redact(text[:200], self.service_key)}")
        try:
            payload = resp.json()
        except ValueError:
            raise KasiError(f"JSON이 아닌 응답이에요: {redact(text[:200], self.service_key)}") from None
        try:
            return normalize_items(payload)
        except KasiError as e:
            raise KasiError(redact(str(e), self.service_key)) from None

    def rest_days(self, year: int) -> list[dict[str, Any]]:
        """특일정보 공휴일. 미발표 연도는 빈 리스트."""
        return self._call(REST_DE_URL, {"solYear": year, "numOfRows": 100})

    def lunar_to_solar(self, lun_year: int, lun_month: int, lun_day: int) -> dict[str, Any]:
        """음력 → 양력. 평달 결과만 쓴다."""
        items = self._call(
            SOL_CAL_URL,
            {"lunYear": lun_year, "lunMonth": f"{lun_month:02d}", "lunDay": f"{lun_day:02d}"},
        )
        plain = [i for i in items if str(i.get("lunLeapmonth", "평")) == "평"]
        if not plain:
            raise KasiError(f"음력 {lun_year}-{lun_month:02d}-{lun_day:02d} 변환 결과가 없어요")
        return plain[0]

    def solar_to_lunar(self, year: int, month: int, day: int) -> dict[str, Any]:
        """양력 → 음력. 검증용."""
        items = self._call(
            LUN_CAL_URL,
            {"solYear": year, "solMonth": f"{month:02d}", "solDay": f"{day:02d}"},
        )
        if not items:
            raise KasiError(f"양력 {year}-{month:02d}-{day:02d} 변환 결과가 없어요")
        return items[0]
