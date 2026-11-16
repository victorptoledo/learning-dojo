from tokens import TokenService

KEY = b"k" * 32


def service(now=1_794_700_000):
    return TokenService({"2026-11": KEY}, "2026-11", issuer="https://auth.dojo.test", clock=lambda: now)


def test_a_fresh_token_verifies():
    claims = service().verify(service().issue("ada", "orders-api"), "orders-api")
    assert claims["sub"] == "ada"


def test_a_token_for_another_api_is_refused():
    assert service().verify(service().issue("ada", "billing-api"), "orders-api") is None
