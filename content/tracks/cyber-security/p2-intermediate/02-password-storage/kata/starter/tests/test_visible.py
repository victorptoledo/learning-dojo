from vault import hash_password, verify_password


def test_hashes_use_scrypt_and_verify():
    stored = hash_password("correct horse battery staple")
    assert stored.startswith("scrypt$n=16384,r=8,p=1$")
    assert verify_password("correct horse battery staple", stored)
    assert not verify_password("correct horse battery stapler", stored)


def test_the_same_password_hashes_differently_each_time():
    assert hash_password("same password here") != hash_password("same password here")
