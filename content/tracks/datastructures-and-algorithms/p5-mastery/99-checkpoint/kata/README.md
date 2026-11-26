# Huffman Archive

The final exam of the track is a small compressor: the core of DEFLATE (zip, gzip, PNG) without
the LZ77 half. You write `Huffman`, which turns bytes into an archive of **canonical Huffman
codes** and back. It needs a greedy algorithm with a proof behind it (Huffman's merge), a heap,
a tree walked without recursion, sorting by two keys, and bits packed and read with shifts.

```java
byte[] text = "abracadabra".getBytes(StandardCharsets.US_ASCII);
Huffman.Archive archive = Huffman.encode(text);
archive.bitCount();          // 23 bits instead of 88
archive.lengths()['a'];      // 1   (a is the most frequent byte)
Huffman.decode(archive);     // the 11 bytes again
```

## 1. Code lengths: `codeLengths(frequencies)`

`frequencies` has 256 entries, one per byte value. Build the Huffman tree greedily: start with one
tree per byte value whose frequency is above 0 and repeatedly merge the **two lightest trees**.
To make the result unique, compare trees by total frequency and then by the **smallest byte value
they contain** (the smaller one is lighter). A byte's code length is the depth of its leaf.

- Unused byte values get length 0; if only one value is used, it gets length 1.
- `null`, an array that does not have 256 entries, or a negative frequency throws
  `IllegalArgumentException`.

## 2. Canonical codes: `canonicalCodes(lengths)`

The archive stores only the 256 lengths, so the codes must follow from them. Sort the used byte
values by **(length, value)**; the first gets code 0; each next one gets the previous code plus 1,
shifted left by however much the length grew. Codes are returned right-aligned in a `long` (the
code `110` is `6`), with 0 for unused values. This is the scheme of RFC 1951:

| byte | A | B | C | D | E | F | G | H |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| length | 3 | 3 | 3 | 3 | 3 | 2 | 4 | 4 |
| code | 010 | 011 | 100 | 101 | 110 | 00 | 1110 | 1111 |

Lengths that cannot form a prefix code (three values of length 1), a length below 0 or above
`MAX_LENGTH` (62), or an array that does not have 256 entries throw `IllegalArgumentException`. An
incomplete code (one value of length 3) is allowed.

## 3. Encoding: `encode(data)`

Write the code of each byte, one after the other, **most significant bit first**: bit 0 of the
output is the top bit (`0x80`) of `bits[0]`. `bitCount` is the number of bits written; the last
byte is padded with zero bits; `bits` has exactly `(bitCount + 7) / 8` bytes. The empty input
gives no lengths, no bits and a bit count of 0.

## 4. Decoding: `decode(archive)`

Read `bitCount` bits and turn them back into bytes. A damaged archive throws
`IllegalArgumentException`: lengths that are invalid as above, a negative `bitCount` or one larger
than the bits it comes with, a run of bits that matches no code, or bits that end in the middle of
a code.

`Huffman.Archive` is given: it copies its arrays on the way in and out.

## How it is graded

This is the final exam 🥋. The hidden tests check the lengths against a slow Huffman with the same
tie rule on 400 frequency tables (and 256 equal frequencies, which must give 8 bits each), the
canonical codes against RFC 1951 and for being prefix-free, that every archive uses exactly the
optimal number of bits, round trips of 300 random inputs and of the edges (empty, one value, all
256 values), Fibonacci frequencies (a 44-level tree), damaged archives, and then **40 MB** each
way within 4 seconds: a `String` of `'0'` and `'1'`, or a map lookup per bit, does not make it.

> Tip: the canonical scheme gives every length a contiguous range of codes. When decoding, keep
> for each length the first code and how many codes it has: after reading each bit, the current
> value either falls in the range of its length (you have a byte) or you read one more bit.
