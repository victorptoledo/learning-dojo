import math
import random
import re
import struct
from pathlib import Path

import pytest

from bits import decode_f32, decode_twos, encode_twos, utf8_decode, utf8_encode, wrap

SOURCE = Path(__file__).resolve().parent.parent / "bits.py"


def test_encode_twos_examples():
    assert encode_twos(5, 8) == "00000101"
    assert encode_twos(-1, 8) == "11111111"
    assert encode_twos(-128, 8) == "10000000"
    assert encode_twos(127, 8) == "01111111"
    assert encode_twos(0, 1) == "0"
    assert encode_twos(-1, 1) == "1"
    assert encode_twos(-2, 16) == "1111111111111110"


def test_encode_twos_rejects_values_that_do_not_fit():
    for value, width in [(128, 8), (-129, 8), (1, 1), (2**31, 32)]:
        with pytest.raises(ValueError):
            encode_twos(value, width)
    with pytest.raises(ValueError):
        encode_twos(0, 0)


def test_decode_twos_examples():
    assert decode_twos("0") == 0
    assert decode_twos("1") == -1
    assert decode_twos("01111111") == 127
    assert decode_twos("10000000") == -128
    assert decode_twos("1000_0000_0000_0000_0000_0000_0000_0000") == -(2**31)


def test_decode_twos_rejects_garbage():
    for text in ["", "_", "102", "0b101", " 1"]:
        with pytest.raises(ValueError):
            decode_twos(text)


def test_twos_round_trip_for_random_values():
    rng = random.Random(7)
    for _ in range(500):
        width = rng.randint(2, 64)
        value = rng.randint(-(2 ** (width - 1)), 2 ** (width - 1) - 1)
        bits = encode_twos(value, width)
        assert len(bits) == width
        assert decode_twos(bits) == value
        assert int(bits, 2) == value % 2**width


def test_wrap_matches_fixed_width_overflow():
    assert wrap(2**31, 32) == -(2**31)
    assert wrap(2**31 - 1 + 1, 32) == -(2**31)
    assert wrap(255, 8) == -1
    assert wrap(256, 8) == 0
    assert wrap(3_000_000_000, 32) == -1_294_967_296
    assert wrap(-129, 8) == 127
    assert wrap(42, 32) == 42


def _python_f32(word: int) -> float:
    return struct.unpack(">f", word.to_bytes(4, "big"))[0]


def test_decode_f32_normal_numbers():
    one = decode_f32(0x3F800000)
    assert one == decode_f32(0x3F800000)
    assert (one.sign, one.biased_exponent, one.fraction, one.kind, one.exponent, one.value) == (
        0, 127, 0, "normal", 0, 1.0,
    )
    minus = decode_f32(0xC0490FDB)
    assert minus.sign == 1 and minus.value == _python_f32(0xC0490FDB)
    tenth = decode_f32(0x3DCCCCCD)
    assert tenth.exponent == -4 and tenth.fraction == 0x4CCCCD
    assert tenth.value == _python_f32(0x3DCCCCCD) != 0.1


def test_decode_f32_zero_and_negative_zero():
    zero = decode_f32(0)
    assert (zero.kind, zero.exponent, zero.value) == ("zero", None, 0.0)
    negative = decode_f32(0x80000000)
    assert negative.kind == "zero" and negative.sign == 1
    assert math.copysign(1.0, negative.value) == -1.0


def test_decode_f32_subnormals():
    tiny = decode_f32(0x00000001)
    assert (tiny.kind, tiny.exponent) == ("subnormal", -126)
    assert tiny.value == 2.0**-149
    largest = decode_f32(0x007FFFFF)
    assert largest.kind == "subnormal" and largest.value == _python_f32(0x007FFFFF)
    smallest_normal = decode_f32(0x00800000)
    assert smallest_normal.kind == "normal" and smallest_normal.value == 2.0**-126


def test_decode_f32_infinities_and_nan():
    plus = decode_f32(0x7F800000)
    assert (plus.kind, plus.exponent, plus.value) == ("infinity", None, math.inf)
    assert decode_f32(0xFF800000).value == -math.inf
    for word in [0x7FC00000, 0x7F800001, 0xFFFFFFFF]:
        nan = decode_f32(word)
        assert nan.kind == "nan" and nan.exponent is None and math.isnan(nan.value)


def test_decode_f32_matches_the_hardware_for_random_words():
    rng = random.Random(32)
    for _ in range(3000):
        word = rng.getrandbits(32)
        decoded = decode_f32(word)
        expected = _python_f32(word)
        assert decoded.sign == word >> 31
        assert decoded.biased_exponent == word >> 23 & 0xFF
        assert decoded.fraction == word & 0x7FFFFF
        if math.isnan(expected):
            assert math.isnan(decoded.value)
        else:
            assert decoded.value == expected


def test_decode_f32_rejects_words_out_of_range():
    for word in [-1, 2**32]:
        with pytest.raises(ValueError):
            decode_f32(word)


def test_utf8_encode_each_length():
    assert utf8_encode(0x41) == b"A"
    assert utf8_encode(0) == b"\x00"
    assert utf8_encode(0xE9) == b"\xc3\xa9"
    assert utf8_encode(0x7FF) == b"\xdf\xbf"
    assert utf8_encode(0x800) == b"\xe0\xa0\x80"
    assert utf8_encode(0xFFFF) == b"\xef\xbf\xbf"
    assert utf8_encode(0x1F600) == b"\xf0\x9f\x98\x80"
    assert utf8_encode(0x10FFFF) == b"\xf4\x8f\xbf\xbf"


def test_utf8_encode_rejects_non_scalar_values():
    for code_point in [-1, 0x110000, 0xD800, 0xDFFF]:
        with pytest.raises(ValueError):
            utf8_encode(code_point)


def test_utf8_round_trip_matches_python():
    rng = random.Random(8)
    for _ in range(2000):
        code_point = rng.choice([rng.randint(0, 0x7F), rng.randint(0x80, 0x7FF),
                                 rng.randint(0x800, 0xD7FF), rng.randint(0xE000, 0x10FFFF)])
        expected = chr(code_point).encode("utf-8")
        assert utf8_encode(code_point) == expected
        assert utf8_decode(expected) == [code_point]


def test_utf8_decode_a_mixed_string():
    text = "José paid 5 € 😀"
    assert utf8_decode(text.encode("utf-8")) == [ord(c) for c in text]
    assert utf8_decode(b"") == []


@pytest.mark.parametrize(
    "data",
    [
        b"\x80",               # continuation byte without a lead
        b"\xc3",               # truncated
        b"\xe2\x82",           # truncated
        b"\xc3A",              # bad continuation
        b"\xc0\x80",           # overlong NUL
        b"\xe0\x80\xaf",       # overlong '/'
        b"\xed\xa0\x80",       # a surrogate
        b"\xf4\x90\x80\x80",   # above U+10FFFF
        b"\xff",               # never valid
    ],
)
def test_utf8_decode_rejects_malformed_input(data):
    with pytest.raises(ValueError):
        utf8_decode(b"ok" + data)


def test_utf8_decode_error_names_the_offset():
    with pytest.raises(ValueError, match=r"\b3\b"):
        utf8_decode(b"abc\x80")


def test_no_shortcuts_in_the_solution():
    code = SOURCE.read_text(encoding="utf-8")
    assert not re.search(r"^\s*(import|from)\s+(struct|codecs|ctypes|numpy)\b", code, re.M), (
        "do the bit work yourself: struct, codecs, ctypes and numpy are not allowed"
    )
    assert ".encode(" not in code and ".decode(" not in code, (
        "do not call str.encode or bytes.decode: encode and decode UTF-8 by hand"
    )
    assert "int.from_bytes" not in code and ".to_bytes(" not in code
