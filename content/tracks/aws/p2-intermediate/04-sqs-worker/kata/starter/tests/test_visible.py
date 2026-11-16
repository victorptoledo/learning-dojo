from conftest import balance, event, record

from handler import handle


def test_applies_one_payment(client):
    result = handle(event(record("m-1", {"payment_id": "p-1", "account": "acc-7", "amount_cents": 1250})),
                    None)
    assert result == {"batchItemFailures": []}
    assert balance(client, "acc-7") == 1250
