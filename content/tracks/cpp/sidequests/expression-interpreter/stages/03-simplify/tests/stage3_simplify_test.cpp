#include <gtest/gtest.h>

#include <string>
#include <string_view>
#include <vector>

#include "expr.hpp"

namespace {

// The parenthesised tree of the simplified expression.
std::string simplified(std::string_view text) {
    const auto expr = dojo::parse(text);
    if (!expr) {
        return "parse error at " + std::to_string(expr.error().position);
    }
    return dojo::to_string(dojo::simplify(*expr));
}

std::vector<std::string> names(std::string_view text) {
    const auto expr = dojo::parse(text);
    return expr ? dojo::variables(*expr) : std::vector<std::string>{"parse error"};
}

}  // namespace

TEST(Stage3Simplify, FoldsConstantParts) {
    EXPECT_EQ(simplified("1 + 2 * 3"), "7");
    EXPECT_EQ(simplified("x * (2 + 3)"), "(x * 5)");
    EXPECT_EQ(simplified("-(4 - 6)"), "2");
    EXPECT_EQ(simplified("2 ^ 3 + y"), "(8 + y)");
}

TEST(Stage3Simplify, FoldsBuiltinCallsOfConstants) {
    EXPECT_EQ(simplified("sqrt(9) + x"), "(3 + x)");
    EXPECT_EQ(simplified("max(1, 4, 2) * min(x, 3)"), "(4 * min(x, 3))");
    EXPECT_EQ(simplified("f(1 + 1)"), "f(2)") << "unknown functions keep their call";
}

TEST(Stage3Simplify, KeepsWhatWouldFail) {
    EXPECT_EQ(simplified("1 / 0"), "(1 / 0)");
    EXPECT_EQ(simplified("sqrt(-4)"), "sqrt(-4)");
    EXPECT_EQ(simplified("sqrt(1, 2)"), "sqrt(1, 2)");
    EXPECT_EQ(simplified("10 ^ 400"), "(10 ^ 400)");
}

TEST(Stage3Simplify, RemovesNeutralOperations) {
    EXPECT_EQ(simplified("x + 0"), "x");
    EXPECT_EQ(simplified("0 + x"), "x");
    EXPECT_EQ(simplified("x - 0"), "x");
    EXPECT_EQ(simplified("x * 1"), "x");
    EXPECT_EQ(simplified("1 * x"), "x");
    EXPECT_EQ(simplified("x / 1"), "x");
    EXPECT_EQ(simplified("x ^ 1"), "x");
    EXPECT_EQ(simplified("--x"), "x");
    EXPECT_EQ(simplified("(x + (3 - 3)) * (2 - 1)"), "x");
}

TEST(Stage3Simplify, LeavesOtherOperationsAlone) {
    EXPECT_EQ(simplified("0 - x"), "(0 - x)");
    EXPECT_EQ(simplified("1 / x"), "(1 / x)");
    EXPECT_EQ(simplified("x * 0"), "(x * 0)") << "x might be unknown: x * 0 is not always 0";
    EXPECT_EQ(simplified("-x"), "(-x)");
    EXPECT_EQ(simplified("a + b * c"), "(a + (b * c))");
}

TEST(Stage3Simplify, KeepsTheMeaning) {
    dojo::Environment env;
    env.variables = {{"x", 1.5}, {"y", -4}};
    for (const char* text : {"x * (1 + 1) - y / 1", "--(x ^ 1) + max(2, 3)", "(y + 0) * (3 - 2)"}) {
        const auto expr = dojo::parse(text);
        ASSERT_TRUE(expr.has_value()) << text;
        EXPECT_EQ(dojo::evaluate(dojo::simplify(*expr), env), dojo::evaluate(*expr, env)) << text;
    }
}

TEST(Stage3Simplify, ErrorsKeepTheirPositions) {
    const auto expr = dojo::parse("(1 + 1) * zeta");
    ASSERT_TRUE(expr.has_value());
    const auto result = dojo::evaluate(dojo::simplify(*expr));
    ASSERT_FALSE(result.has_value());
    EXPECT_EQ(result.error(),
              (dojo::Error{.position = 10, .kind = dojo::ErrorKind::UnknownVariable}));
}

TEST(Stage3Simplify, ListsTheVariablesSortedOnce) {
    using Names = std::vector<std::string>;
    EXPECT_EQ(names("y * x + max(x, z, -y) / w"), (Names{"w", "x", "y", "z"}));
    EXPECT_EQ(names("sqrt(2) + f(3)"), Names{});
    EXPECT_EQ(names("rate"), (Names{"rate"}));
}
