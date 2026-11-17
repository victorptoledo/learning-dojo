#include <gtest/gtest.h>

#include <optional>
#include <utility>

#include "expr.hpp"
#include "expr_testing.hpp"

using dojo::Error;
using dojo::ErrorKind;
using dojo_testing::parse_error;
using dojo_testing::tree;

TEST(Stage1Parse, NumbersAndNames) {
    EXPECT_EQ(tree("42"), "42");
    EXPECT_EQ(tree("  3.25 "), "3.25");
    EXPECT_EQ(tree("rate"), "rate");
    EXPECT_EQ(tree("_x2"), "_x2");
}

TEST(Stage1Parse, ProductsBindTighterThanSums) {
    EXPECT_EQ(tree("1 + 2 * 3"), "(1 + (2 * 3))");
    EXPECT_EQ(tree("1 * 2 + 3"), "((1 * 2) + 3)");
    EXPECT_EQ(tree("a/b-c*d"), "((a / b) - (c * d))");
}

TEST(Stage1Parse, SameLevelGroupsToTheLeft) {
    EXPECT_EQ(tree("8 - 3 - 2"), "((8 - 3) - 2)");
    EXPECT_EQ(tree("8 / 4 / 2"), "((8 / 4) / 2)");
}

TEST(Stage1Parse, PowerGroupsToTheRightAndBindsTightest) {
    EXPECT_EQ(tree("2 ^ 3 ^ 2"), "(2 ^ (3 ^ 2))");
    EXPECT_EQ(tree("2 * x ^ 2"), "(2 * (x ^ 2))");
    EXPECT_EQ(tree("-2 ^ 2"), "(-(2 ^ 2))");
    EXPECT_EQ(tree("2 ^ -1"), "(2 ^ (-1))");
}

TEST(Stage1Parse, ParenthesesAndNegation) {
    EXPECT_EQ(tree("(1 + 2) * 3"), "((1 + 2) * 3)");
    EXPECT_EQ(tree("-x"), "(-x)");
    EXPECT_EQ(tree("--x"), "(-(-x))");
    EXPECT_EQ(tree("3 * -(a + 1)"), "(3 * (-(a + 1)))");
    EXPECT_EQ(tree("((7))"), "7");
}

TEST(Stage1Parse, FunctionCalls) {
    EXPECT_EQ(tree("max(a, b + 1)"), "max(a, (b + 1))");
    EXPECT_EQ(tree("sqrt( x )"), "sqrt(x)");
    EXPECT_EQ(tree("now()"), "now()");
    EXPECT_EQ(tree("f(g(1), -2)"), "f(g(1), (-2))");
    EXPECT_EQ(tree("2 * min(1,2,3)"), "(2 * min(1, 2, 3))");
}

TEST(Stage1Parse, UnexpectedEndIsAtTheEndOfTheText) {
    EXPECT_EQ(parse_error(""), (Error{.position = 0, .kind = ErrorKind::UnexpectedEnd}));
    EXPECT_EQ(parse_error("1 + "), (Error{.position = 4, .kind = ErrorKind::UnexpectedEnd}));
    EXPECT_EQ(parse_error("2 * (3 + 4"), (Error{.position = 10, .kind = ErrorKind::UnexpectedEnd}));
    EXPECT_EQ(parse_error("max(1, 2"), (Error{.position = 8, .kind = ErrorKind::UnexpectedEnd}));
}

TEST(Stage1Parse, UnexpectedTokensAreWhereTheyStart) {
    EXPECT_EQ(parse_error("1 2"), (Error{.position = 2, .kind = ErrorKind::UnexpectedToken}));
    EXPECT_EQ(parse_error(")"), (Error{.position = 0, .kind = ErrorKind::UnexpectedToken}));
    EXPECT_EQ(parse_error("3 + * 4"), (Error{.position = 4, .kind = ErrorKind::UnexpectedToken}));
    EXPECT_EQ(parse_error("max(1,)"), (Error{.position = 6, .kind = ErrorKind::UnexpectedToken}));
    EXPECT_EQ(parse_error("(1 + 2) x"), (Error{.position = 8, .kind = ErrorKind::UnexpectedToken}));
}

TEST(Stage1Parse, UnexpectedCharactersAreWhereTheyAre) {
    EXPECT_EQ(parse_error("1 $ 2"), (Error{.position = 2, .kind = ErrorKind::UnexpectedCharacter}));
    EXPECT_EQ(parse_error(".5"), (Error{.position = 0, .kind = ErrorKind::UnexpectedCharacter}));
    EXPECT_EQ(parse_error("1."), (Error{.position = 1, .kind = ErrorKind::UnexpectedCharacter}));
    EXPECT_EQ(parse_error("a + b;"),
              (Error{.position = 5, .kind = ErrorKind::UnexpectedCharacter}));
}

TEST(Stage1Parse, TheTreeCanBeMoved) {
    auto expr = dojo::parse("1 + 2");
    ASSERT_TRUE(expr.has_value());
    dojo::Expr moved = std::move(*expr);
    EXPECT_EQ(dojo::to_string(moved), "(1 + 2)");
}
