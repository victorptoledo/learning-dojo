#include "matrix.hpp"

namespace dojo {

Matrix::Matrix(std::size_t rows, std::size_t cols, double fill) {
    (void)rows;
    (void)cols;
    (void)fill;
}

std::optional<Matrix> Matrix::from_rows(const std::vector<std::vector<double>>& rows) {
    (void)rows;
    return std::nullopt;
}

Matrix Matrix::identity(std::size_t n) {
    (void)n;
    return {};
}

std::size_t Matrix::rows() const {
    return 0;
}

std::size_t Matrix::cols() const {
    return 0;
}

bool Matrix::empty() const {
    return true;
}

std::optional<double> Matrix::at(std::size_t row, std::size_t col) const {
    (void)row;
    (void)col;
    return std::nullopt;
}

bool Matrix::set(std::size_t row, std::size_t col, double value) {
    (void)row;
    (void)col;
    (void)value;
    return false;
}

}  // namespace dojo
