import pytest
from botocore.exceptions import ClientError
from conftest import TABLE, balance, event, record

import handler
from handler import handle, parse_payment


def pay(payment_id, account="acc-7", amount=100):
    return {"payment_id": payment_id, "account": account, "amount_cents": amount}


def failed_ids(result):
    assert set(result) == {"batchItemFailures"}, "return {'batchItemFailures': [...]}"
    return [failure["itemIdentifier"] for failure in result["batchItemFailures"]]


def test_applies_every_payment_of_a_batch(client):
    result = handle(event(record("m-1", pay("p-1", amount=100)),
                          record("m-2", pay("p-2", amount=250)),
                          record("m-3", pay("p-3", account="acc-9", amount=5))), None)
    assert failed_ids(result) == []
    assert balance(client, "acc-7") == 350
    assert balance(client, "acc-9") == 5


def test_a_redelivered_message_is_applied_only_once(client):
    handle(event(record("m-1", pay("p-1"))), None)
    result = handle(event(record("m-1-again", pay("p-1"))), None)
    assert failed_ids(result) == [], "a duplicate is not a failure: it was already applied"
    assert balance(client, "acc-7") == 100, "SQS delivers at least once: apply each payment_id once"


def test_duplicates_inside_one_batch_are_applied_once(client):
    result = handle(event(record("m-1", pay("p-1")), record("m-2", pay("p-1"))), None)
    assert failed_ids(result) == []
    assert balance(client, "acc-7") == 100


def test_records_the_payment_marker(client):
    handle(event(record("m-1", pay("p-1", amount=40))), None)
    item = client.get_item(TableName=TABLE, Key={"pk": {"S": "PAYMENT#p-1"}}).get("Item")
    assert item is not None, "store a marker item with pk PAYMENT#<payment_id>"


@pytest.mark.parametrize("body", [
    "not json",
    "[1, 2]",
    {"account": "acc-7", "amount_cents": 100},
    {"payment_id": "p-9", "amount_cents": 100},
    {"payment_id": "p-9", "account": "acc-7", "amount_cents": -5},
    {"payment_id": "p-9", "account": "acc-7", "amount_cents": 0},
    {"payment_id": "p-9", "account": "acc-7", "amount_cents": "100"},
    {"payment_id": "p-9", "account": "acc-7", "amount_cents": 12.5},
    {"payment_id": "p-9", "account": "acc-7", "amount_cents": True},
], ids=["text", "array", "no-id", "no-account", "negative", "zero", "string-amount", "float-amount",
        "bool-amount"])
def test_parse_payment_rejects_invalid_bodies(body):
    text = body if isinstance(body, str) else __import__("json").dumps(body)
    with pytest.raises(ValueError):
        parse_payment(text)


def test_an_invalid_message_fails_alone(client):
    result = handle(event(record("m-1", pay("p-1")), record("m-bad", "not json"),
                          record("m-3", pay("p-3"))), None)
    assert failed_ids(result) == ["m-bad"], "report only the bad message; the others succeed"
    assert balance(client, "acc-7") == 200


def test_a_transient_error_fails_only_that_message(client, monkeypatch):
    real = client.transact_write_items

    def flaky(**request):
        marker = request["TransactItems"][0]["Put"]["Item"]["pk"]["S"]
        if marker == "PAYMENT#p-2":
            raise ClientError({"Error": {"Code": "ThrottlingException", "Message": "slow down"}},
                              "TransactWriteItems")
        return real(**request)

    monkeypatch.setattr(client, "transact_write_items", flaky)
    result = handle(event(record("m-1", pay("p-1")), record("m-2", pay("p-2")),
                          record("m-3", pay("p-3"))), None)
    assert failed_ids(result) == ["m-2"], "a throttled write must be retried: report it as failed"
    assert balance(client, "acc-7") == 200


def test_marker_and_balance_are_written_in_one_transaction(client, monkeypatch):
    calls = []
    real = client.transact_write_items

    def spy(**request):
        calls.append(request)
        return real(**request)

    monkeypatch.setattr(client, "transact_write_items", spy)
    for name in ("put_item", "update_item"):
        monkeypatch.setattr(client, name, lambda **_: pytest.fail(
            "write the marker and the balance with one transact_write_items call"))
    handle(event(record("m-1", pay("p-1"))), None)
    assert len(calls) == 1 and len(calls[0]["TransactItems"]) == 2


def test_reuses_the_client_between_invocations(client):
    first = handler.dynamodb()
    handle(event(record("m-1", pay("p-1"))), None)
    assert handler.dynamodb() is first, "create the boto3 client once, outside the handler"
