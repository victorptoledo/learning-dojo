// Your own tests: add as many as you like. The dojo adds its contract tests to this folder
// when it checks a stage.
#include <gtest/gtest.h>

#include "matrix.hpp"

TEST(MyMatrix, NewMatrixHasItsShape) {
    const dojo::Matrix m(2, 3);
    EXPECT_EQ(m.rows(), 2U);
    EXPECT_EQ(m.cols(), 3U);
}
