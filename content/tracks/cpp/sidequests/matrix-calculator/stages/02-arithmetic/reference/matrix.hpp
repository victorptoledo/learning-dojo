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

    std::size_t rows() const { return rows_; }
    std::size_t cols() const { return cols_; }
    bool empty() const { return values_.empty(); }

    // The number at (row, col), counting from 0, or std::nullopt outside the matrix.
    std::optional<double> at(std::size_t row, std::size_t col) const;

    // Sets the number at (row, col); false (and nothing changes) outside the matrix.
    bool set(std::size_t row, std::size_t col, double value);

    // Same shape and same numbers.
    bool operator==(const Matrix&) const = default;

    // The cols x rows matrix whose (c, r) is this matrix's (r, c).
    Matrix transposed() const;

    // Every number multiplied by k.
    Matrix& operator*=(double k);

private:
    friend std::optional<Matrix> add(const Matrix& a, const Matrix& b);
    friend std::optional<Matrix> subtract(const Matrix& a, const Matrix& b);
    friend std::optional<Matrix> multiply(const Matrix& a, const Matrix& b);
    friend std::optional<Matrix> power(const Matrix& m, unsigned exponent);

    // a op b number by number; the shapes must match.
    template <typename Op>
    static Matrix zip(const Matrix& a, const Matrix& b, Op op) {
        Matrix result = a;
        for (std::size_t i = 0; i < result.values_.size(); ++i) {
            result.values_[i] = op(a.values_[i], b.values_[i]);
        }
        return result;
    }

    // a x b; a must have as many columns as b has rows.
    static Matrix product(const Matrix& a, const Matrix& b);

    bool contains(std::size_t row, std::size_t col) const { return row < rows_ && col < cols_; }
    std::size_t index(std::size_t row, std::size_t col) const { return (row * cols_) + col; }

    std::size_t rows_ = 0;
    std::size_t cols_ = 0;
    std::vector<double> values_;  // row by row
};

// Number by number, for two matrices of the same shape; std::nullopt otherwise.
std::optional<Matrix> add(const Matrix& a, const Matrix& b);
std::optional<Matrix> subtract(const Matrix& a, const Matrix& b);

// The matrix product a x b, when a has as many columns as b has rows; std::nullopt otherwise.
std::optional<Matrix> multiply(const Matrix& a, const Matrix& b);

// Every number multiplied by k, or negated.
Matrix operator*(const Matrix& m, double k);
Matrix operator*(double k, const Matrix& m);
Matrix operator-(const Matrix& m);

// The sum of the diagonal of a square matrix (0 for the empty one); std::nullopt otherwise.
std::optional<double> trace(const Matrix& m);

// m multiplied by itself `exponent` times (the identity for 0), for a square matrix only.
std::optional<Matrix> power(const Matrix& m, unsigned exponent);

}  // namespace dojo
