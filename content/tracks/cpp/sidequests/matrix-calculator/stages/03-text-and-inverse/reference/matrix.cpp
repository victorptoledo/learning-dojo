#include "matrix.hpp"

#include <algorithm>
#include <charconv>
#include <cmath>
#include <format>
#include <functional>
#include <utility>

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

namespace {

bool is_space(char c) {
    return c == ' ' || c == '\t' || c == '\n' || c == '\r';
}

std::string_view trim(std::string_view text) {
    while (!text.empty() && is_space(text.front())) {
        text.remove_prefix(1);
    }
    while (!text.empty() && is_space(text.back())) {
        text.remove_suffix(1);
    }
    return text;
}

// The finite numbers of one row ("1 -2.5 3e2"), or std::nullopt.
std::optional<std::vector<double>> parse_row(std::string_view text) {
    std::vector<double> row;
    text = trim(text);
    while (!text.empty()) {
        const std::size_t length = std::min(text.find_first_of(" \t\n\r"), text.size());
        const std::string_view token = text.substr(0, length);
        const char* const first = token.data();
        const char* const last = first + token.size();
        double value = 0.0;
        const auto [next, error] = std::from_chars(first, last, value);
        if (error != std::errc{} || next != last || !std::isfinite(value)) {
            return std::nullopt;
        }
        row.push_back(value);
        text = trim(text.substr(length));
    }
    return row;
}

// Gaussian elimination with partial pivoting: the rows of `work` become upper triangular
// (and `other`, if given, gets the same row operations). Returns the determinant, or 0 when a
// pivot is too small to divide by.
double eliminate(std::vector<std::vector<double>>& work, std::vector<std::vector<double>>* other) {
    constexpr double kTiny = 1e-12;
    const std::size_t n = work.size();
    double det = 1.0;
    for (std::size_t col = 0; col < n; ++col) {
        const auto pivot = std::ranges::max_element(
            work.begin() + static_cast<std::ptrdiff_t>(col), work.end(), {},
            [col](const std::vector<double>& row) { return std::abs(row[col]); });
        const auto pivot_row = static_cast<std::size_t>(pivot - work.begin());
        if (std::abs(work[pivot_row][col]) < kTiny) {
            return 0.0;
        }
        if (pivot_row != col) {
            std::swap(work[pivot_row], work[col]);
            if (other != nullptr) {
                std::swap((*other)[pivot_row], (*other)[col]);
            }
            det = -det;
        }
        det *= work[col][col];
        for (std::size_t row = col + 1; row < n; ++row) {
            const double factor = work[row][col] / work[col][col];
            for (std::size_t k = col; k < n; ++k) {
                work[row][k] -= factor * work[col][k];
            }
            if (other != nullptr) {
                for (std::size_t k = 0; k < n; ++k) {
                    (*other)[row][k] -= factor * (*other)[col][k];
                }
            }
        }
    }
    return det;
}

std::vector<std::vector<double>> to_rows(const Matrix& m) {
    std::vector<std::vector<double>> rows(m.rows(), std::vector<double>(m.cols()));
    for (std::size_t r = 0; r < m.rows(); ++r) {
        for (std::size_t c = 0; c < m.cols(); ++c) {
            rows[r][c] = m.at(r, c).value_or(0.0);
        }
    }
    return rows;
}

}  // namespace

std::optional<Matrix> parse(std::string_view text) {
    text = trim(text);
    if (text.size() < 2 || text.front() != '[' || text.back() != ']') {
        return std::nullopt;
    }
    text = trim(text.substr(1, text.size() - 2));
    if (text.empty()) {
        return Matrix{};
    }
    std::vector<std::vector<double>> rows;
    while (true) {
        const std::size_t semicolon = text.find(';');
        auto row = parse_row(text.substr(0, semicolon));
        if (!row || row->empty()) {
            return std::nullopt;
        }
        rows.push_back(std::move(*row));
        if (semicolon == std::string_view::npos) {
            break;
        }
        text.remove_prefix(semicolon + 1);
    }
    return Matrix::from_rows(rows);
}

std::string to_string(const Matrix& m) {
    std::string text = "[";
    for (std::size_t r = 0; r < m.rows(); ++r) {
        if (r > 0) {
            text += "; ";
        }
        for (std::size_t c = 0; c < m.cols(); ++c) {
            if (c > 0) {
                text += ' ';
            }
            text += std::format("{}", m.at(r, c).value_or(0.0));
        }
    }
    text += ']';
    return text;
}

std::optional<double> determinant(const Matrix& m) {
    if (m.rows() != m.cols()) {
        return std::nullopt;
    }
    auto work = to_rows(m);
    return eliminate(work, nullptr);
}

std::optional<Matrix> inverse(const Matrix& m) {
    if (m.rows() != m.cols()) {
        return std::nullopt;
    }
    const std::size_t n = m.rows();
    auto work = to_rows(m);
    auto result = to_rows(Matrix::identity(n));
    if (eliminate(work, &result) == 0.0) {
        return std::nullopt;
    }
    // Back substitution: clear above each pivot, then scale it to 1.
    for (std::size_t col = n; col-- > 0;) {
        for (std::size_t row = 0; row < col; ++row) {
            const double factor = work[row][col] / work[col][col];
            for (std::size_t k = 0; k < n; ++k) {
                result[row][k] -= factor * result[col][k];
            }
            work[row][col] = 0.0;
        }
        for (std::size_t k = 0; k < n; ++k) {
            result[col][k] /= work[col][col];
        }
    }
    return Matrix::from_rows(result);
}

}  // namespace dojo
