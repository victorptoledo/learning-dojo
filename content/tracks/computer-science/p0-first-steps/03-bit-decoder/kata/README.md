# Bit Decoder

Write the encodings of the previous lesson by hand, with nothing but integers, shifts and masks.
Everything goes in `bits.py`.

## Two's complement

| Function | Returns |
| --- | --- |
| `encode_twos(value, width) -> str` | the `width`-bit two's complement pattern of `value` as `"0"`/`"1"` characters, most significant bit first. `ValueError` if `value` does not fit (the range is −2ʷ⁻¹ to 2ʷ⁻¹ − 1) or `width < 1` |
| `decode_twos(bits) -> int` | the signed value of a pattern; `_` separators are ignored (`"1111_1011"` is −5). `ValueError` for an empty pattern or any other character |
| `wrap(value, width) -> int` | what a signed `width`-bit integer holds after `value` overflows it: `wrap(2**31, 32) == -2**31`, `wrap(255, 8) == -1` |

## IEEE 754 binary32

`decode_f32(word) -> Float32` splits a 32-bit word (0 to 2³² − 1, otherwise `ValueError`) into
the dataclass already declared in the starter:

| Field | Meaning |
| --- | --- |
| `sign` | bit 31 |
| `biased_exponent` | bits 30–23, as stored |
| `fraction` | bits 22–0, as stored |
| `kind` | `"zero"`, `"subnormal"`, `"normal"`, `"infinity"` or `"nan"` |
| `exponent` | the real power of two: `biased − 127` for normal numbers, `−126` for subnormals, `None` otherwise |
| `value` | the number as a Python `float` (exact: every binary32 is a binary64); `math.nan` for NaN, signed zero and infinity kept |

```python
decode_f32(0x40490FDB)
# Float32(sign=0, biased_exponent=128, fraction=4788187, kind='normal', exponent=1, value=3.1415927410125732)
```

## UTF-8

| Function | Returns |
| --- | --- |
| `utf8_encode(code_point) -> bytes` | the one to four UTF-8 bytes of a code point. `ValueError` for negative values, values above U+10FFFF and surrogates (U+D800–U+DFFF) |
| `utf8_decode(data) -> list[int]` | the code points of `data`. `ValueError` for malformed input, with the byte **offset** of the problem in the message: a stray continuation byte, a truncated sequence, a bad continuation byte, an **overlong** encoding (a code point encoded with more bytes than it needs, such as `C0 80` for U+0000), a surrogate, or a value above U+10FFFF |

## Rules and tests

Do the bit work yourself: the hidden tests reject `struct`, `codecs`, `ctypes`, `numpy`,
`str.encode`, `bytes.decode`, `int.from_bytes` and `int.to_bytes` in `bits.py`. `math.ldexp(m, e)`
(m × 2ᵉ) is allowed and is the easiest way to build the float value.

Run the visible tests with `uv run pytest`. The hidden tests compare `decode_f32` with the
hardware on thousands of random words and `utf8_encode`/`utf8_decode` with Python's own codec,
and ruff checks `bits.py`.
