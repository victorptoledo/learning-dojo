from guardrails import luhn_valid, redact


def test_luhn():
    assert luhn_valid("4111111111111111")
    assert not luhn_valid("4111111111111112")


def test_redacts_an_email():
    assert redact("Write to ana@example.com").text == "Write to <EMAIL_1>"
