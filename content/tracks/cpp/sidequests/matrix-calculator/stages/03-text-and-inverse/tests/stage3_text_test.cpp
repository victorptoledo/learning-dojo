#include <gtest/gtest.h>

#include <optional>
#include <string>

#include "matrix.hpp"
#include "matrix_testing.hpp"

using dojo::Matrix;
using dojo_testing::make;
using dojo_testing::near;
using dojo_testing::numbers;
using dojo_testing::Rows;

TEST(Stage3Text, ParsesRowsAndNumbers) {
    const auto m = dojo::parse("[1 2 3; 4 5 6]");
    ASSERT_TRUE(m.has_value());
    EXPECT_EQ(numbers(*m), (Rows{{1, 2, 3}, {4, 5, 6}}));
    const auto column = dojo::parse("[1; 2; 3]");
    ASSERT_TRUE(column.has_value());
    EXPECT_EQ(column->rows(), 3U);
    EXPECT_EQ(column->cols(), 1U);
}

TEST(Stage3Text, ParseAllowsWhitespaceAndAnyNumberForm) {
    const auto m = dojo::parse("  [ -1.5   2e3 ;\t0.25  -7 ]\n");
    ASSERT_TRUE(m.has_value());
    EXPECT_EQ(numbers(*m), (Rows{{-1.5, 2000}, {0.25, -7}}));
}

TEST(Stage3Text, ParseReadsTheEmptyMatrix) {
    const auto empty = dojo::parse("[]");
    ASSERT_TRUE(empty.has_value());
    EXPECT_TRUE(empty->empty());
    const auto spaced = dojo::parse(" [  ] ");
    ASSERT_TRUE(spaced.has_value());
    EXPECT_TRUE(spaced->empty());
}

TEST(Stage3Text, ParseRefusesWhatIsNotAMatrix) {
    for (const char* text :
         {"", "1 2; 3 4", "[1 2; 3 4", "1 2]", "[1 2; 3]", "[1 2;; 3 4]", "[1 2;]", "[; 1]",
          "[1 x]", "[1x 2]", "[1,2]", "[1 2] 3", "[[1 2]]", "[inf 1]", "[nan]", "[1 - 2]"}) {
        EXPECT_EQ(dojo::parse(text), std::nullopt) << "parse(\"" << text << "\")";
    }
}

TEST(Stage3Text, ToStringWritesTheShortestForm) {
    EXPECT_EQ(dojo::to_string(make({{1, 2}, {3, 4}})), "[1 2; 3 4]");
    EXPECT_EQ(dojo::to_string(make({{-1.5, 0.25, 2000}})), "[-1.5 0.25 2000]");
    EXPECT_EQ(dojo::to_string(make({{7}})), "[7]");
    EXPECT_EQ(dojo::to_string(make({{1}, {2}})), "[1; 2]");
    EXPECT_EQ(dojo::to_string(Matrix{}), "[]");
}

TEST(Stage3Text, TextRoundTripsExactly) {
    const Matrix m = make({{0.1, 1.0 / 3.0}, {-2.0 / 7.0, 123456.789}});
    const auto back = dojo::parse(dojo::to_string(m));
    ASSERT_TRUE(back.has_value()) << dojo::to_string(m);
    EXPECT_EQ(*back, m) << dojo::to_string(m);
}

TEST(Stage3Inverse, DeterminantOfSmallMatrices) {
    EXPECT_NEAR(dojo::determinant(make({{3}})).value_or(NAN), 3.0, 1e-9);
    EXPECT_NEAR(dojo::determinant(make({{1, 2}, {3, 4}})).value_or(NAN), -2.0, 1e-9);
    EXPECT_NEAR(dojo::determinant(make({{2, -3, 1}, {2, 0, -1}, {1, 4, 5}})).value_or(NAN), 49.0,
                1e-9);
    EXPECT_NEAR(dojo::determinant(Matrix::identity(5)).value_or(NAN), 1.0, 1e-9);
}

TEST(Stage3Inverse, DeterminantNeedsPivoting) {
    // A zero in the top-left corner: elimination has to swap rows, and each swap flips the sign.
    EXPECT_NEAR(dojo::determinant(make({{0, 1}, {1, 0}})).value_or(NAN), -1.0, 1e-9);
    EXPECT_NEAR(dojo::determinant(make({{0, 1, 1}, {1, 0, 1}, {1, 1, 1}})).value_or(NAN), 1.0,
                1e-9);
}

TEST(Stage3Inverse, DeterminantOfSpecialCases) {
    EXPECT_NEAR(dojo::determinant(make({{1, 2}, {2, 4}})).value_or(NAN), 0.0, 1e-9);
    EXPECT_EQ(dojo::determinant(Matrix{}), 1.0);
    EXPECT_EQ(dojo::determinant(Matrix(2, 3)), std::nullopt);
}

TEST(Stage3Inverse, InverseUndoesTheMatrix) {
    const Matrix m = make({{4, 7}, {2, 6}});
    const auto inv = dojo::inverse(m);
    ASSERT_TRUE(inv.has_value());
    EXPECT_TRUE(near(*inv, {{0.6, -0.7}, {-0.2, 0.4}}));
    const auto product = dojo::multiply(m, *inv);
    ASSERT_TRUE(product.has_value());
    EXPECT_TRUE(near(*product, numbers(Matrix::identity(2))));
}

TEST(Stage3Inverse, InverseOfALargerMatrixWithPivoting) {
    const Matrix m = make({{0, 1, 1}, {1, 0, 1}, {1, 1, 1}});
    const auto inv = dojo::inverse(m);
    ASSERT_TRUE(inv.has_value());
    EXPECT_TRUE(near(*inv, {{-1, 0, 1}, {0, -1, 1}, {1, 1, -1}}));
    EXPECT_EQ(numbers(m), (Rows{{0, 1, 1}, {1, 0, 1}, {1, 1, 1}})) << "inverse changed m";
}

TEST(Stage3Inverse, NoInverseForSingularOrNonSquare) {
    EXPECT_EQ(dojo::inverse(make({{1, 2}, {2, 4}})), std::nullopt);
    EXPECT_EQ(dojo::inverse(make({{1, 2, 3}, {4, 5, 6}, {7, 8, 9}})), std::nullopt);
    EXPECT_EQ(dojo::inverse(Matrix(3, 3)), std::nullopt);
    EXPECT_EQ(dojo::inverse(Matrix(2, 3)), std::nullopt);
    const auto empty = dojo::inverse(Matrix{});
    ASSERT_TRUE(empty.has_value());
    EXPECT_TRUE(empty->empty());
}
