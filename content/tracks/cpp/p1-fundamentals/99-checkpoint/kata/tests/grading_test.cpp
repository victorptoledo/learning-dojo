#include <gtest/gtest.h>

#include <limits>
#include <string>
#include <vector>

#include "inventory.hpp"

using dojo::Inventory;
using dojo::parse_cents;

TEST(ParseCents, ReadsUnitsAndDecimals) {
    EXPECT_EQ(parse_cents("12.34"), 1234);
    EXPECT_EQ(parse_cents("7"), 700);
    EXPECT_EQ(parse_cents("0.5"), 50);
    EXPECT_EQ(parse_cents("3.05"), 305);
    EXPECT_EQ(parse_cents("0"), 0);
}

TEST(ParseCents, RejectsBadText) {
    for (const char* bad :
         {"", ".", "1.", ".5", "-1", "+1", "1.234", "1,50", "abc", "1 2", "1.2x"}) {
        EXPECT_EQ(parse_cents(bad), std::nullopt) << "text: [" << bad << "]";
    }
}

TEST(ParseCents, RejectsAmountsThatDoNotFit) {
    EXPECT_EQ(parse_cents("99999999999999999999"), std::nullopt);
    EXPECT_EQ(parse_cents("92233720368547758.07"), std::numeric_limits<dojo::Cents>::max());
    EXPECT_EQ(parse_cents("92233720368547758.08"), std::nullopt);
}

TEST(Add, RejectsDuplicatesAndBadValues) {
    Inventory stock;
    EXPECT_TRUE(stock.add("A-1", "kettle", 0, 2999));
    EXPECT_FALSE(stock.add("A-1", "other", 1, 1));
    EXPECT_FALSE(stock.add("", "nameless", 1, 1));
    EXPECT_FALSE(stock.add("B-1", "", 1, 1));
    EXPECT_FALSE(stock.add("B-1", "mug", -1, 1));
    EXPECT_FALSE(stock.add("B-1", "mug", 1, -1));
    EXPECT_EQ(stock.size(), 1U);
    EXPECT_EQ(stock.find("A-1")->name, "kettle");
}

TEST(Restock, OnlyKnownSkusAndPositiveAmounts) {
    Inventory stock;
    stock.add("A-1", "kettle", 1, 100);
    EXPECT_TRUE(stock.restock("A-1", 4));
    EXPECT_FALSE(stock.restock("A-1", 0));
    EXPECT_FALSE(stock.restock("A-1", -3));
    EXPECT_FALSE(stock.restock("Z-9", 1));
    EXPECT_EQ(stock.find("A-1")->quantity, 5);
}

TEST(Sell, NeverGoesBelowZero) {
    Inventory stock;
    stock.add("A-1", "kettle", 3, 100);
    EXPECT_FALSE(stock.sell("A-1", 4));
    EXPECT_FALSE(stock.sell("A-1", 0));
    EXPECT_FALSE(stock.sell("Z-9", 1));
    EXPECT_EQ(stock.find("A-1")->quantity, 3);
    EXPECT_TRUE(stock.sell("A-1", 3));
    EXPECT_EQ(stock.find("A-1")->quantity, 0);
}

TEST(Find, ReturnsACopyOrNothing) {
    Inventory stock;
    stock.add("A-1", "kettle", 3, 100);
    auto copy = stock.find("A-1");
    ASSERT_TRUE(copy.has_value());
    EXPECT_EQ(*copy, (dojo::Item{"A-1", "kettle", 3, 100}));
    copy->quantity = 99;
    EXPECT_EQ(stock.find("A-1")->quantity, 3);
    EXPECT_FALSE(stock.find("nope").has_value());
}

TEST(TotalValue, SumsQuantityTimesPrice) {
    Inventory stock;
    EXPECT_EQ(stock.total_value(), 0);
    stock.add("A-1", "kettle", 3, 2999);
    stock.add("B-2", "mug", 10, 450);
    EXPECT_EQ(stock.total_value(), 3 * 2999 + 10 * 450);
}

TEST(LowStock, InSkuOrder) {
    Inventory stock;
    stock.add("C-3", "teapot", 1, 1);
    stock.add("A-1", "kettle", 5, 1);
    stock.add("B-2", "mug", 2, 1);
    EXPECT_EQ(stock.low_stock(2), (std::vector<std::string>{"B-2", "C-3"}));
    EXPECT_TRUE(stock.low_stock(0).empty());
}

TEST(Apply, RunsCommandsAndReportsFailedLines) {
    Inventory stock;
    const auto failed = stock.apply(
        "# opening stock\n"
        "add A-1 kettle 5 29.99\n"
        "\n"
        "add B-2 mug 10 4.5\n"
        "sell A-1 2\n"
        "sell A-1 9\n"
        "restock B-2 x\n"
        "add C-3 teapot 1\n"
        "fly A-1 2\n"
        "restock B-2 5\n");
    EXPECT_EQ(failed, (std::vector<int>{6, 7, 8, 9}));
    EXPECT_EQ(stock.find("A-1")->quantity, 3);
    EXPECT_EQ(stock.find("B-2")->quantity, 15);
    EXPECT_EQ(stock.find("B-2")->unit_price, 450);
    EXPECT_FALSE(stock.find("C-3").has_value());
}

TEST(Apply, ToleratesExtraSpacesAndWindowsLineEnds) {
    Inventory stock;
    const auto failed = stock.apply("  add   A-1  kettle 2 1.00 \r\n\r\nsell A-1 1\r\n");
    EXPECT_TRUE(failed.empty());
    EXPECT_EQ(stock.find("A-1")->quantity, 1);
}

TEST(Apply, RejectsQuantitiesThatAreNotWholeNumbers) {
    Inventory stock;
    const auto failed =
        stock.apply("add A-1 kettle 2.5 1\nadd B-1 mug -2 1\nadd C-1 cup 99999999999 1\n");
    EXPECT_EQ(failed, (std::vector<int>{1, 2, 3}));
    EXPECT_EQ(stock.size(), 0U);
}
