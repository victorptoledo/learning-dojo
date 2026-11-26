#include <gtest/gtest.h>

#include <chrono>
#include <format>

#include "timefmt.hpp"

using namespace std::chrono_literals;

TEST(Visible, ParsesAndPrintsADuration) {
    EXPECT_EQ(dojo::parse_duration("1h30m"), 90min);
    EXPECT_EQ(std::format("{}", dojo::Elapsed{90min}), "1h30m");
}
