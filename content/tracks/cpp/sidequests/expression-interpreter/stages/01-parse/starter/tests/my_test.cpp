// Your own tests: add as many as you like. The dojo adds its contract tests to this folder
// when it checks a stage.
#include <gtest/gtest.h>

#include "expr.hpp"

TEST(MyExpr, ProductsBindTighterThanSums) {
    const auto expr = dojo::parse("1 + 2 * 3");
    ASSERT_TRUE(expr.has_value());
    EXPECT_EQ(dojo::to_string(*expr), "(1 + (2 * 3))");
}
