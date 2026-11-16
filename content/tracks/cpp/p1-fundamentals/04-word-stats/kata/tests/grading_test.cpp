#include <gtest/gtest.h>

#include <string>
#include <vector>

#include "words.hpp"

using Strings = std::vector<std::string>;

namespace {
const char* const kText = "The cat and the hat. THE END, don't stop!";
}

TEST(Words, KeepsDigitsAndInnerApostrophes) {
    EXPECT_EQ(dojo::words("Room 101 isn't 'quoted' rock'n'roll"),
              (Strings{"room", "101", "isn't", "quoted", "rock'n'roll"}));
}

TEST(Words, EmptyOrPunctuationOnlyHasNoWords) {
    EXPECT_TRUE(dojo::words("").empty());
    EXPECT_TRUE(dojo::words(" ,.;!? - _ '' ").empty());
}

TEST(Words, SeparatorsSplitWords) {
    EXPECT_EQ(dojo::words("snake_case-and\ttabs\nlines"),
              (Strings{"snake", "case", "and", "tabs", "lines"}));
}

TEST(Count, CountsEachWord) {
    const auto counts = dojo::count({"a", "b", "a", "c", "a"});
    EXPECT_EQ(counts.size(), 3U);
    EXPECT_EQ(counts.at("a"), 3);
    EXPECT_EQ(counts.at("b"), 1);
    EXPECT_TRUE(dojo::count({}).empty());
}

TEST(Top, MostFrequentFirstThenAlphabetical) {
    const std::vector<dojo::WordCount> expected{{"the", 3}, {"and", 1}, {"cat", 1}};
    EXPECT_EQ(dojo::top(kText, 3), expected);
}

TEST(Top, FewerWordsThanAsked) {
    const std::vector<dojo::WordCount> expected{{"b", 2}, {"a", 1}};
    EXPECT_EQ(dojo::top("b a b", 10), expected);
}

TEST(Top, ZeroAsksForNothing) {
    EXPECT_TRUE(dojo::top(kText, 0).empty());
    EXPECT_TRUE(dojo::top("", 3).empty());
}

TEST(FirstSeen, KeepsFirstAppearanceOrder) {
    EXPECT_EQ(dojo::first_seen(kText),
              (Strings{"the", "cat", "and", "hat", "end", "don't", "stop"}));
}

TEST(ByLength, GroupsDistinctSortedWords) {
    const auto groups = dojo::by_length(kText);
    ASSERT_EQ(groups.size(), 3U);
    EXPECT_EQ(groups.at(3), (Strings{"and", "cat", "end", "hat", "the"}));
    EXPECT_EQ(groups.at(4), (Strings{"stop"}));
    EXPECT_EQ(groups.at(5), (Strings{"don't"}));
}

TEST(ByLength, EmptyText) {
    EXPECT_TRUE(dojo::by_length("").empty());
}
