#include <gtest/gtest.h>

#include <optional>

#include "matrix.hpp"
#include "matrix_testing.hpp"

using dojo::Matrix;
using dojo_testing::make;
using dojo_testing::numbers;
using dojo_testing::Rows;

TEST(Stage2Arithmetic, TransposeSwapsRowsAndColumns) {
    const Matrix m = make({{1, 2, 3}, {4, 5, 6}});
    const Matrix t = m.transposed();
    EXPECT_EQ(t.rows(), 3U);
    EXPECT_EQ(t.cols(), 2U);
    EXPECT_EQ(numbers(t), (Rows{{1, 4}, {2, 5}, {3, 6}}));
    EXPECT_EQ(t.transposed(), m);
    EXPECT_EQ(numbers(m), (Rows{{1, 2, 3}, {4, 5, 6}})) << "transposed() changed the original";
    EXPECT_TRUE(Matrix{}.transposed().empty());
}

TEST(Stage2Arithmetic, AddAndSubtractNumberByNumber) {
    const Matrix a = make({{1, 2}, {3, 4}});
    const Matrix b = make({{10, 20}, {30, 40}});
    const auto sum = dojo::add(a, b);
    ASSERT_TRUE(sum.has_value());
    EXPECT_EQ(numbers(*sum), (Rows{{11, 22}, {33, 44}}));
    const auto difference = dojo::subtract(a, b);
    ASSERT_TRUE(difference.has_value());
    EXPECT_EQ(numbers(*difference), (Rows{{-9, -18}, {-27, -36}}));
    EXPECT_EQ(numbers(a), (Rows{{1, 2}, {3, 4}})) << "add or subtract changed an argument";
}

TEST(Stage2Arithmetic, AddingEmptyMatricesGivesTheEmptyMatrix) {
    const auto sum = dojo::add(Matrix{}, Matrix(0, 4));
    ASSERT_TRUE(sum.has_value());
    EXPECT_TRUE(sum->empty());
}

TEST(Stage2Arithmetic, AddAndSubtractNeedTheSameShape) {
    EXPECT_EQ(dojo::add(Matrix(2, 3), Matrix(3, 2)), std::nullopt);
    EXPECT_EQ(dojo::add(Matrix(2, 2), Matrix(2, 3)), std::nullopt);
    EXPECT_EQ(dojo::subtract(Matrix(1, 2), Matrix(2, 1)), std::nullopt);
    EXPECT_EQ(dojo::add(Matrix(1, 1), Matrix{}), std::nullopt);
}

TEST(Stage2Arithmetic, MultiplyIsTheMatrixProduct) {
    const Matrix a = make({{1, 2, 3}, {4, 5, 6}});
    const Matrix b = make({{7, 8}, {9, 10}, {11, 12}});
    const auto ab = dojo::multiply(a, b);
    ASSERT_TRUE(ab.has_value());
    EXPECT_EQ(numbers(*ab), (Rows{{58, 64}, {139, 154}}));
    const auto ba = dojo::multiply(b, a);
    ASSERT_TRUE(ba.has_value());
    EXPECT_EQ(ba->rows(), 3U);
    EXPECT_EQ(ba->cols(), 3U);
    EXPECT_EQ(numbers(*ba), (Rows{{39, 54, 69}, {49, 68, 87}, {59, 82, 105}}));
}

TEST(Stage2Arithmetic, MultiplyByAVectorAndByTheIdentity) {
    const Matrix m = make({{2, 0, 1}, {1, 3, 2}});
    const auto column = dojo::multiply(m, make({{1}, {2}, {3}}));
    ASSERT_TRUE(column.has_value());
    EXPECT_EQ(numbers(*column), (Rows{{5}, {13}}));
    EXPECT_EQ(dojo::multiply(m, Matrix::identity(3)), m);
    EXPECT_EQ(dojo::multiply(Matrix::identity(2), m), m);
}

TEST(Stage2Arithmetic, MultiplyNeedsColumnsToMatchRows) {
    EXPECT_EQ(dojo::multiply(Matrix(2, 3), Matrix(2, 3)), std::nullopt);
    EXPECT_EQ(dojo::multiply(Matrix(3, 1), Matrix(3, 1)), std::nullopt);
    EXPECT_EQ(dojo::multiply(Matrix(2, 2), Matrix{}), std::nullopt);
}

TEST(Stage2Arithmetic, ScalarProductAndNegation) {
    const Matrix m = make({{1, -2}, {0.5, 4}});
    EXPECT_EQ(numbers(m * 2.0), (Rows{{2, -4}, {1, 8}}));
    EXPECT_EQ(numbers(3.0 * m), (Rows{{3, -6}, {1.5, 12}}));
    EXPECT_EQ(numbers(-m), (Rows{{-1, 2}, {-0.5, -4}}));
    EXPECT_EQ(numbers(m), (Rows{{1, -2}, {0.5, 4}})) << "an operator changed its operand";

    Matrix scaled = m;
    scaled *= 10.0;
    EXPECT_EQ(numbers(scaled), (Rows{{10, -20}, {5, 40}}));
    EXPECT_EQ(&(scaled *= 1.0), &scaled) << "*= should return the matrix itself";
}

TEST(Stage2Arithmetic, TraceOfASquareMatrix) {
    EXPECT_EQ(dojo::trace(make({{1, 2}, {3, 4}})), 5.0);
    EXPECT_EQ(dojo::trace(Matrix::identity(4)), 4.0);
    EXPECT_EQ(dojo::trace(Matrix{}), 0.0);
    EXPECT_EQ(dojo::trace(Matrix(2, 3)), std::nullopt);
}

TEST(Stage2Arithmetic, PowerMultipliesRepeatedly) {
    const Matrix m = make({{1, 1}, {1, 0}});
    EXPECT_EQ(dojo::power(m, 0), Matrix::identity(2));
    EXPECT_EQ(dojo::power(m, 1), m);
    const auto fifth = dojo::power(m, 5);
    ASSERT_TRUE(fifth.has_value());
    EXPECT_EQ(numbers(*fifth), (Rows{{8, 5}, {5, 3}}));
    // The Fibonacci numbers: m^40 holds F(41), F(40) and F(39).
    const auto fortieth = dojo::power(m, 40);
    ASSERT_TRUE(fortieth.has_value());
    EXPECT_EQ(numbers(*fortieth), (Rows{{165580141, 102334155}, {102334155, 63245986}}));
}

TEST(Stage2Arithmetic, PowerNeedsASquareMatrix) {
    EXPECT_EQ(dojo::power(Matrix(2, 3), 2), std::nullopt);
    EXPECT_EQ(dojo::power(Matrix(1, 2), 0), std::nullopt);
    EXPECT_EQ(dojo::power(Matrix{}, 3), Matrix{});
}
