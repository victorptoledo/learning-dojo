#include <gtest/gtest.h>

#include <chrono>
#include <cstdint>
#include <memory>
#include <random>
#include <stdexcept>
#include <string>
#include <utility>
#include <vector>

#include "lru.hpp"

using dojo::LruCache;

namespace {

struct Point {
    int x = 0;
    int y = 0;
    bool operator==(const Point&) const = default;
};

struct PointHash {
    std::size_t operator()(const Point& p) const noexcept {
        return std::hash<long long>{}((static_cast<long long>(p.x) << 32) ^ p.y);
    }
};

}  // namespace

TEST(Lru, ZeroCapacityIsRejected) {
    EXPECT_THROW((LruCache<int, int>(0)), std::invalid_argument);
}

TEST(Lru, GetCountsHitsAndMisses) {
    LruCache<int, std::string> cache(3);
    cache.put(1, "one");
    EXPECT_EQ(*cache.get(1), "one");
    EXPECT_EQ(cache.get(2), nullptr);
    EXPECT_EQ(cache.get(3), nullptr);
    EXPECT_EQ(cache.hits(), 1U);
    EXPECT_EQ(cache.misses(), 2U);
}

TEST(Lru, PutReplacesWithoutEvicting) {
    std::vector<int> evicted;
    LruCache<int, int> cache(2, [&](const int& key, const int&) { evicted.push_back(key); });
    cache.put(1, 10);
    cache.put(2, 20);
    cache.put(1, 11);
    EXPECT_EQ(cache.size(), 2U);
    EXPECT_TRUE(evicted.empty());
    EXPECT_EQ(*cache.peek(1), 11);
    cache.put(3, 30);  // 2 is now the least recent
    EXPECT_EQ(evicted, (std::vector<int>{2}));
}

TEST(Lru, KeysAreMostRecentFirst) {
    LruCache<char, int> cache(3);
    cache.put('a', 1);
    cache.put('b', 2);
    cache.put('c', 3);
    (void)cache.get('a');
    EXPECT_EQ(cache.keys(), (std::vector<char>{'a', 'c', 'b'}));
    cache.put('d', 4);
    EXPECT_EQ(cache.keys(), (std::vector<char>{'d', 'a', 'c'}));
}

TEST(Lru, PeekChangesNothing) {
    LruCache<int, int> cache(2);
    cache.put(1, 1);
    cache.put(2, 2);
    EXPECT_EQ(*cache.peek(1), 1);
    EXPECT_EQ(cache.peek(9), nullptr);
    cache.put(3, 3);  // 1 is still the least recent: peek did not touch it
    EXPECT_EQ(cache.peek(1), nullptr);
    EXPECT_EQ(cache.hits() + cache.misses(), 0U);
}

TEST(Lru, EvictionCallbackSeesTheEntry) {
    std::vector<std::pair<std::string, int>> evicted;
    LruCache<std::string, int> cache(
        1, [&](const std::string& key, const int& value) { evicted.emplace_back(key, value); });
    cache.put("a", 1);
    cache.put("b", 2);
    cache.put("c", 3);
    EXPECT_EQ(evicted, (std::vector<std::pair<std::string, int>>{{"a", 1}, {"b", 2}}));
}

TEST(Lru, EraseRemovesWithoutCallback) {
    int evictions = 0;
    LruCache<int, int> cache(2, [&](const int&, const int&) { ++evictions; });
    cache.put(1, 1);
    EXPECT_TRUE(cache.erase(1));
    EXPECT_FALSE(cache.erase(1));
    EXPECT_EQ(cache.size(), 0U);
    cache.put(2, 2);
    cache.put(3, 3);
    EXPECT_EQ(evictions, 0);
}

TEST(Lru, GetReturnsAPointerYouCanWriteThrough) {
    LruCache<int, std::vector<int>> cache(2);
    cache.put(1, {});
    cache.get(1)->push_back(42);
    EXPECT_EQ(cache.peek(1)->size(), 1U);
}

TEST(Lru, CustomHashAndMoveOnlyValues) {
    LruCache<Point, std::unique_ptr<int>, PointHash> cache(2);
    cache.put({1, 2}, std::make_unique<int>(12));
    cache.put({3, 4}, std::make_unique<int>(34));
    EXPECT_EQ(**cache.get({1, 2}), 12);
    cache.put({5, 6}, std::make_unique<int>(56));
    EXPECT_EQ(cache.get({3, 4}), nullptr);
    EXPECT_EQ(cache.keys(), (std::vector<Point>{{5, 6}, {1, 2}}));
}

TEST(Lru, LargeWorkloadIsConstantTimePerOperation) {
    constexpr std::size_t kCapacity = 50'000;
    LruCache<std::uint32_t, std::uint32_t> cache(kCapacity);
    std::mt19937 random(7);
    std::uniform_int_distribution<std::uint32_t> keys(0, 120'000);
    const auto started = std::chrono::steady_clock::now();
    for (int i = 0; i < 400'000; ++i) {
        const auto key = keys(random);
        if (i % 3 == 0) {
            cache.put(key, key * 2);
        } else if (const auto* value = cache.get(key)) {
            ASSERT_EQ(*value, key * 2);
        }
    }
    const auto elapsed = std::chrono::steady_clock::now() - started;
    EXPECT_LE(cache.size(), kCapacity);
    EXPECT_EQ(cache.hits() + cache.misses(), 400'000U - 133'334U);
    EXPECT_LT(elapsed, std::chrono::seconds(4)) << "get and put must not scan the entries";
}
