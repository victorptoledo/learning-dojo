"""Bits to numbers and text: two's complement, IEEE 754 binary32 and UTF-8, by hand."""

import math  # noqa: F401  (math.ldexp, math.inf and math.nan help in decode_f32)
from dataclasses import dataclass


def encode_twos(value: int, width: int) -> str:
    """The width-bit two's complement pattern of value, as a string of 0 and 1."""
    raise NotImplementedError("todo: write encode_twos()")


def decode_twos(bits: str) -> int:
    """The signed value of a two's complement pattern; underscores are ignored."""
    raise NotImplementedError("todo: write decode_twos()")


def wrap(value: int, width: int) -> int:
    """What a signed width-bit integer holds after value overflows it."""
    raise NotImplementedError("todo: write wrap()")


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
    raise NotImplementedError("todo: write decode_f32()")


def utf8_encode(code_point: int) -> bytes:
    """The UTF-8 bytes of one Unicode scalar value."""
    raise NotImplementedError("todo: write utf8_encode()")


def utf8_decode(data: bytes) -> list[int]:
    """The code points of UTF-8 data; malformed input raises ValueError with the offset."""
    raise NotImplementedError("todo: write utf8_decode()")
