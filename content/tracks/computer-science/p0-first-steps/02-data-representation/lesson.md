# Data Representation: Binary, Two's Complement, Floating Point and Text

Memory holds only bits. Whether the 32 bits `0x40490FDB` mean the integer 1,078,530,011, the
float 3.1415927 or the four characters `@I..` depends entirely on the code that reads them. This
lesson shows the standard encodings, and where each one surprises programmers.

## Positional notation, binary and hexadecimal

A number written in base *b* is a sum of digits times powers of *b*:

```text
1011₂ = 1·8 + 0·4 + 1·2 + 1·1 = 11
0x2F  = 2·16 + 15        = 47
```

Hexadecimal is binary in groups of four bits, which is why it is everywhere in debuggers, colour
codes and memory dumps: `0x2F = 0010 1111`. One hex digit is a **nibble**, two are a **byte**.

To convert decimal to binary, divide by 2 repeatedly and read the remainders backwards, or
subtract the largest power of two that fits. *n* bits hold 2ⁿ different values.

## Unsigned and signed integers

With *n* bits, an **unsigned** integer ranges from 0 to 2ⁿ − 1. Arithmetic is done **modulo
2ⁿ**: in 8 bits, 255 + 1 = 0. That wrap-around is called **overflow**.

For signed numbers, every modern machine uses **two's complement**: the top bit has weight
−2ⁿ⁻¹ instead of +2ⁿ⁻¹.

```text
8 bits:  0111 1111 = 127      1000 0000 = −128      1111 1111 = −1
```

- The range is −2ⁿ⁻¹ to 2ⁿ⁻¹ − 1, so `int` (32 bits) goes from −2,147,483,648 to 2,147,483,647.
- To negate: invert every bit and add 1. `5 = 0000 0101`, `−5 = 1111 1011`.
- Addition and subtraction are the **same circuit** for signed and unsigned numbers; only the
  interpretation of the result differs. That is why two's complement won.
- Overflow wraps: `Integer.MAX_VALUE + 1 == Integer.MIN_VALUE` in Java. Rust panics in debug
  builds, C# throws inside `checked`, and in C signed overflow is undefined behaviour.
- `Math.abs(Integer.MIN_VALUE)` is still negative: +2³¹ does not fit.
- **Sign extension**: widening a signed value copies the top bit, so `(byte) 0xFF` widened to an
  `int` is −1 (`0xFFFFFFFF`), not 255. Java's `>>>` shifts in zeros, `>>` copies the sign.

## Byte order

A 32-bit integer occupies four bytes. **Little-endian** machines (x86, most ARM setups) store the
least significant byte first; **big-endian** order ("network byte order") stores the most
significant first.

```text
0x0A0B0C0D   little-endian: 0D 0C 0B 0A     big-endian: 0A 0B 0C 0D
```

Byte order matters whenever bytes leave the process: file formats, network protocols, binary
serialisation. Protocols fix one order; Java's `DataOutputStream` and `ByteBuffer` default to
big-endian.

## Floating point: IEEE 754

Real numbers are stored like scientific notation in base 2: **±1.fraction × 2^exponent**. The
IEEE 754 **binary32** (`float`) layout:

```text
 31  30 ........ 23  22 ............................ 0
 sign  exponent (8)        fraction (23)
```

- **Sign**: 0 positive, 1 negative.
- **Exponent**: stored with a **bias** of 127, so the stored value 127 means 2⁰.
- **Fraction**: the bits after the binary point; the leading `1.` is implicit (not stored) for
  normal numbers, which buys one bit of precision.

Example, `0x40490FDB`: sign 0, exponent `1000 0000` = 128 → 2¹, fraction `0x490FDB` →
1.5707963…, value ≈ 3.1415927. `binary64` (`double`) is the same with 11 exponent bits (bias
1023) and 52 fraction bits.

### Special values

| Exponent bits | Fraction | Meaning |
| --- | --- | --- |
| all 0 | 0 | ±0 (there is a negative zero) |
| all 0 | ≠ 0 | **subnormal**: 0.fraction × 2⁻¹²⁶, fills the gap near zero |
| 1 … 254 | any | normal: 1.fraction × 2^(e − 127) |
| all 1 | 0 | ±infinity (`1.0 / 0.0`) |
| all 1 | ≠ 0 | NaN, not a number (`0.0 / 0.0`); NaN ≠ NaN |

### Rounding, and what it means for you

- A float has about 7 significant decimal digits, a double about 15–16. Above 2²⁴ (16,777,216)
  a float cannot even hold every integer; a double stops at 2⁵³.
- 0.1 is `0.000110011001100…₂`, a repeating fraction, so it is rounded to the nearest
  representable value. `0.1 + 0.2` gives `0.30000000000000004`.
- Every operation rounds (the default is *round to nearest, ties to even*), so **order
  matters**: `(a + b) + c` can differ from `a + (b + c)`. Summing many small numbers into a
  large one loses them; Kahan summation or sorting first helps.
- Compare with a tolerance, never with `==`, and **never store money in binary floating
  point**. Use integer cents or a decimal type.
- NaN compares false with everything, including itself, and poisons any calculation it touches.

## Text: Unicode and UTF-8

**Unicode** assigns a number, a **code point**, to every character: `A` is U+0041, `é` is U+00E9,
`€` is U+20AC, `😀` is U+1F600. An **encoding** turns code points into bytes. **UTF-8**, used by
almost the whole web, is variable length:

| Code points | Bytes | Pattern |
| --- | --- | --- |
| U+0000 – U+007F | 1 | `0xxxxxxx` (plain ASCII) |
| U+0080 – U+07FF | 2 | `110xxxxx 10xxxxxx` |
| U+0800 – U+FFFF | 3 | `1110xxxx 10xxxxxx 10xxxxxx` |
| U+10000 – U+10FFFF | 4 | `11110xxx 10xxxxxx 10xxxxxx 10xxxxxx` |

`é` (U+00E9 = 000 1110 1001) becomes `C3 A9`. Read those two bytes as Latin-1 and you get `Ã©`:
the classic **mojibake** of a missing or wrong encoding declaration.

Java strings, JavaScript strings and C# strings are **UTF-16**: characters outside the first
65,536 code points take two 16-bit units (a *surrogate pair*), so `"😀".length()` is 2. Go
strings are UTF-8 bytes, and Python 3 strings are sequences of code points.

## Summary

- Hexadecimal is shorthand for groups of four bits.
- Signed integers are two's complement: one adder for both, a range of −2ⁿ⁻¹ to 2ⁿ⁻¹ − 1, and
  silent wrap-around in most languages.
- Byte order matters as soon as bytes leave the process.
- IEEE 754 stores sign, biased exponent and fraction; it rounds, has infinities, NaN and
  subnormals, and is the wrong type for money.
- Unicode numbers characters; UTF-8 encodes them in one to four bytes; mojibake is a decoding
  with the wrong encoding.

## Further reading

- Bryant and O'Hallaron, *Computer Systems: A Programmer's Perspective*, 3rd ed., chapter 2 "Representing and Manipulating Information": <https://csapp.cs.cmu.edu/>
- David Goldberg, "What Every Computer Scientist Should Know About Floating-Point Arithmetic" (ACM Computing Surveys, 1991): <https://docs.oracle.com/cd/E19957-01/806-3568/ncg_goldberg.html>
- Petzold, *Code: The Hidden Language of Computer Hardware and Software*, 2nd ed., chapters on binary and hexadecimal
- The Floating-Point Guide: <https://floating-point-gui.de/>
- Joel Spolsky, "The Absolute Minimum Every Software Developer Absolutely, Positively Must Know About Unicode and Character Sets": <https://www.joelonsoftware.com/2003/10/08/the-absolute-minimum-every-software-developer-absolutely-positively-must-know-about-unicode-and-character-sets-no-excuses/>
- The Unicode Standard, chapter 3, UTF-8 (table 3-6): <https://www.unicode.org/versions/latest/>
