# Formatting and Parsing Text: std::format, std::print and std::from_chars

Most programs end up turning numbers into text and text into numbers: logs, reports, config
files, network protocols. C++ had two old ways, `printf` (fast, unsafe) and iostreams (safe,
verbose, stateful), and both depend on the global locale. C++20 and C++23 replaced them with
`std::format`, `std::print` and the `<charconv>` functions.

## std::format and std::print

```cpp
#include <format>
#include <print>

std::string line = std::format("{} owes {:.2f} EUR", name, total);  // "Ana owes 12.50 EUR"
std::println("{:>8} | {:<10} | {:^5}", 42, "left", "mid");          // print + newline to stdout
std::print(stderr, "warning: {}\n", message);                       // to a FILE*
```

`{}` takes the next argument; `{0}` and `{1}` pick by position. Every argument's type is
known, so there is no `%d` versus `%ld` to get wrong.

## The format mini-language

After the colon comes `[[fill]align][sign][#][0][width][.precision][type]`:

| Spec | Gives | Note |
| --- | --- | --- |
| `{:>8}`, `{:<8}`, `{:^8}` | right, left, centred in 8 columns | numbers default to right, text to left |
| `{:*^9}` with `"ab"` | `***ab****` | the fill character comes before the alignment |
| `{:08.3f}` with `3.14159` | `0003.142` | zero padding, 3 decimals, fixed |
| `{:+}` with `5` | `+5` | always show the sign |
| `{:x}`, `{:#x}`, `{:b}` | `ff`, `0xff`, `1010` | bases; `#` adds the prefix |
| `{:e}`, `{:g}` with `1500.0` | `1.500000e+03`, `1500` | scientific (6 decimals by default), general |
| `{}` with `0.1` | `0.1` | the **shortest** text that reads back as the same double |
| `{:{}}` | width from the next argument | nested replacement fields |

Because `{}` for floating point is the shortest round-trip form, `std::format("{}", 0.1)` is
`"0.1"`, not `"0.10000000000000001"`. Ranges and tuples format too (C++23):
`std::format("{}", std::vector{1, 2})` is `"[1, 2]"`.

## Checked at compile time

The first parameter of `std::format` is a `std::format_string<Args...>`, built by a
`consteval` constructor: the compiler parses the literal and checks every field against the
argument types. `std::format("{:d}", "text")` and `std::format("{} {}", 1)` **do not
compile**.

A format string known only at run time (from a translation file, say) goes through
`std::vformat(fmt, std::make_format_args(args...))`, which throws `std::format_error` on a
bad string. Use it only when you have to.

## Into a buffer, without surprises

```cpp
std::string out;
std::format_to(std::back_inserter(out), "{}={};", key, value);   // appends through any output iterator

std::array<char, 16> buf{};
auto result = std::format_to_n(buf.data(), buf.size() - 1, "{}", long_name);
// result.out is where writing stopped; result.size is the length the whole text needs
std::size_t needed = std::formatted_size("{:>8.2f}", total);
```

`format_to_n` never writes past its limit and tells you how much space the whole text
needed: the safe replacement for `snprintf`, and the tool for C interfaces that hand you a
buffer.

## Your own types: std::formatter

```cpp
struct Money {
    std::int64_t cents;
    std::string_view currency;
};

template <>
struct std::formatter<Money> {
    bool symbol = false;  // our own option: {:s} prints "€12.50" instead of "12.50 EUR"

    constexpr auto parse(std::format_parse_context& ctx) {
        auto it = ctx.begin();
        if (it != ctx.end() && *it == 's') {
            symbol = true;
            ++it;
        }
        if (it != ctx.end() && *it != '}') {
            throw std::format_error("Money: unknown format option");
        }
        return it;  // points at the closing '}'
    }

    auto format(const Money& m, std::format_context& ctx) const {
        const auto units = m.cents / 100;
        const auto rest = std::abs(m.cents % 100);
        if (symbol && m.currency == "EUR") {
            return std::format_to(ctx.out(), "€{}.{:02}", units, rest);
        }
        return std::format_to(ctx.out(), "{}.{:02} {}", units, rest, m.currency);
    }
};

std::println("{} / {:s}", Money{1250, "EUR"}, Money{1250, "EUR"});  // 12.50 EUR / €12.50
```

- `parse` reads the options between `:` and `}` and returns an iterator to the `}`. It is
  `constexpr`, so an unknown option in a literal format string is a **compile** error: the
  `throw` happens during constant evaluation.
- `format` writes through `ctx.out()` and is `const`.
- To reuse the standard options (width, fill) for a type that is "really" a number or a
  string, inherit: `struct std::formatter<UserId> : std::formatter<int>` and call the base
  `format` with the inner value.

## Parsing numbers: std::from_chars

```cpp
#include <charconv>

std::optional<int> parse_int(std::string_view text) {
    int value = 0;
    const auto [ptr, ec] = std::from_chars(text.data(), text.data() + text.size(), value);
    if (ec != std::errc{} || ptr != text.data() + text.size()) {
        return std::nullopt;  // no digits, out of range, or junk after the number
    }
    return value;
}
```

`from_chars` (integers, and floating point in GCC 11 and later) is the parser you want:

- **No locale.** `"1.5"` is one and a half on every machine. `std::stod` and `strtod` use the
  C locale, so with a German locale they read `"1.5"` as `1`.
- **No allocation, no exceptions, no whitespace skipping.** It reports
  `std::errc::invalid_argument` (no digits) and `std::errc::result_out_of_range` (too big),
  and returns where it stopped.
- **"The whole text" is your call.** `from_chars` happily stops after `"12"` in `"12abc"`;
  checking `ptr` against the end is your job. `std::stoi("12abc")` silently returns 12.
- No leading `+` and no `0x` prefix: pass a base (`std::from_chars(first, last, v, 16)`) for hex.

`std::to_chars` goes the other way, also locale-free and round-trip exact. `std::format` uses
it underneath.

## Streams still have a place

`std::istringstream` is fine for quick whitespace-separated reading in tools and tests, and
`std::getline` is still the easy way to read lines (next lesson). But streams carry state
(`std::hex` and `std::setprecision` stick to the stream until reset), depend on the stream's
locale and are slower. For output prefer `std::format` and `std::print`; for numbers,
`from_chars`.

## Summary

- `std::format` and `std::print` are type-safe, and the format string is checked at compile time.
- The mini-language covers width, fill, alignment, sign, precision and bases; `{}` for a
  double is the shortest round-trip text.
- `format_to_n` and `formatted_size` write safely into fixed buffers.
- A `std::formatter<T>` specialisation has a `constexpr parse` for its options and a `const format`.
- `std::from_chars` parses without locale, allocation or exceptions; check `ec` **and** `ptr`.

## Further reading

- cppreference, std::format: <https://en.cppreference.com/w/cpp/utility/format/format>
- cppreference, Standard format specification: <https://en.cppreference.com/w/cpp/utility/format/spec>
- cppreference, std::formatter: <https://en.cppreference.com/w/cpp/utility/format/formatter>
- cppreference, std::from_chars: <https://en.cppreference.com/w/cpp/utility/from_chars>
- Victor Zverovich, {fmt}, the library std::format came from: <https://fmt.dev>
