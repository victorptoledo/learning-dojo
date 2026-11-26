#include <gtest/gtest.h>

#include <chrono>

#include "retention.hpp"

TEST(Visible, KeepsTheNewestBackupOfADay) {
    using namespace std::chrono;
    const std::vector<dojo::Backup> backups{
        {.path = "db-20261126-0300.bak",
         .taken = sys_days{2026y / November / 26} + 3h,
         .day = local_days{2026y / November / 26}},
        {.path = "db-20261126-1500.bak",
         .taken = sys_days{2026y / November / 26} + 15h,
         .day = local_days{2026y / November / 26}},
    };
    const dojo::Plan plan = dojo::plan(backups, dojo::Policy{.daily = 1, .weekly = 0, .monthly = 0});
    ASSERT_EQ(plan.keep.size(), 1U);
    EXPECT_EQ(plan.keep[0].path, "db-20261126-1500.bak");
    EXPECT_EQ(plan.keep[0].reason, "daily");
}
