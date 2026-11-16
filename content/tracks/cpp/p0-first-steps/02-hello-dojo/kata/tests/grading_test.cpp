#include <gtest/gtest.h>

#include "greet.hpp"

TEST(Greet, UsesTheName) {
    EXPECT_EQ(dojo::greet("Bjarne"), "Hello, Bjarne!");
}

TEST(Greet, TrimsSurroundingSpaces) {
    EXPECT_EQ(dojo::greet("  Grace \t"), "Hello, Grace!");
}

TEST(Greet, BlankNameGreetsAStranger) {
    EXPECT_EQ(dojo::greet(""), "Hello, stranger!");
    EXPECT_EQ(dojo::greet("   "), "Hello, stranger!");
}

TEST(Greet, KeepsInnerSpaces) {
    EXPECT_EQ(dojo::greet("Ada Lovelace"), "Hello, Ada Lovelace!");
}

TEST(Initials, TakesTheFirstLetterOfEachWord) {
    EXPECT_EQ(dojo::initials("grace brewster hopper"), "GBH");
}

TEST(Initials, IgnoresExtraSpaces) {
    EXPECT_EQ(dojo::initials("  alan   mathison turing "), "AMT");
}

TEST(Initials, EmptyTextHasNoInitials) {
    EXPECT_EQ(dojo::initials(""), "");
    EXPECT_EQ(dojo::initials("   "), "");
}

TEST(Initials, SingleWord) {
    EXPECT_EQ(dojo::initials("linus"), "L");
}
