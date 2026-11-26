#include <gtest/gtest.h>

#include <string>

#include "lru.hpp"

TEST(Visible, EvictsTheLeastRecentlyUsed) {
    dojo::LruCache<std::string, int> cache(2);
    cache.put("a", 1);
    cache.put("b", 2);
    ASSERT_NE(cache.get("a"), nullptr);  // "a" is now the most recent
    cache.put("c", 3);                   // so "b" goes
    EXPECT_EQ(cache.get("b"), nullptr);
    EXPECT_EQ(*cache.get("a"), 1);
}
