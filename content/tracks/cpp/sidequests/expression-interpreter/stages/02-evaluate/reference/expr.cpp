#include "expr.hpp"

#include <algorithm>
#include <cctype>
#include <charconv>
#include <cmath>
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

struct Builtin {
    std::string_view name;
    std::size_t min_arity;
    std::size_t max_arity;
    double (*apply)(std::span<const double>);
};

constexpr std::size_t kAny = static_cast<std::size_t>(-1);

constexpr Builtin kBuiltins[] = {
    {"sqrt", 1, 1, [](std::span<const double> a) { return std::sqrt(a[0]); }},
    {"abs", 1, 1, [](std::span<const double> a) { return std::abs(a[0]); }},
    {"pow", 2, 2, [](std::span<const double> a) { return std::pow(a[0], a[1]); }},
    {"min", 1, kAny, [](std::span<const double> a) { return std::ranges::min(a); }},
    {"max", 1, kAny, [](std::span<const double> a) { return std::ranges::max(a); }},
};

const Builtin* builtin(std::string_view name) {
    const auto found = std::ranges::find(kBuiltins, name, &Builtin::name);
    return found == std::ranges::end(kBuiltins) ? nullptr : &*found;
}

std::expected<double, Error> finite(double value, std::size_t position) {
    if (!std::isfinite(value)) {
        return std::unexpected(Error{.position = position, .kind = ErrorKind::NotFinite});
    }
    return value;
}

double apply(char op, double left, double right) {
    switch (op) {
        case '+':
            return left + right;
        case '-':
            return left - right;
        case '*':
            return left * right;
        case '/':
            return left / right;
        default:
            return std::pow(left, right);
    }
}

// One overload per kind of node, chosen by std::visit.
struct Evaluator {
    const Environment& env;

    std::expected<double, Error> operator()(const Expr& expr) const {
        return std::visit(*this, expr.node);
    }

    std::expected<double, Error> operator()(const Number& number) const { return number.value; }

    std::expected<double, Error> operator()(const Variable& variable) const {
        const auto found = env.variables.find(variable.name);
        if (found == env.variables.end()) {
            return std::unexpected(
                Error{.position = variable.position, .kind = ErrorKind::UnknownVariable});
        }
        return found->second;
    }

    std::expected<double, Error> operator()(const Unary& unary) const {
        return (*this)(*unary.operand).transform([](double x) { return -x; });
    }

    std::expected<double, Error> operator()(const Binary& binary) const {
        const auto left = (*this)(*binary.left);
        if (!left) {
            return left;
        }
        const auto right = (*this)(*binary.right);
        if (!right) {
            return right;
        }
        if (binary.op == '/' && *right == 0.0) {
            return std::unexpected(
                Error{.position = binary.position, .kind = ErrorKind::DivisionByZero});
        }
        return finite(apply(binary.op, *left, *right), binary.position);
    }

    std::expected<double, Error> operator()(const Call& call) const {
        const auto error = [&call](ErrorKind kind) {
            return std::unexpected(Error{.position = call.position, .kind = kind});
        };
        const auto custom = env.functions.find(call.name);
        const Builtin* const known = builtin(call.name);
        if (custom == env.functions.end() && known == nullptr) {
            return error(ErrorKind::UnknownFunction);
        }
        const std::size_t count = call.arguments.size();
        if (custom == env.functions.end() &&
            (count < known->min_arity || count > known->max_arity)) {
            return error(ErrorKind::WrongArity);
        }
        std::vector<double> values;
        for (const auto& argument : call.arguments) {
            const auto value = (*this)(argument);
            if (!value) {
                return value;
            }
            values.push_back(*value);
        }
        const double result =
            custom != env.functions.end() ? custom->second(values) : known->apply(values);
        return finite(result, call.position);
    }
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

std::expected<double, Error> evaluate(const Expr& expr, const Environment& env) {
    return Evaluator{.env = env}(expr);
}

}  // namespace dojo
