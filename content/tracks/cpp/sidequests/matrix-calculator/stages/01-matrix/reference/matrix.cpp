#include "matrix.hpp"

#include <algorithm>

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

}  // namespace dojo
