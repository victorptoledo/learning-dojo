#include <gtest/gtest.h>

#include <string>
#include <string_view>
#include <vector>

#include "logs.hpp"

using dojo::Entry;
using dojo::Level;

namespace {

const char* const kLog =
    "100 INFO auth 12ms login ok\n"
    "101 ERROR billing 250ms card declined\n"
    "\n"
    "garbage line\n"
    "102 WARN auth 90ms slow token check\n"
    "103 ERROR billing 40ms \n"
    "104 ERROR search 250ms index missing\r\n"
    "105 DEBUG search 1ms\n"
    "106 FATAL auth 5ms unknown level\n"
    "107 INFO auth 5 no unit\n"
    "108 ERROR auth 7ms bad password";

std::vector<std::string> collect(std::string_view text) {
    std::vector<std::string> out;
    for (const std::string_view line : dojo::lines(text)) {
        out.emplace_back(line);
    }
    return out;
}

}  // namespace

TEST(Lines, SplitsAndSkipsBlankLines) {
    EXPECT_EQ(collect("a\n\nb c\r\n  \nd"), (std::vector<std::string>{"a", "b c", "d"}));
    EXPECT_TRUE(collect("").empty());
    EXPECT_TRUE(collect("\n\n").empty());
}

TEST(Lines, IsLazy) {
    auto generator = dojo::lines("first\nsecond\nthird");
    auto it = generator.begin();
    EXPECT_EQ(*it, "first");
    ++it;
    EXPECT_EQ(*it, "second");
}

TEST(ParseEntry, ReadsEveryField) {
    EXPECT_EQ(dojo::parse_entry("101 ERROR billing 250ms card declined"),
              (Entry{101, Level::Error, "billing", 250, "card declined"}));
    EXPECT_EQ(dojo::parse_entry("105 DEBUG search 1ms"),
              (Entry{105, Level::Debug, "search", 1, ""}));
}

TEST(ParseEntry, RejectsMalformedLines) {
    for (const char* bad : {"", "garbage line", "106 FATAL auth 5ms x", "107 INFO auth 5 x",
                            "x INFO auth 5ms y", "1 INFO auth ms y", "1 INFO", "1 INFO auth"}) {
        EXPECT_EQ(dojo::parse_entry(bad), std::nullopt) << "line: [" << bad << "]";
    }
}

TEST(ParseAll, KeepsTheGoodEntriesInOrder) {
    const auto entries = dojo::parse_all(kLog);
    ASSERT_EQ(entries.size(), 7U);
    EXPECT_EQ(entries.front().timestamp, 100);
    EXPECT_EQ(entries[3], (Entry{103, Level::Error, "billing", 40, ""}));
    EXPECT_EQ(entries[4].message, "index missing");
    EXPECT_EQ(entries.back().timestamp, 108);
}

TEST(ErrorsByService, CountsOnlyErrors) {
    const auto counts = dojo::errors_by_service(dojo::parse_all(kLog));
    EXPECT_EQ(counts, (std::map<std::string, int>{{"auth", 1}, {"billing", 2}, {"search", 1}}));
}

TEST(Slowest, HighestLatencyThenEarliest) {
    const auto top = dojo::slowest(dojo::parse_all(kLog), 3);
    ASSERT_EQ(top.size(), 3U);
    EXPECT_EQ(top[0].timestamp, 101);
    EXPECT_EQ(top[1].timestamp, 104);
    EXPECT_EQ(top[2].timestamp, 102);
    EXPECT_EQ(dojo::slowest(dojo::parse_all(kLog), 50).size(), 7U);
    EXPECT_TRUE(dojo::slowest({}, 3).empty());
}

TEST(ServicesAtLeast, DistinctAndSorted) {
    const auto entries = dojo::parse_all(kLog);
    EXPECT_EQ(dojo::services_at_least(entries, Level::Warn),
              (std::vector<std::string>{"auth", "billing", "search"}));
    EXPECT_EQ(dojo::services_at_least(entries, Level::Debug).size(), 3U);
    const std::vector<Entry> quiet{{1, Level::Info, "a", 1, ""}, {2, Level::Error, "b", 1, ""}};
    EXPECT_EQ(dojo::services_at_least(quiet, Level::Warn), (std::vector<std::string>{"b"}));
}

TEST(MovingAverage, SlidesAWindow) {
    EXPECT_EQ(dojo::moving_average({1, 2, 3, 4}, 2), (std::vector<double>{1.5, 2.5, 3.5}));
    EXPECT_EQ(dojo::moving_average({5, 5, 8}, 3), (std::vector<double>{6.0}));
    EXPECT_EQ(dojo::moving_average({4, 8}, 1), (std::vector<double>{4.0, 8.0}));
}

TEST(MovingAverage, WindowThatDoesNotFit) {
    EXPECT_TRUE(dojo::moving_average({1, 2}, 3).empty());
    EXPECT_TRUE(dojo::moving_average({1, 2}, 0).empty());
    EXPECT_TRUE(dojo::moving_average({}, 1).empty());
}
