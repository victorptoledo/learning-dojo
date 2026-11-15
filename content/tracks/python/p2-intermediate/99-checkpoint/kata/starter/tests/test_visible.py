from resilience import retry


def test_retries_until_the_call_succeeds():
    calls = []

    @retry(times=3, sleep=lambda seconds: None)
    def flaky():
        calls.append(1)
        if len(calls) < 3:
            raise ConnectionError("try again")
        return "ok"

    assert flaky() == "ok"
    assert len(calls) == 3
