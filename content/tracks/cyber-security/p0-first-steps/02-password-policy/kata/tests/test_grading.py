from policy import MAX_LENGTH, check_password, is_acceptable


def test_no_composition_rules():
    assert check_password("all lower case words here") == []
    assert check_password("ALLUPPERCASEWORDS") == []
    assert check_password("000011112222333344") == []


def test_length_boundaries():
    assert check_password("a" * 13 + "bc") == []  # 15
    assert check_password("a" * 12 + "bc") == ["too-short"]  # 14
    assert check_password("ab" * 64) == []  # 128
    assert check_password("ab" * 64 + "c") == ["too-long"]  # 129


def test_mfa_lowers_the_minimum_to_eight():
    assert check_password("blue-sky", mfa=True) == []
    assert check_password("short1!", mfa=True) == ["too-short"]
    assert check_password("blue-sky") == ["too-short"]


def test_spaces_count_and_are_not_stripped():
    assert check_password("  a b c d e f g ") == []
    assert check_password("      abc     ") == ["too-short"]


def test_unicode_counts_code_points_after_nfkc():
    # 15 accented letters written as one code point each
    assert check_password("é" * 14 + "à") == []
    # "e" + combining acute accent is composed to one character by NFKC: 14 characters
    assert check_password("é" * 12 + "ab") == ["too-short"]
    assert check_password("ｐａｓｓｗｏｒｄ", blocklist={"password"}) == ["too-short", "common"]


def test_blocklist_ignores_case_and_uses_the_default():
    assert check_password("Password123456789") == ["common"]
    assert check_password("WELCOME12345678") == ["common"]
    assert check_password("my own long phrase", blocklist={"MY OWN LONG PHRASE"}) == ["common"]
    assert check_password("password123456789", blocklist=set()) == []


def test_username_inside_the_password():
    assert check_password("ada-lovelace-rules-ok", username="Ada") == ["contains-username"]
    assert check_password("the ADMIN account rocks", username="admin") == ["contains-username"]


def test_short_usernames_are_ignored():
    assert check_password("ab and some more words", username="ab") == []
    assert check_password("a long passphrase here", username="") == []


def test_repetitive():
    assert check_password("a" * 15) == ["repetitive"]
    assert check_password("aaaaaaa", mfa=True) == ["too-short", "repetitive"]


def test_several_problems_in_order():
    assert check_password("adaada", username="ada", blocklist={"adaada"}) == [
        "too-short",
        "common",
        "contains-username",
    ]
    assert check_password("") == ["too-short"]
    assert check_password("x" * (MAX_LENGTH + 1)) == ["too-long", "repetitive"]


def test_is_acceptable():
    assert is_acceptable("correct horse battery staple")
    assert not is_acceptable("letmein-letmein")
    assert is_acceptable("blue-sky", mfa=True)
    assert not is_acceptable("my name is grace", username="grace")
