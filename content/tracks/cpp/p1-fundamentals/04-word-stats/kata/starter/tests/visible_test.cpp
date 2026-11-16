#include <gtest/gtest.h>

#include "words.hpp"

TEST(Visible, SplitsAndLowersWords) {
    const std::vector<std::string> expected{"the", "cat", "don't", "stop"};
    EXPECT_EQ(dojo::words("The cat, don't STOP!"), expected);
}
