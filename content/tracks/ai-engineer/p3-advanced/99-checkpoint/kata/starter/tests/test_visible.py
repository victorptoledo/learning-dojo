from copilot import backoff, redact


def test_redact_reuses_the_placeholder():
    found = {}
    assert redact("a@x.example, b@y.example, a@x.example", found) == "<EMAIL_1>, <EMAIL_2>, <EMAIL_1>"


def test_backoff_doubles():
    assert [backoff(n) for n in range(3)] == [0.5, 1.0, 2.0]
