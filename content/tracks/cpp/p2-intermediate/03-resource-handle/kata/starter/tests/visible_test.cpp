#include <gtest/gtest.h>

#include "buffer.hpp"

TEST(Visible, NewBufferIsZeroed) {
    const dojo::Buffer buffer(3);
    ASSERT_EQ(buffer.size(), 3U);
    EXPECT_EQ(buffer[0], 0);
    EXPECT_EQ(buffer[2], 0);
}
