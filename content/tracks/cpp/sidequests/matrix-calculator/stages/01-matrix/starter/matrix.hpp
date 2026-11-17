#pragma once

#include <cstddef>
#include <optional>
#include <vector>

namespace dojo {

// A rows x cols matrix of doubles. Invariant: it holds exactly rows() * cols() numbers, and a
// matrix with no rows has no columns (and the other way round).
class Matrix {
public:
    // The empty 0 x 0 matrix.
    Matrix() = default;

    // A rows x cols matrix with every number set to `fill`. Zero rows or zero columns give the
    // empty 0 x 0 matrix.
    Matrix(std::size_t rows, std::size_t cols, double fill = 0.0);

    // The matrix with these rows. No rows give the empty matrix; std::nullopt when a row is
    // empty or the rows do not all have the same length.
    static std::optional<Matrix> from_rows(const std::vector<std::vector<double>>& rows);

    // The n x n identity matrix (ones on the diagonal, zeros elsewhere).
    static Matrix identity(std::size_t n);

    std::size_t rows() const;
    std::size_t cols() const;
    bool empty() const;

    // The number at (row, col), counting from 0, or std::nullopt outside the matrix.
    std::optional<double> at(std::size_t row, std::size_t col) const;

    // Sets the number at (row, col); false (and nothing changes) outside the matrix.
    bool set(std::size_t row, std::size_t col, double value);

    // Same shape and same numbers.
    bool operator==(const Matrix&) const = default;

private:
    // Your data members: what does the class need to keep its invariant?
};

}  // namespace dojo
