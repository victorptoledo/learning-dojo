Typed values. Add to `config.hpp`:

```cpp
enum class GetError {
    Missing,     // no such section or key
    BadValue,    // the text is not a value of that type
    OutOfRange,  // a number that does not fit the type
};

// The types get<T> can read: bool, std::string, floating-point numbers and integers (but not
// the character types char, wchar_t, char8_t, char16_t and char32_t).
template <typename T>
concept ConfigValue = /* yours */;

// Members of Config:
template <ConfigValue T>
std::expected<T, GetError> get(std::string_view section, std::string_view key) const;

// get<T>, or fallback when the key is missing or its value does not convert.
template <ConfigValue T>
T get_or(std::string_view section, std::string_view key, T fallback) const;
```

- `std::string` is the text itself.
- `bool` is `true`, `yes`, `on` or `1`, and `false`, `no`, `off` or `0`, in any case (`Yes`,
  `OFF`). Anything else is `BadValue`.
- A number is exactly what `std::from_chars` reads for that type, and nothing after it: `8080`
  is an `int`, a `std::uint16_t` and a `double`; `2.5` is not an `int`, `-3` is not an
  `unsigned`, `9x` and an empty value are not numbers. A number that does not fit the type
  (`99999999999` for an `int`, `1e999` for a `double`) is `OutOfRange`.

Templates live in the header. One `get` with `if constexpr` on the type can choose the
conversion; a function template over `std::from_chars` covers every integer and
floating-point type at once. The tests also check the concept itself, such as
`dojo::ConfigValue<std::string_view>` being false.
