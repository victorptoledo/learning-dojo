import pytest


def test_an_event_is_created_and_read_back(api):
    created = api.call("POST", "/events", {"id": "rust-meetup", "title": "Rust meetup", "capacity": 40})
    expected = {"id": "rust-meetup", "title": "Rust meetup", "capacity": 40, "taken": 0, "remaining": 40}
    assert created.status == 201 and created.body == expected, f"POST /events: {created.status} {created.body}"
    assert created.headers.get("content-type") == "application/json", "answer with content-type application/json"
    found = api.call("GET", "/events/rust-meetup")
    assert found.status == 200 and found.body == expected, f"GET /events/rust-meetup: {found.status} {found.body}"
    assert api.item("EVENT#rust-meetup", "META") is not None, "store the event as EVENT#<id> / META"


def test_an_event_id_is_never_taken_twice(api):
    api.event()
    api.calls.clear()
    again = api.call("POST", "/events", {"id": "rust-meetup", "title": "Another", "capacity": 5})
    assert again.status == 409 and again.body == {"error": "event exists"}, f"second create: {again.status} {again.body}"
    assert api.operations() == ["PutItem"], (
        f"create with one conditional put_item, no read first; sent {api.operations()}")
    assert "ConditionExpression" in api.calls[0][1], "the put needs a ConditionExpression"
    assert api.call("GET", "/events/rust-meetup").body["title"] == "Rust meetup", "the first event must be kept"


def test_an_unknown_event_is_404(api):
    found = api.call("GET", "/events/nope")
    assert found.status == 404 and found.body == {"error": "no such event"}, f"{found.status} {found.body}"


def test_an_unknown_route_is_404(api):
    found = api.call("PATCH", "/events/rust-meetup")
    assert found.status == 404 and found.body == {"error": "no such route"}, f"{found.status} {found.body}"


@pytest.mark.parametrize("body", [
    {"id": "ab", "title": "Too short", "capacity": 5},
    {"id": "Rust_Meetup", "title": "Bad characters", "capacity": 5},
    {"id": "x" * 41, "title": "Too long", "capacity": 5},
    {"id": "rust-meetup", "title": "", "capacity": 5},
    {"id": "rust-meetup", "title": "   ", "capacity": 5},
    {"id": "rust-meetup", "title": "t" * 201, "capacity": 5},
    {"id": "rust-meetup", "capacity": 5},
    {"id": "rust-meetup", "title": "Zero seats", "capacity": 0},
    {"id": "rust-meetup", "title": "Too many", "capacity": 10_001},
    {"id": "rust-meetup", "title": "Not a number", "capacity": "40"},
    {"id": "rust-meetup", "title": "A boolean", "capacity": True},
    {"id": "rust-meetup", "title": "A float", "capacity": 4.5},
    ["not", "an", "object"],
])
def test_invalid_events_are_refused(api, body):
    found = api.call("POST", "/events", body)
    assert found.status == 400 and "error" in (found.body or {}), f"{body} must be refused with 400, got {found.status}"
    assert api.items() == [], "nothing may be stored for an invalid request"


def test_a_body_that_is_not_json_is_400(api):
    found = api.call("POST", "/events", raw="{oops")
    assert found.status == 400, f"invalid JSON must answer 400, got {found.status}"


def test_signing_up_takes_a_seat(api):
    api.event(capacity=3)
    found = api.sign_up("  Ana@Example.COM ")
    assert found.status == 201 and found.body == {"event": "rust-meetup", "email": "ana@example.com"}, (
        f"sign-up: {found.status} {found.body}")
    event = api.call("GET", "/events/rust-meetup").body
    assert event["taken"] == 1 and event["remaining"] == 2, f"one seat taken: {event}"
    assert api.item("EVENT#rust-meetup", "SIGNUP#ana@example.com") is not None, (
        "store the sign-up as EVENT#<id> / SIGNUP#<email>")


def test_the_sign_up_is_one_transaction(api):
    api.event()
    api.calls.clear()
    api.sign_up("ana@example.com")
    assert api.operations() == ["TransactWriteItems"], (
        f"sign up with exactly one transact_write_items call, no read first; sent {api.operations()}")
    operations = api.calls[0][1]["TransactItems"]
    assert len(operations) == 2, "the transaction holds the seat update and the sign-up put"


def test_nobody_signs_up_twice(api):
    api.event(capacity=3)
    api.sign_up("ana@example.com")
    again = api.sign_up("ANA@example.com")
    assert again.status == 409 and again.body == {"error": "already signed up"}, f"{again.status} {again.body}"
    assert api.call("GET", "/events/rust-meetup").body["taken"] == 1, "a refused sign-up must not take a seat"


def test_a_full_event_refuses_and_stores_nothing(api):
    api.event(capacity=2)
    assert api.sign_up("ana@example.com").status == 201
    assert api.sign_up("bo@example.com").status == 201
    full = api.sign_up("cy@example.com")
    assert full.status == 409 and full.body == {"error": "event is full"}, f"third sign-up: {full.status} {full.body}"
    assert api.item("EVENT#rust-meetup", "SIGNUP#cy@example.com") is None, (
        "a refused sign-up must leave no item: both writes happen or neither")
    assert api.call("GET", "/events/rust-meetup").body["taken"] == 2


def test_the_seat_counter_is_the_truth(api):
    """Seats taken by other requests a moment ago are only visible in the counter."""
    api.event(capacity=5)
    api.table.update_item(TableName="Signups", Key={"pk": {"S": "EVENT#rust-meetup"}, "sk": {"S": "META"}},
                          UpdateExpression="SET taken = :five", ExpressionAttributeValues={":five": {"N": "5"}})
    full = api.sign_up("ana@example.com")
    assert full.status == 409 and full.body == {"error": "event is full"}, (
        f"taken = capacity means full, whatever else the table holds: {full.status} {full.body}")


def test_signing_up_for_an_unknown_event_is_404(api):
    found = api.sign_up("ana@example.com", event_id="nope")
    assert found.status == 404 and found.body == {"error": "no such event"}, f"{found.status} {found.body}"
    assert api.items() == [], "no sign-up may be stored for an event that does not exist"


@pytest.mark.parametrize("email", ["ana", "ana@example", "ana @example.com", "", None, 42])
def test_bad_emails_are_400(api, email):
    api.event()
    found = api.sign_up(email)
    assert found.status == 400, f"email {email!r} must answer 400, got {found.status}"


def test_the_table_is_never_scanned(api):
    api.event()
    api.sign_up("ana@example.com")
    api.call("GET", "/events/rust-meetup")
    assert "Scan" not in api.operations(), "never scan the table"
