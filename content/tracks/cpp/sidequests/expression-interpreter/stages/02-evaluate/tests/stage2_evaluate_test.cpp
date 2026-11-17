#include <gtest/gtest.h>

#include <cmath>
#include <expected>
#include <numeric>
#include <span>
#include <string_view>

#include "expr.hpp"

using dojo::Environment;
using dojo::Error;
using dojo::ErrorKind;

namespace {

// Parses and evaluates; a parse error is returned as it is.
std::expected<double, Error> calc(std::string_view text, const Environment& env = {}) {
    return dojo::parse(text).and_then(
        [&env](const dojo::Expr& expr) { return dojo::evaluate(expr, env); });
}

std::unexpected<Error> error(std::size_t position, ErrorKind kind) {
    return std::unexpected(Error{.position = position, .kind = kind});
}

Environment sample() {
    Environment env;
    env.variables = {{"x", 3}, {"y", -2}, {"rate", 0.5}};
    return env;
}

}  // namespace

TEST(Stage2Evaluate, Arithmetic) {
    EXPECT_EQ(calc("1 + 2 * 3"), 7.0);
    EXPECT_EQ(calc("(1 + 2) * 3"), 9.0);
    EXPECT_EQ(calc("8 - 3 - 2"), 3.0);
    EXPECT_EQ(calc("7 / 2"), 3.5);
    EXPECT_EQ(calc("2 ^ 3 ^ 2"), 512.0);
    EXPECT_EQ(calc("-2 ^ 2"), -4.0);
    EXPECT_EQ(calc("2 ^ -1"), 0.5);
    EXPECT_EQ(calc("--4"), 4.0);
}

TEST(Stage2Evaluate, Variables) {
    const Environment env = sample();
    EXPECT_EQ(calc("x * x + y", env), 7.0);
    EXPECT_EQ(calc("-y * rate", env), 1.0);
}

TEST(Stage2Evaluate, Builtins) {
    const Environment env = sample();
    EXPECT_EQ(calc("sqrt(16)"), 4.0);
    EXPECT_EQ(calc("abs(y)", env), 2.0);
    EXPECT_EQ(calc("pow(2, 10)"), 1024.0);
    EXPECT_EQ(calc("min(4, x, 9)", env), 3.0);
    EXPECT_EQ(calc("max(y)", env), -2.0);
    EXPECT_EQ(calc("max(1, min(5, 2)) * 10"), 20.0);
}

TEST(Stage2Evaluate, FunctionsOfTheEnvironment) {
    Environment env = sample();
    env.functions["sum"] = [](std::span<const double> values) {
        return std::accumulate(values.begin(), values.end(), 0.0);
    };
    env.functions["sqrt"] = [](std::span<const double>) { return 99.0; };  // hides the builtin
    EXPECT_EQ(calc("sum()", env), 0.0);
    EXPECT_EQ(calc("sum(1, 2, x, 4)", env), 10.0);
    EXPECT_EQ(calc("sqrt(4)", env), 99.0);
    EXPECT_EQ(calc("sqrt(1, 2)", env), 99.0) << "an environment function takes any number";
}

TEST(Stage2Evaluate, UnknownNamesAreReportedWhereTheyStart) {
    const Environment env = sample();
    EXPECT_EQ(calc("x + zeta", env), error(4, ErrorKind::UnknownVariable));
    EXPECT_EQ(calc("2 * nope(x)", env), error(4, ErrorKind::UnknownFunction));
    EXPECT_EQ(calc("nope(zeta)", env), error(0, ErrorKind::UnknownFunction))
        << "the function is looked up before its arguments are evaluated";
}

TEST(Stage2Evaluate, BuiltinsCheckTheirArity) {
    EXPECT_EQ(calc("sqrt()"), error(0, ErrorKind::WrongArity));
    EXPECT_EQ(calc("1 + sqrt(1, 2)"), error(4, ErrorKind::WrongArity));
    EXPECT_EQ(calc("pow(2)"), error(0, ErrorKind::WrongArity));
    EXPECT_EQ(calc("min()"), error(0, ErrorKind::WrongArity));
}

TEST(Stage2Evaluate, DivisionByZeroIsAtTheOperator) {
    EXPECT_EQ(calc("1 + 4 / (2 - 2)"), error(6, ErrorKind::DivisionByZero));
    EXPECT_EQ(calc("0 / 0"), error(2, ErrorKind::DivisionByZero));
}

TEST(Stage2Evaluate, ResultsMustBeFinite) {
    EXPECT_EQ(calc("sqrt(-1)"), error(0, ErrorKind::NotFinite));
    EXPECT_EQ(calc("10 ^ 400"), error(3, ErrorKind::NotFinite));
    EXPECT_EQ(calc("2 * (10 ^ 300 * 10 ^ 300)"), error(14, ErrorKind::NotFinite));
}

TEST(Stage2Evaluate, TheFirstErrorFromTheLeftWins) {
    const Environment env = sample();
    EXPECT_EQ(calc("a + 1 / 0", env), error(0, ErrorKind::UnknownVariable));
    EXPECT_EQ(calc("1 / 0 + a", env), error(2, ErrorKind::DivisionByZero));
    EXPECT_EQ(calc("max(x, b, c)", env), error(7, ErrorKind::UnknownVariable));
}

TEST(Stage2Evaluate, ParseErrorsStayParseErrors) {
    EXPECT_EQ(calc("1 +"), error(3, ErrorKind::UnexpectedEnd));
}
