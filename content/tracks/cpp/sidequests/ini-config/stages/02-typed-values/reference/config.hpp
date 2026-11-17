#pragma once

#include <charconv>
#include <concepts>
#include <cstddef>
#include <expected>
#include <optional>
#include <string>
#include <string_view>
#include <system_error>
#include <utility>
#include <vector>

namespace dojo {

enum class ErrorKind {
    BadSection,     // a [section] line that is not closed or has a bad name
    BadKey,         // a key that is empty or has a character outside [A-Za-z0-9_.-]
    MissingEquals,  // a line that is not blank, a comment, a section or key = value
    DuplicateKey,   // the same key twice in one section
};

struct ParseError {
    std::size_t line = 0;  // counting from 1
    ErrorKind kind{};

    bool operator==(const ParseError&) const = default;
};

enum class GetError {
    Missing,     // no such section or key
    BadValue,    // the text is not a value of that type
    OutOfRange,  // a number that does not fit the type
};

// The types get<T> can read: bool, std::string, floating-point numbers and integers (but not
// the character types).
template <typename T>
concept ConfigValue =
    std::same_as<T, bool> || std::same_as<T, std::string> || std::floating_point<T> ||
    (std::integral<T> && !std::same_as<T, char> && !std::same_as<T, wchar_t> &&
     !std::same_as<T, char8_t> && !std::same_as<T, char16_t> && !std::same_as<T, char32_t>);

namespace detail {

std::expected<bool, GetError> to_bool(std::string_view text);

template <typename T>
std::expected<T, GetError> to_number(std::string_view text) {
    const char* const first = text.data();
    const char* const last = first + text.size();
    T value{};
    const auto [next, error] = std::from_chars(first, last, value);
    if (error == std::errc::result_out_of_range) {
        return std::unexpected(GetError::OutOfRange);
    }
    if (error != std::errc{} || next != last) {
        return std::unexpected(GetError::BadValue);
    }
    return value;
}

}  // namespace detail

class Config {
public:
    // An INI document: blank lines, comments (# or ;), [section] lines and key = value lines.
    static std::expected<Config, ParseError> parse(std::string_view text);

    // The text of a key ("" is the section of the keys before any [section]).
    std::optional<std::string> raw(std::string_view section, std::string_view key) const;

    // The sections in the order they first appear ("" first, only when it has keys).
    std::vector<std::string> sections() const;

    // The keys of a section, in order.
    std::vector<std::string> keys(std::string_view section) const;

    template <ConfigValue T>
    std::expected<T, GetError> get(std::string_view section, std::string_view key) const {
        const auto text = raw(section, key);
        if (!text) {
            return std::unexpected(GetError::Missing);
        }
        if constexpr (std::same_as<T, std::string>) {
            return *text;
        } else if constexpr (std::same_as<T, bool>) {
            return detail::to_bool(*text);
        } else {
            return detail::to_number<T>(*text);
        }
    }

    // get<T>, or `fallback` when the key is missing or its value does not convert.
    template <ConfigValue T>
    T get_or(std::string_view section, std::string_view key, T fallback) const {
        return get<T>(section, key).value_or(std::move(fallback));
    }

    // Same sections and keys, in the same order, with the same values.
    bool operator==(const Config&) const = default;

private:
    friend class Parser;

    using Entry = std::pair<std::string, std::string>;

    struct Section {
        std::string name;
        std::vector<Entry> entries;

        bool operator==(const Section&) const = default;
    };

    const Section* find(std::string_view name) const;
    Section& open(std::string_view name);

    std::vector<Section> sections_{Section{}};  // sections_[0] is the "" section
};

}  // namespace dojo
