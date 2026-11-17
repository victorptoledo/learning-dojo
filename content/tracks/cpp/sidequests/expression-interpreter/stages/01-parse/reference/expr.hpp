#pragma once

#include <cstddef>
#include <expected>
#include <memory>
#include <string>
#include <string_view>
#include <variant>
#include <vector>

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

struct Expr;

struct Number {
    double value = 0;
    std::size_t position = 0;
};

struct Variable {
    std::string name;
    std::size_t position = 0;
};

struct Unary {
    std::unique_ptr<Expr> operand;  // always the negation
    std::size_t position = 0;
};

struct Binary {
    char op = '+';  // + - * / ^
    std::unique_ptr<Expr> left;
    std::unique_ptr<Expr> right;
    std::size_t position = 0;  // of the operator
};

struct Call {
    std::string name;
    std::vector<Expr> arguments;
    std::size_t position = 0;  // of the name
};

// A node of the syntax tree. The tree owns its children through unique_ptr and vector, so an
// Expr moves but does not copy.
struct Expr {
    std::variant<Number, Variable, Unary, Binary, Call> node;
};

std::expected<Expr, Error> parse(std::string_view text);

// Fully parenthesised: "(1 + (2 * x))", "(-y)", "max(a, b)".
std::string to_string(const Expr& expr);

}  // namespace dojo
