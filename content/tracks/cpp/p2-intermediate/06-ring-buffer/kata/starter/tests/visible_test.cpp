#include <gtest/gtest.h>

#include <vector>

#include "generic.hpp"

TEST(Visible, SumsInts) {
    EXPECT_EQ(dojo::sum(std::vector<int>{1, 2, 3}), 6);
}

TEST(Visible, RingBufferIsFirstInFirstOut) {
    dojo::RingBuffer<int, 2> buffer;
    EXPECT_TRUE(buffer.push(1));
    EXPECT_TRUE(buffer.push(2));
    EXPECT_EQ(buffer.pop(), 1);
}
