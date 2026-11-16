#include <gtest/gtest.h>

#include "inventory.hpp"

TEST(Visible, AddsAndSells) {
    dojo::Inventory stock;
    ASSERT_TRUE(stock.add("A-1", "kettle", 5, 2999));
    EXPECT_TRUE(stock.sell("A-1", 2));
    EXPECT_EQ(stock.find("A-1")->quantity, 3);
}
