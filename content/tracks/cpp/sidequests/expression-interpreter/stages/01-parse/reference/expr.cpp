#include "expr.hpp"

#include <algorithm>
#include <cctype>
#include <charconv>
#include <format>
#include <utility>

namespace dojo {

namespace {

bool is_digit(char c) {
    return std::isdigit(static_cast<unsigned char>(c)) != 0;
}

bool starts_name(char c) {
    return std::isalpha(static_cast<unsigned char>(c)) != 0 || c == '_';
}

bool in_name(char c) {
    return starts_name(c) || is_digit(c);
}

Expr boxed(Binary node) {
    return Expr{std::move(node)};
}

std::unique_ptr<Expr> box(Expr expr) {
    return std::make_unique<Expr>(std::move(expr));
}

// Recursive descent, one function per precedence level:
//   expression := term (('+' | '-') term)*
//   term       := unary (('*' | '/') unary)*
//   unary      := '-' unary | power
//   power      := primary ('^' unary)?
//   primary    := number | name | name '(' arguments ')' | '(' expression ')'
class Parser {
public:
    explicit Parser(std::string_view text) : text_(text) {}

    std::expected<Expr, Error> all() {
        auto expr = expression();
        if (expr && peek() != '\0') {
            return std::unexpected(here());
        }
        return expr;
    }

private:
    // The next character after blanks, or '\0' at the end.
    char peek() {
        while (pos_ < text_.size() && (text_[pos_] == ' ' || text_[pos_] == '\t')) {
            ++pos_;
        }
        return pos_ < text_.size() ? text_[pos_] : '\0';
    }

    // The error for whatever is at the current position.
    Error here() {
        const char c = peek();
        if (c == '\0') {
            return Error{.position = text_.size(), .kind = ErrorKind::UnexpectedEnd};
        }
        const bool token = in_name(c) || std::string_view("+-*/^(),").contains(c);
        return Error{.position = pos_,
                     .kind = token ? ErrorKind::UnexpectedToken : ErrorKind::UnexpectedCharacter};
    }

    std::expected<Expr, Error> expression() {
        auto left = term();
        while (left && (peek() == '+' || peek() == '-')) {
            const char op = text_[pos_];
            const std::size_t position = pos_++;
            auto right = term();
            if (!right) {
                return right;
            }
            left = boxed(Binary{.op = op,
                                .left = box(std::move(*left)),
                                .right = box(std::move(*right)),
                                .position = position});
        }
        return left;
    }

    std::expected<Expr, Error> term() {
        auto left = unary();
        while (left && (peek() == '*' || peek() == '/')) {
            const char op = text_[pos_];
            const std::size_t position = pos_++;
            auto right = unary();
            if (!right) {
                return right;
            }
            left = boxed(Binary{.op = op,
                                .left = box(std::move(*left)),
                                .right = box(std::move(*right)),
                                .position = position});
        }
        return left;
    }

    std::expected<Expr, Error> unary() {
        if (peek() == '-') {
            const std::size_t position = pos_++;
            auto operand = unary();
            if (!operand) {
                return operand;
            }
            return Expr{Unary{.operand = box(std::move(*operand)), .position = position}};
        }
        return power();
    }

    std::expected<Expr, Error> power() {
        auto base = primary();
        if (base && peek() == '^') {
            const std::size_t position = pos_++;
            auto exponent = unary();  // right-associative: 2 ^ 3 ^ 2 is 2 ^ (3 ^ 2)
            if (!exponent) {
                return exponent;
            }
            return boxed(Binary{.op = '^',
                                .left = box(std::move(*base)),
                                .right = box(std::move(*exponent)),
                                .position = position});
        }
        return base;
    }

    std::expected<Expr, Error> primary() {
        const char c = peek();
        if (is_digit(c)) {
            return number();
        }
        if (starts_name(c)) {
            return name();
        }
        if (c == '(') {
            ++pos_;
            auto inner = expression();
            if (inner && peek() != ')') {
                return std::unexpected(here());
            }
            ++pos_;
            return inner;
        }
        return std::unexpected(here());
    }

    std::expected<Expr, Error> number() {
        const std::size_t start = pos_;
        while (pos_ < text_.size() && is_digit(text_[pos_])) {
            ++pos_;
        }
        if (pos_ + 1 < text_.size() && text_[pos_] == '.' && is_digit(text_[pos_ + 1])) {
            ++pos_;
            while (pos_ < text_.size() && is_digit(text_[pos_])) {
                ++pos_;
            }
        }
        const std::string_view digits = text_.substr(start, pos_ - start);
        const char* const first = digits.data();
        double value = 0;
        [[maybe_unused]] const auto read = std::from_chars(first, first + digits.size(), value);
        return Expr{Number{.value = value, .position = start}};
    }

    std::expected<Expr, Error> name() {
        const std::size_t start = pos_;
        while (pos_ < text_.size() && in_name(text_[pos_])) {
            ++pos_;
        }
        std::string id(text_.substr(start, pos_ - start));
        if (peek() != '(') {
            return Expr{Variable{.name = std::move(id), .position = start}};
        }
        ++pos_;
        Call call{.name = std::move(id), .arguments = {}, .position = start};
        if (peek() == ')') {
            ++pos_;
            return Expr{std::move(call)};
        }
        while (true) {
            auto argument = expression();
            if (!argument) {
                return argument;
            }
            call.arguments.push_back(std::move(*argument));
            const char next = peek();
            if (next != ',' && next != ')') {
                return std::unexpected(here());
            }
            ++pos_;
            if (next == ')') {
                return Expr{std::move(call)};
            }
        }
    }

    std::string_view text_;
    std::size_t pos_ = 0;
};

struct Printer {
    std::string operator()(const Expr& expr) const { return std::visit(*this, expr.node); }

    std::string operator()(const Number& number) const { return std::format("{}", number.value); }

    std::string operator()(const Variable& variable) const { return variable.name; }

    std::string operator()(const Unary& unary) const {
        return "(-" + (*this)(*unary.operand) + ")";
    }

    std::string operator()(const Binary& binary) const {
        return std::format("({} {} {})", (*this)(*binary.left), binary.op, (*this)(*binary.right));
    }

    std::string operator()(const Call& call) const {
        std::string text = call.name + "(";
        for (std::size_t i = 0; i < call.arguments.size(); ++i) {
            if (i > 0) {
                text += ", ";
            }
            text += (*this)(call.arguments[i]);
        }
        return text + ")";
    }
};

}  // namespace

std::expected<Expr, Error> parse(std::string_view text) {
    return Parser(text).all();
}

std::string to_string(const Expr& expr) {
    return Printer{}(expr);
}

}  // namespace dojo
