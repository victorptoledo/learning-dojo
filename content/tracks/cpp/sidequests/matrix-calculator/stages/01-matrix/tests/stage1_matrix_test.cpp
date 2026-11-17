#include <gtest/gtest.h>

#include <optional>

#include "matrix.hpp"
#include "matrix_testing.hpp"

using dojo::Matrix;
using dojo_testing::make;
using dojo_testing::numbers;
using dojo_testing::Rows;

TEST(Stage1Matrix, DefaultIsTheEmptyMatrix) {
    const Matrix m;
    EXPECT_EQ(m.rows(), 0U);
    EXPECT_EQ(m.cols(), 0U);
    EXPECT_TRUE(m.empty());
}

TEST(Stage1Matrix, SizedMatrixIsFilled) {
    const Matrix zeros(2, 3);
    EXPECT_EQ(zeros.rows(), 2U);
    EXPECT_EQ(zeros.cols(), 3U);
    EXPECT_FALSE(zeros.empty());
    EXPECT_EQ(numbers(zeros), (Rows{{0, 0, 0}, {0, 0, 0}}));
    EXPECT_EQ(numbers(Matrix(3, 1, 7.5)), (Rows{{7.5}, {7.5}, {7.5}}));
}

TEST(Stage1Matrix, NoRowsOrNoColumnsIsTheEmptyMatrix) {
    const Matrix no_rows(0, 5);
    const Matrix no_cols(3, 0);
    EXPECT_EQ(no_rows.cols(), 0U);
    EXPECT_EQ(no_cols.rows(), 0U);
    EXPECT_TRUE(no_rows.empty());
    EXPECT_EQ(no_rows, Matrix{});
    EXPECT_EQ(no_cols, Matrix{});
}

TEST(Stage1Matrix, FromRowsKeepsTheNumbersInOrder) {
    const auto m = Matrix::from_rows({{1, 2, 3}, {4, 5, 6}});
    ASSERT_TRUE(m.has_value());
    EXPECT_EQ(m->rows(), 2U);
    EXPECT_EQ(m->cols(), 3U);
    EXPECT_EQ(m->at(0, 2), 3.0);
    EXPECT_EQ(m->at(1, 0), 4.0);
    EXPECT_EQ(numbers(*m), (Rows{{1, 2, 3}, {4, 5, 6}}));
}

TEST(Stage1Matrix, FromRowsRefusesRaggedOrEmptyRows) {
    EXPECT_EQ(Matrix::from_rows({{1, 2}, {3}}), std::nullopt);
    EXPECT_EQ(Matrix::from_rows({{1}, {2, 3}}), std::nullopt);
    EXPECT_EQ(Matrix::from_rows({{}}), std::nullopt);
    EXPECT_EQ(Matrix::from_rows({{}, {}}), std::nullopt);
}

TEST(Stage1Matrix, FromNoRowsIsTheEmptyMatrix) {
    const auto m = Matrix::from_rows({});
    ASSERT_TRUE(m.has_value());
    EXPECT_TRUE(m->empty());
}

TEST(Stage1Matrix, IdentityHasOnesOnTheDiagonal) {
    EXPECT_EQ(numbers(Matrix::identity(3)), (Rows{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}}));
    EXPECT_EQ(Matrix::identity(1), make({{1}}));
    EXPECT_TRUE(Matrix::identity(0).empty());
}

TEST(Stage1Matrix, AtIsEmptyOutsideTheMatrix) {
    const Matrix m = make({{1, 2}, {3, 4}});
    EXPECT_EQ(m.at(1, 1), 4.0);
    EXPECT_EQ(m.at(2, 0), std::nullopt);
    EXPECT_EQ(m.at(0, 2), std::nullopt);
    EXPECT_EQ(Matrix{}.at(0, 0), std::nullopt);
}

TEST(Stage1Matrix, SetChangesOnlyThatNumber) {
    Matrix m(2, 2);
    EXPECT_TRUE(m.set(0, 1, 9));
    EXPECT_EQ(numbers(m), (Rows{{0, 9}, {0, 0}}));
    EXPECT_FALSE(m.set(2, 0, 5));
    EXPECT_FALSE(m.set(0, 2, 5));
    EXPECT_EQ(numbers(m), (Rows{{0, 9}, {0, 0}}));
}

TEST(Stage1Matrix, EqualMeansSameShapeAndNumbers) {
    EXPECT_EQ(make({{1, 2}, {3, 4}}), make({{1, 2}, {3, 4}}));
    EXPECT_NE(make({{1, 2}, {3, 4}}), make({{1, 2}, {3, 5}}));
    EXPECT_NE(Matrix(2, 3), Matrix(3, 2));
    EXPECT_NE(Matrix(1, 4), Matrix(4, 1));
}

TEST(Stage1Matrix, CopiesAreIndependentValues) {
    const Matrix original = make({{1, 2}, {3, 4}});
    Matrix copy = original;
    EXPECT_EQ(copy, original);
    copy.set(0, 0, 100);
    EXPECT_EQ(original.at(0, 0), 1.0);
    EXPECT_NE(copy, original);

    Matrix assigned;
    assigned = original;
    assigned.set(1, 1, -1);
    EXPECT_EQ(original.at(1, 1), 4.0);
}
