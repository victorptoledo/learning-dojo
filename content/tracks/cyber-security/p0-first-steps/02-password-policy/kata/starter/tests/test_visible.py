from policy import check_password


def test_a_long_passphrase_without_symbols_is_fine():
    assert check_password("correct horse battery staple") == []


def test_a_short_complex_password_is_too_short():
    assert check_password("Tr0ub4dor&3") == ["too-short"]
