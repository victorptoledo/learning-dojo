#include <gtest/gtest.h>

#include "config.hpp"

TEST(Visible, ParsesAndReadsAnInt) {
    const auto config = dojo::Config::parse("port = 8080\n");
    ASSERT_TRUE(config.has_value());
    EXPECT_EQ(config->get<int>("port"), 8080);
}
