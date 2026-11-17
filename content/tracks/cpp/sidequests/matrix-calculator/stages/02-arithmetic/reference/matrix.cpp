#include "matrix.hpp"

#include <algorithm>
#include <functional>

namespace dojo {

Matrix::Matrix(std::size_t rows, std::size_t cols, double fill) {
    if (rows == 0 || cols == 0) {
        return;  // the empty matrix: no rows and no columns
    }
    rows_ = rows;
    cols_ = cols;
    values_.assign(rows * cols, fill);
}

std::optional<Matrix> Matrix::from_rows(const std::vector<std::vector<double>>& rows) {
    if (rows.empty()) {
        return Matrix{};
    }
    const std::size_t width = rows.front().size();
    const bool rectangular =
        std::ranges::all_of(rows, [width](const auto& row) { return row.size() == width; });
    if (width == 0 || !rectangular) {
        return std::nullopt;
    }
    Matrix result(rows.size(), width);
    auto out = result.values_.begin();
    for (const auto& row : rows) {
        out = std::ranges::copy(row, out).out;
    }
    return result;
}

Matrix Matrix::identity(std::size_t n) {
    Matrix result(n, n);
    for (std::size_t i = 0; i < n; ++i) {
        result.values_[result.index(i, i)] = 1.0;
    }
    return result;
}

std::optional<double> Matrix::at(std::size_t row, std::size_t col) const {
    if (!contains(row, col)) {
        return std::nullopt;
    }
    return values_[index(row, col)];
}

bool Matrix::set(std::size_t row, std::size_t col, double value) {
    if (!contains(row, col)) {
        return false;
    }
    values_[index(row, col)] = value;
    return true;
}

Matrix Matrix::transposed() const {
    Matrix result(cols_, rows_);
    for (std::size_t r = 0; r < rows_; ++r) {
        for (std::size_t c = 0; c < cols_; ++c) {
            result.values_[result.index(c, r)] = values_[index(r, c)];
        }
    }
    return result;
}

Matrix& Matrix::operator*=(double k) {
    for (double& value : values_) {
        value *= k;
    }
    return *this;
}

namespace {

bool same_shape(const Matrix& a, const Matrix& b) {
    return a.rows() == b.rows() && a.cols() == b.cols();
}

}  // namespace

std::optional<Matrix> add(const Matrix& a, const Matrix& b) {
    if (!same_shape(a, b)) {
        return std::nullopt;
    }
    return Matrix::zip(a, b, std::plus{});
}

std::optional<Matrix> subtract(const Matrix& a, const Matrix& b) {
    if (!same_shape(a, b)) {
        return std::nullopt;
    }
    return Matrix::zip(a, b, std::minus{});
}

Matrix Matrix::product(const Matrix& a, const Matrix& b) {
    Matrix result(a.rows_, b.cols_);
    for (std::size_t r = 0; r < a.rows_; ++r) {
        for (std::size_t k = 0; k < a.cols_; ++k) {
            const double left = a.values_[a.index(r, k)];
            for (std::size_t c = 0; c < b.cols_; ++c) {
                result.values_[result.index(r, c)] += left * b.values_[b.index(k, c)];
            }
        }
    }
    return result;
}

std::optional<Matrix> multiply(const Matrix& a, const Matrix& b) {
    if (a.cols() != b.rows()) {
        return std::nullopt;
    }
    return Matrix::product(a, b);
}

Matrix operator*(const Matrix& m, double k) {
    Matrix result = m;
    result *= k;
    return result;
}

Matrix operator*(double k, const Matrix& m) {
    return m * k;
}

Matrix operator-(const Matrix& m) {
    return m * -1.0;
}

std::optional<double> trace(const Matrix& m) {
    if (m.rows() != m.cols()) {
        return std::nullopt;
    }
    double sum = 0.0;
    for (std::size_t i = 0; i < m.rows(); ++i) {
        sum += m.at(i, i).value_or(0.0);
    }
    return sum;
}

std::optional<Matrix> power(const Matrix& m, unsigned exponent) {
    if (m.rows() != m.cols()) {
        return std::nullopt;
    }
    // Squaring: m^13 = m^8 * m^4 * m^1, about log2(exponent) products instead of exponent.
    Matrix result = Matrix::identity(m.rows());
    Matrix square = m;
    while (exponent > 0) {
        if ((exponent & 1U) != 0) {
            result = Matrix::product(result, square);
        }
        exponent >>= 1U;
        if (exponent > 0) {
            square = Matrix::product(square, square);
        }
    }
    return result;
}

}  // namespace dojo
