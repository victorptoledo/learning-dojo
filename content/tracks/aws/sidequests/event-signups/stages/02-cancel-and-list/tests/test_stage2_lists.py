import base64
import json

import pytest


def emails(count):
    return [f"guest{number:03}@example.com" for number in range(count)]


def fill(api, count, event_id="rust-meetup"):
    api.event(event_id, capacity=max(count, 1))
    for email in emails(count):
        assert api.sign_up(email, event_id=event_id).status == 201
    api.calls.clear()


def pages(api, event_id="rust-meetup", **query):
    collected, cursor, seen = [], None, 0
    while True:
        params = {key: str(value) for key, value in query.items()}
        if cursor:
            params["cursor"] = cursor
        page = api.call("GET", f"/events/{event_id}/signups", query=params or None)
        assert page.status == 200, f"list: {page.status} {page.body}"
        assert set(page.body) == {"items", "next"}, f"answer {{'items': [...], 'next': ...}}, got {page.body}"
        collected.append(page.body["items"])
        cursor = page.body["next"]
        seen += 1
        assert seen < 50, "the cursor never reaches null"
        if cursor is None:
            return collected


def test_cancelling_frees_the_seat(api):
    api.event(capacity=1)
    api.sign_up("ana@example.com")
    api.calls.clear()
    gone = api.call("DELETE", "/events/rust-meetup/signups/ana@example.com")
    assert gone.status == 204 and gone.body is None, f"cancel: {gone.status} {gone.body}"
    assert api.operations() == ["TransactWriteItems"], f"cancel in one transaction, sent {api.operations()}"
    assert api.item("EVENT#rust-meetup", "SIGNUP#ana@example.com") is None, "the sign-up item must be gone"
    assert api.call("GET", "/events/rust-meetup").body["taken"] == 0, "the seat must be free again"
    assert api.sign_up("bo@example.com").status == 201, "someone else can now take the seat"


def test_the_email_in_the_path_is_normalized(api):
    api.event()
    api.sign_up("ana@example.com")
    assert api.call("DELETE", "/events/rust-meetup/signups/ANA@Example.com").status == 204


def test_cancelling_twice_frees_one_seat(api):
    api.event(capacity=3)
    for email in ("ana@example.com", "bo@example.com"):
        api.sign_up(email)
    assert api.call("DELETE", "/events/rust-meetup/signups/ana@example.com").status == 204
    again = api.call("DELETE", "/events/rust-meetup/signups/ana@example.com")
    assert again.status == 404 and again.body == {"error": "not signed up"}, f"second cancel: {again.status} {again.body}"
    assert api.call("GET", "/events/rust-meetup").body["taken"] == 1, "the second cancel must not free another seat"


def test_cancelling_for_an_unknown_event_is_404(api):
    found = api.call("DELETE", "/events/nope/signups/ana@example.com")
    assert found.status == 404, f"{found.status} {found.body}"
    assert api.items() == [], "nothing may be created by a failed cancel"


def test_the_list_is_alphabetical_and_skips_the_event_item(api):
    api.event(capacity=5)
    for email in ("cy@example.com", "ana@example.com", "bo@example.com"):
        api.sign_up(email)
    page = api.call("GET", "/events/rust-meetup/signups")
    assert page.status == 200 and page.body == {
        "items": ["ana@example.com", "bo@example.com", "cy@example.com"], "next": None}, f"{page.body}"


def test_an_event_with_nobody_lists_nothing(api):
    api.event()
    page = api.call("GET", "/events/rust-meetup/signups")
    assert page.status == 200 and page.body == {"items": [], "next": None}, f"{page.status} {page.body}"


def test_listing_an_unknown_event_is_404(api):
    found = api.call("GET", "/events/nope/signups")
    assert found.status == 404 and found.body == {"error": "no such event"}, f"{found.status} {found.body}"


@pytest.mark.parametrize("count,limit,sizes", [
    (130, None, [50, 50, 30]),
    (100, 50, [50, 50]),
    (7, 3, [3, 3, 1]),
    (5, 100, [5]),
    (1, 1, [1]),
])
def test_pages_visit_every_sign_up_once(api, count, limit, sizes):
    fill(api, count)
    collected = pages(api, **({"limit": limit} if limit else {}))
    assert [len(page) for page in collected] == sizes, (
        f"{count} sign-ups with limit {limit or 50}: pages of {sizes}, got {[len(p) for p in collected]} "
        "(next must be null on the last page, even when it is full)")
    flat = [email for page in collected for email in page]
    assert flat == emails(count), "following next must visit every sign-up exactly once, in order"
    assert "Scan" not in api.operations(), "list with query, never scan"
    for name, params in api.calls:
        if name == "Query":
            assert params.get("Limit") and params["Limit"] <= 101, "pass a Limit to every query"


def test_other_events_do_not_leak_into_the_list(api):
    fill(api, 3)
    api.event("go-meetup", capacity=2)
    api.sign_up("zed@example.com", event_id="go-meetup")
    assert pages(api) == [emails(3)], "list only this event's sign-ups"


def test_the_cursor_is_opaque_text(api):
    fill(api, 4)
    first = api.call("GET", "/events/rust-meetup/signups", query={"limit": "2"}).body
    assert isinstance(first["next"], str) and "@" not in first["next"], (
        "the cursor must be opaque text, not the bare email")


@pytest.mark.parametrize("cursor", ["not-a-cursor!", base64.urlsafe_b64encode(b"[1,2]").decode(), "e30"])
def test_a_cursor_the_handler_cannot_read_is_400(api, cursor):
    fill(api, 3)
    found = api.call("GET", "/events/rust-meetup/signups", query={"cursor": cursor})
    assert found.status == 400, f"cursor {cursor!r} must answer 400, got {found.status} {found.body}"


@pytest.mark.parametrize("limit", ["0", "101", "-5", "ten"])
def test_limits_outside_1_to_100_are_400(api, limit):
    api.event()
    found = api.call("GET", "/events/rust-meetup/signups", query={"limit": limit})
    assert found.status == 400, f"limit={limit} must answer 400, got {found.status}"


def test_base64_bodies_are_decoded(api):
    api.event()
    body = base64.b64encode(json.dumps({"email": "ana@example.com"}).encode()).decode()
    event = {"version": "2.0", "routeKey": "POST /events/{id}/signups", "rawPath": "/events/rust-meetup/signups",
             "pathParameters": {"id": "rust-meetup"}, "headers": {"content-type": "application/json"},
             "body": body, "isBase64Encoded": True}
    answer = api.module.handler(event, None)
    assert answer["statusCode"] == 201, f"a base64 body must be decoded first, got {answer}"
