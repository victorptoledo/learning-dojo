import pytest

DAY = 24 * 60 * 60
T0 = 1_800_000_000.0


@pytest.fixture
def clock(api, monkeypatch):
    class Clock:
        time = T0

    assert hasattr(api.module, "now"), "add a module-level now() that returns time.time()"
    monkeypatch.setattr(api.module, "now", lambda: Clock.time)
    return Clock


def key(value):
    return {"Idempotency-Key": value}


def test_the_answer_is_stored_in_the_sign_up_transaction(api, clock):
    api.event()
    api.calls.clear()
    first = api.sign_up("ana@example.com", headers=key("k-1"))
    assert first.status == 201 and first.body == {"event": "rust-meetup", "email": "ana@example.com"}
    writes = [params for name, params in api.calls if name == "TransactWriteItems"]
    assert len(writes) == 1 and len(writes[0]["TransactItems"]) == 3, (
        "store the answer as a third operation of the sign-up transaction")
    assert not [name for name in api.operations() if name in ("PutItem", "UpdateItem")], (
        "no separate write for the idempotency record")
    stored = api.item("IDEMPOTENCY#k-1", "KEY")
    assert stored is not None, "store the answer as IDEMPOTENCY#<key> / KEY"
    assert "N" in stored.get("expires_at", {}), "expires_at must be a DynamoDB number (TTL ignores strings)"
    assert int(stored["expires_at"]["N"]) == int(T0) + DAY, "expires_at = int(now()) + 86400, in seconds"


def test_a_retry_replays_the_answer_and_writes_nothing(api, clock):
    api.event(capacity=3)
    first = api.sign_up("ana@example.com", headers=key("k-1"))
    api.calls.clear()
    retry = api.sign_up("ana@example.com", headers=key("k-1"))
    assert retry.status == 201 and retry.body == first.body, f"a retry answers as the first time: {retry.status} {retry.body}"
    assert retry.headers.get("idempotent-replayed") == "true", "mark the replay with idempotent-replayed: true"
    assert not {"TransactWriteItems", "PutItem", "UpdateItem", "DeleteItem"} & set(api.operations()), (
        f"a replay writes nothing, sent {api.operations()}")
    assert api.call("GET", "/events/rust-meetup").body["taken"] == 1, "a retry must not take another seat"


def test_a_retry_after_the_event_filled_up_still_succeeds(api, clock):
    api.event(capacity=1)
    api.sign_up("ana@example.com", headers=key("k-1"))
    retry = api.sign_up("ana@example.com", headers=key("k-1"))
    assert retry.status == 201, f"the retry of the sign-up that took the last seat must answer 201, got {retry.status}"


def test_a_retry_after_a_cancel_does_not_sign_up_again(api, clock):
    api.event(capacity=2)
    api.sign_up("ana@example.com", headers=key("k-1"))
    api.call("DELETE", "/events/rust-meetup/signups/ana@example.com")
    retry = api.sign_up("ana@example.com", headers=key("k-1"))
    assert retry.status == 201 and retry.headers.get("idempotent-replayed") == "true", "replay the stored answer"
    assert api.item("EVENT#rust-meetup", "SIGNUP#ana@example.com") is None, (
        "a replayed request must not sign the person up again")
    assert api.call("GET", "/events/rust-meetup").body["taken"] == 0


@pytest.mark.parametrize("email,event_id", [("bo@example.com", "rust-meetup"), ("ana@example.com", "go-meetup")])
def test_a_key_reused_for_another_request_is_422(api, clock, email, event_id):
    api.event()
    api.event("go-meetup")
    api.sign_up("ana@example.com", headers=key("k-1"))
    reused = api.sign_up(email, event_id=event_id, headers=key("k-1"))
    assert reused.status == 422 and reused.body == {"error": "idempotency key reused"}, f"{reused.status} {reused.body}"
    assert api.item(f"EVENT#{event_id}", f"SIGNUP#{email}") is None, "a reused key signs nobody up"


def test_the_same_person_with_a_different_case_is_the_same_request(api, clock):
    api.event()
    api.sign_up("ana@example.com", headers=key("k-1"))
    retry = api.sign_up(" ANA@example.com", headers=key("k-1"))
    assert retry.status == 201, f"the request is compared after normalizing the email, got {retry.status}"


@pytest.mark.parametrize("value", ["", "x" * 65, "has space", "ünïcode", "a/b"])
def test_malformed_keys_are_400(api, clock, value):
    api.event()
    found = api.sign_up("ana@example.com", headers=key(value))
    assert found.status == 400, f"Idempotency-Key {value!r} must answer 400, got {found.status}"
    assert api.call("GET", "/events/rust-meetup").body["taken"] == 0


def test_failures_are_not_stored(api, clock):
    api.event(capacity=1)
    api.sign_up("bo@example.com")
    full = api.sign_up("ana@example.com", headers=key("k-1"))
    assert full.status == 409 and full.body == {"error": "event is full"}
    assert api.item("IDEMPOTENCY#k-1", "KEY") is None, "do not store failed answers"
    api.call("DELETE", "/events/rust-meetup/signups/bo@example.com")
    retry = api.sign_up("ana@example.com", headers=key("k-1"))
    assert retry.status == 201 and retry.headers.get("idempotent-replayed") != "true", (
        "after a failure the same key tries again for real")


def test_an_expired_answer_counts_as_absent_even_before_ttl_deletes_it(api, clock):
    api.event(capacity=3)
    api.sign_up("ana@example.com", headers=key("k-1"))
    clock.time = T0 + DAY + 1
    assert api.item("IDEMPOTENCY#k-1", "KEY") is not None, "(the test expects TTL not to have deleted it yet)"
    reused = api.sign_up("bo@example.com", headers=key("k-1"))
    assert reused.status == 201 and reused.body["email"] == "bo@example.com", (
        f"an expired key is free again, even if the item is still there: {reused.status} {reused.body}")
    stored = api.item("IDEMPOTENCY#k-1", "KEY")
    assert int(stored["expires_at"]["N"]) == int(T0 + DAY + 1) + DAY, "the new answer gets a new expiry"


def test_a_key_is_still_valid_just_before_it_expires(api, clock):
    api.event(capacity=3)
    api.sign_up("ana@example.com", headers=key("k-1"))
    clock.time = T0 + DAY - 1
    reused = api.sign_up("bo@example.com", headers=key("k-1"))
    assert reused.status == 422, f"one second before expiry the key is still taken, got {reused.status}"


def test_without_a_key_nothing_changes(api, clock):
    api.event()
    api.calls.clear()
    api.sign_up("ana@example.com")
    writes = [params for name, params in api.calls if name == "TransactWriteItems"]
    assert len(writes[0]["TransactItems"]) == 2, "without a key the transaction keeps its two operations"
    assert api.items("IDEMPOTENCY#") == []
