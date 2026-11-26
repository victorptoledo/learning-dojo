#include <gtest/gtest.h>

#include "telemetry.hpp"

TEST(Visible, PublishesAReading) {
    dojo::Hub hub;
    hub.publish(dojo::Reading{"temp", 21.5});
    const auto stats = hub.snapshot();
    ASSERT_EQ(stats.size(), 1U);
    EXPECT_EQ(stats[0].sensor, "temp");
    EXPECT_DOUBLE_EQ(stats[0].max, 21.5);
    EXPECT_EQ(dojo::describe(dojo::Reading{"temp", 21.5}), "temp = 21.5");
}
