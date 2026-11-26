#include <gtest/gtest.h>

#include <array>

#include "metrics.hpp"

TEST(Visible, RecordsIntoBuckets) {
    constexpr std::array<double, 2> bounds{1.0, 5.0};
    dojo::Histogram h(bounds);
    h.record(0.5);
    h.record(3.0);
    h.record(3.0);
    EXPECT_EQ(h.count(), 3U);
    EXPECT_EQ(h.buckets()[1], 2U);
}
