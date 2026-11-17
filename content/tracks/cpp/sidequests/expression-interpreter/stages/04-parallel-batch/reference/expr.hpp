#pragma once

#include <cstddef>
#include <expected>
#include <functional>
#include <map>
#include <memory>
#include <span>
#include <string>
#include <string_view>
#include <variant>
#include <vector>

namespace dojo {

enum class ErrorKind {
    UnexpectedCharacter,  // a character that starts no token
    UnexpectedEnd,        // the text ends where more was needed
    UnexpectedToken,      // a token where it cannot go
    UnknownVariable,
    UnknownFunction,
    WrongArity,      // a function called with the wrong number of arguments
    DivisionByZero,  // x / 0
    NotFinite,       // a result that is infinite or not a number
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

using Function = std::function<double(std::span<const double>)>;

struct Environment {
    std::map<std::string, double, std::less<>> variables;
    std::map<std::string, Function, std::less<>> functions;  // these hide the builtins
};

std::expected<Expr, Error> parse(std::string_view text);

// Fully parenthesised: "(1 + (2 * x))", "(-y)", "max(a, b)".
std::string to_string(const Expr& expr);

std::expected<double, Error> evaluate(const Expr& expr, const Environment& env = {});

// The same expression with constant parts computed and neutral operations removed.
Expr simplify(const Expr& expr);

// The names of the variables the expression reads, sorted, each once.
std::vector<std::string> variables(const Expr& expr);

// Parses and evaluates every formula with `threads` threads (0: one per hardware thread); the
// results are in the order of the formulas.
std::vector<std::expected<double, Error>> evaluate_all(std::span<const std::string> formulas,
                                                       const Environment& env, unsigned threads);

}  // namespace dojo
