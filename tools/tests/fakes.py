"""네트워크 없이 API 응답을 흉내 낸다."""

from __future__ import annotations

import json
from typing import Any, Callable

FAKE_KEY = "FAKE+KEY/abc=="


class FakeResponse:
    def __init__(self, text: str, status_code: int = 200):
        self.text = text
        self.status_code = status_code

    def json(self) -> Any:
        return json.loads(self.text)


def ok(items: Any, total: int | None = None) -> FakeResponse:
    body = {"items": items, "numOfRows": 100, "pageNo": 1, "totalCount": total if total is not None else 0}
    return FakeResponse(json.dumps({"response": {"header": {"resultCode": "00", "resultMsg": "NORMAL SERVICE."}, "body": body}}))


def empty() -> FakeResponse:
    # 미발표 연도 실제 응답 형태: items가 빈 문자열
    return ok("", 0)


XML_KEY_ERROR = FakeResponse(
    "<OpenAPI_ServiceResponse><cmmMsgHeader><errMsg>SERVICE ERROR</errMsg>"
    "<returnAuthMsg>SERVICE_KEY_IS_NOT_REGISTERED_ERROR</returnAuthMsg>"
    "<returnReasonCode>30</returnReasonCode></cmmMsgHeader></OpenAPI_ServiceResponse>"
)


class Recorder:
    """요청 파라미터를 기록하고 handler로 응답을 만든다."""

    def __init__(self, handler: Callable[[str, dict], FakeResponse]):
        self.handler = handler
        self.calls: list[tuple[str, dict]] = []

    def __call__(self, url: str, params: dict, timeout: float) -> FakeResponse:
        self.calls.append((url, dict(params)))
        return self.handler(url, params)
