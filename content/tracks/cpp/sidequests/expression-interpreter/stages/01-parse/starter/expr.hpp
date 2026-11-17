#pragma once

#include <cstddef>
#include <expected>
#include <string>
#include <string_view>

namespace dojo {

enum class ErrorKind {
    UnexpectedCharacter,  // a character that starts no token
    UnexpectedEnd,        // the text ends where more was needed
    UnexpectedToken,      // a token where it cannot go
};

struct Error {
    std::size_t position = 0;  // where in the text, counting from 0
    ErrorKind kind{};

    bool operator==(const Error&) const = default;
};

// A node of the syntax tree: yours to design, around a std::variant of the kinds of node
// (numbers, variables, negations, binary operations, calls).
struct Expr {};

std::expected<Expr, Error> parse(std::string_view text);

// Fully parenthesised: "(1 + (2 * x))", "(-y)", "max(a, b)".
std::string to_string(const Expr& expr);

}  // namespace dojo
