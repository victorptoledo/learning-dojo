// Helpers shared by the contract tests of every stage.
#pragma once

#include <gtest/gtest.h>

#include <cmath>
#include <cstddef>
#include <sstream>
#include <vector>

#include "matrix.hpp"

namespace dojo_testing {

using Rows = std::vector<std::vector<double>>;

// The matrix with these rows; the test fails if from_rows refuses them.
inline dojo::Matrix make(const Rows& rows) {
    auto matrix = dojo::Matrix::from_rows(rows);
    EXPECT_TRUE(matrix.has_value()) << "Matrix::from_rows refused a rectangular matrix";
    return matrix.value_or(dojo::Matrix{});
}

// Every number of the matrix, row by row, read through at().
inline Rows numbers(const dojo::Matrix& m) {
    Rows result(m.rows());
    for (std::size_t r = 0; r < m.rows(); ++r) {
        for (std::size_t c = 0; c < m.cols(); ++c) {
            result[r].push_back(m.at(r, c).value_or(NAN));
        }
    }
    return result;
}

// Same shape, and every number within `tolerance` of the expected one.
inline testing::AssertionResult near(const dojo::Matrix& actual, const Rows& expected,
                                     double tolerance = 1e-9) {
    const Rows got = numbers(actual);
    bool same = got.size() == expected.size();
    for (std::size_t r = 0; same && r < got.size(); ++r) {
        same = got[r].size() == expected[r].size();
        for (std::size_t c = 0; same && c < got[r].size(); ++c) {
            same = std::abs(got[r][c] - expected[r][c]) <= tolerance;
        }
    }
    if (same) {
        return testing::AssertionSuccess();
    }
    std::ostringstream text;
    text << "got a " << actual.rows() << "x" << actual.cols() << " matrix:";
    for (const auto& row : got) {
        text << " [";
        for (double value : row) {
            text << " " << value;
        }
        text << " ]";
    }
    return testing::AssertionFailure() << text.str();
}

}  // namespace dojo_testing
