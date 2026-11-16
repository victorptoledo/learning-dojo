#include <gtest/gtest.h>

#include "greet.hpp"

TEST(Visible, GreetsByName) {
    EXPECT_EQ(dojo::greet("Ada"), "Hello, Ada!");
}
