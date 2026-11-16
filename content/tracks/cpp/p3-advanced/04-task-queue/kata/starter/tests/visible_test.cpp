#include <gtest/gtest.h>

#include "pool.hpp"

TEST(Visible, QueueIsFirstInFirstOut) {
    dojo::BlockingQueue<int> queue;
    EXPECT_TRUE(queue.push(1));
    EXPECT_TRUE(queue.push(2));
    EXPECT_EQ(queue.pop(), 1);
    EXPECT_EQ(queue.try_pop(), 2);
}
