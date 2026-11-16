from bits import decode_f32, decode_twos, encode_twos, utf8_encode


def test_minus_five_in_eight_bits():
    assert encode_twos(-5, 8) == "11111011"
    assert decode_twos("1111_1011") == -5


def test_pi_as_a_float():
    pi = decode_f32(0x40490FDB)
    assert (pi.sign, pi.biased_exponent, pi.exponent, pi.kind) == (0, 128, 1, "normal")
    assert abs(pi.value - 3.1415927) < 1e-6


def test_euro_sign_takes_three_bytes():
    assert utf8_encode(0x20AC) == b"\xe2\x82\xac"
