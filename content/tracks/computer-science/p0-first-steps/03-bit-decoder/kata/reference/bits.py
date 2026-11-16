"""Bits to numbers and text: two's complement, IEEE 754 binary32 and UTF-8, by hand."""

import math
from dataclasses import dataclass


def encode_twos(value: int, width: int) -> str:
    """The width-bit two's complement pattern of value, as a string of 0 and 1."""
    if width < 1:
        raise ValueError("width must be at least 1")
    low, high = -(1 << (width - 1)), (1 << (width - 1)) - 1
    if not low <= value <= high:
        raise ValueError(f"{value} does not fit in {width} bits")
    pattern = value & ((1 << width) - 1)
    return "".join("1" if pattern >> i & 1 else "0" for i in range(width - 1, -1, -1))


def decode_twos(bits: str) -> int:
    """The signed value of a two's complement pattern; underscores are ignored."""
    digits = bits.replace("_", "")
    if not digits or any(c not in "01" for c in digits):
        raise ValueError(f"not a bit pattern: {bits!r}")
    value = 0
    for c in digits:
        value = value << 1 | (c == "1")
    if digits[0] == "1":
        value -= 1 << len(digits)
    return value


def wrap(value: int, width: int) -> int:
    """What a signed width-bit integer holds after value overflows it."""
    mask = (1 << width) - 1
    pattern = value & mask
    return pattern - (1 << width) if pattern >> (width - 1) else pattern


@dataclass(frozen=True)
class Float32:
    sign: int
    biased_exponent: int
    fraction: int
    kind: str
    exponent: int | None
    value: float


def decode_f32(word: int) -> Float32:
    """Split a 32-bit word into the fields of an IEEE 754 binary32 value."""
    if not 0 <= word <= 0xFFFF_FFFF:
        raise ValueError("a binary32 word has 32 bits")
    sign = word >> 31
    biased = word >> 23 & 0xFF
    fraction = word & 0x7F_FFFF
    factor = -1.0 if sign else 1.0
    if biased == 0xFF:
        if fraction:
            return Float32(sign, biased, fraction, "nan", None, math.nan)
        return Float32(sign, biased, fraction, "infinity", None, factor * math.inf)
    if biased == 0:
        if fraction == 0:
            return Float32(sign, biased, fraction, "zero", None, factor * 0.0)
        value = factor * math.ldexp(fraction, -126 - 23)
        return Float32(sign, biased, fraction, "subnormal", -126, value)
    exponent = biased - 127
    value = factor * math.ldexp((1 << 23) | fraction, exponent - 23)
    return Float32(sign, biased, fraction, "normal", exponent, value)


def utf8_encode(code_point: int) -> bytes:
    """The UTF-8 bytes of one Unicode scalar value."""
    if not 0 <= code_point <= 0x10FFFF or 0xD800 <= code_point <= 0xDFFF:
        raise ValueError(f"U+{code_point:X} is not a Unicode scalar value")
    if code_point < 0x80:
        return bytes([code_point])
    if code_point < 0x800:
        return bytes([0xC0 | code_point >> 6, 0x80 | code_point & 0x3F])
    if code_point < 0x10000:
        return bytes([
            0xE0 | code_point >> 12,
            0x80 | code_point >> 6 & 0x3F,
            0x80 | code_point & 0x3F,
        ])
    return bytes([
        0xF0 | code_point >> 18,
        0x80 | code_point >> 12 & 0x3F,
        0x80 | code_point >> 6 & 0x3F,
        0x80 | code_point & 0x3F,
    ])


# (lead byte mask, lead byte value, number of continuation bytes, smallest code point)
_LEADS = [(0xE0, 0xC0, 1, 0x80), (0xF0, 0xE0, 2, 0x800), (0xF8, 0xF0, 3, 0x10000)]


def utf8_decode(data: bytes) -> list[int]:
    """The code points of UTF-8 data; malformed input raises ValueError with the offset."""
    points: list[int] = []
    i = 0
    while i < len(data):
        lead = data[i]
        if lead < 0x80:
            points.append(lead)
            i += 1
            continue
        for mask, marker, extra, smallest in _LEADS:
            if lead & mask == marker:
                break
        else:
            raise ValueError(f"invalid lead byte 0x{lead:02X} at offset {i}")
        if i + extra >= len(data):
            raise ValueError(f"truncated sequence at offset {i}")
        code_point = lead & (0x7F >> (extra + 1))
        for k in range(1, extra + 1):
            byte = data[i + k]
            if byte & 0xC0 != 0x80:
                raise ValueError(f"invalid continuation byte at offset {i + k}")
            code_point = code_point << 6 | byte & 0x3F
        if code_point < smallest:
            raise ValueError(f"overlong encoding at offset {i}")
        if code_point > 0x10FFFF or 0xD800 <= code_point <= 0xDFFF:
            raise ValueError(f"not a Unicode scalar value at offset {i}")
        points.append(code_point)
        i += extra + 1
    return points
