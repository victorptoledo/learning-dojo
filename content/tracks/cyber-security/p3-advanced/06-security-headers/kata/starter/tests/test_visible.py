from headers import check_headers, secure_headers


def test_no_headers_at_all():
    assert check_headers([]) == [
        "hsts-missing",
        "csp-missing",
        "frame-missing",
        "nosniff-missing",
        "referrer-policy-missing",
    ]


def test_the_recommended_set_passes_the_audit():
    assert check_headers(secure_headers("rAnd0mN0nceVa1ue")) == []
