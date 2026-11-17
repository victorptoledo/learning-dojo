#pragma once

#include <cstddef>
#include <expected>
#include <optional>
#include <string>
#include <string_view>
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

    // Same sections and keys, in the same order, with the same values.
    bool operator==(const Config&) const = default;

private:
    // Your data members: sections and keys both keep the order they were read in.
};

}  // namespace dojo
