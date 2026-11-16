#include <gtest/gtest.h>

#include "logs.hpp"

TEST(Visible, ParsesOneEntry) {
    const auto entry = dojo::parse_entry("1700000000 ERROR billing 250ms card declined");
    ASSERT_TRUE(entry.has_value());
    EXPECT_EQ(entry->service, "billing");
    EXPECT_EQ(entry->latency_ms, 250);
    EXPECT_EQ(entry->message, "card declined");
}
