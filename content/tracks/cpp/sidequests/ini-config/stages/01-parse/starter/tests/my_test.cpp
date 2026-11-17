// Your own tests: add as many as you like. The dojo adds its contract tests to this folder
// when it checks a stage.
#include <gtest/gtest.h>

#include "config.hpp"

TEST(MyConfig, ReadsOneKey) {
    const auto config = dojo::Config::parse("[server]\nport = 8080\n");
    ASSERT_TRUE(config.has_value());
    EXPECT_EQ(config->raw("server", "port"), "8080");
}
