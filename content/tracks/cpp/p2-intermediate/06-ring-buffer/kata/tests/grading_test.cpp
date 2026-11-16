#include <gtest/gtest.h>

#include <array>
#include <list>
#include <memory>
#include <string>
#include <vector>

#include "generic.hpp"

namespace {

template <typename R>
concept Summable = requires(R range) { dojo::sum(range); };

struct Person {
    std::string name;
    int age = 0;
};

}  // namespace

TEST(Number, AcceptsIntegersAndFloatingPoint) {
    EXPECT_TRUE(dojo::Number<int>);
    EXPECT_TRUE(dojo::Number<long long>);
    EXPECT_TRUE(dojo::Number<unsigned char>);
    EXPECT_TRUE(dojo::Number<double>);
    EXPECT_TRUE(dojo::Number<float>);
}

TEST(Number, RejectsEverythingElse) {
    EXPECT_FALSE(dojo::Number<bool>);
    EXPECT_FALSE(dojo::Number<std::string>);
    EXPECT_FALSE(dojo::Number<int*>);
    EXPECT_FALSE(dojo::Number<Person>);
}

TEST(Sum, OnlyForRangesOfNumbers) {
    EXPECT_TRUE(Summable<std::vector<int>>);
    EXPECT_TRUE((Summable<std::array<double, 3>>));
    EXPECT_FALSE(Summable<std::vector<std::string>>);
    EXPECT_FALSE(Summable<std::vector<bool>>);
}

TEST(Sum, AddsAnyRange) {
    EXPECT_DOUBLE_EQ(dojo::sum(std::list<double>{0.5, 1.25}), 1.75);
    EXPECT_EQ(dojo::sum(std::vector<int>{}), 0);
    EXPECT_EQ(dojo::sum(std::views::iota(1, 101)), 5050);
    const std::array<long long, 2> big{4'000'000'000LL, 5'000'000'000LL};
    EXPECT_EQ(dojo::sum(big), 9'000'000'000LL);
}

TEST(Mean, AveragesAsDouble) {
    EXPECT_DOUBLE_EQ(*dojo::mean(std::vector<int>{1, 2}), 1.5);
    EXPECT_EQ(dojo::mean(std::vector<double>{}), std::nullopt);
}

TEST(MaxBy, UsesTheProjection) {
    const std::vector<Person> people{{"ada", 36}, {"alan", 41}, {"grace", 41}, {"linus", 21}};
    const auto oldest = dojo::max_by(people, &Person::age);
    ASSERT_TRUE(oldest.has_value());
    EXPECT_EQ(oldest->name, "alan");
    const auto longest = dojo::max_by(people, [](const Person& p) { return p.name.size(); });
    EXPECT_EQ(longest->name, "grace");
}

TEST(MaxBy, WithoutProjectionAndEmpty) {
    EXPECT_EQ(dojo::max_by(std::vector<int>{3, 9, 2}), 9);
    EXPECT_EQ(dojo::max_by(std::vector<int>{}), std::nullopt);
}

TEST(RingBuffer, CapacityIsKnownAtCompileTime) {
    static_assert(dojo::RingBuffer<int, 4>::capacity() == 4);
    EXPECT_EQ((dojo::RingBuffer<int, 4>::capacity()), 4U);
}

TEST(RingBuffer, PushRefusesWhenFull) {
    dojo::RingBuffer<int, 2> buffer;
    EXPECT_TRUE(buffer.empty());
    EXPECT_TRUE(buffer.push(1));
    EXPECT_TRUE(buffer.push(2));
    EXPECT_TRUE(buffer.full());
    EXPECT_FALSE(buffer.push(3));
    EXPECT_EQ(buffer.size(), 2U);
    EXPECT_EQ(buffer.front(), 1);
}

TEST(RingBuffer, PopIsOldestFirstAndWrapsAround) {
    dojo::RingBuffer<int, 3> buffer;
    for (int round = 0; round < 5; ++round) {
        buffer.push(round * 10);
        buffer.push(round * 10 + 1);
        EXPECT_EQ(buffer.pop(), round * 10);
        EXPECT_EQ(buffer.pop(), round * 10 + 1);
    }
    EXPECT_EQ(buffer.pop(), std::nullopt);
}

TEST(RingBuffer, PushOverwriteDropsTheOldest) {
    dojo::RingBuffer<std::string, 3> buffer;
    for (const char* word : {"a", "b", "c", "d", "e"}) {
        buffer.push_overwrite(word);
    }
    EXPECT_EQ(buffer.size(), 3U);
    std::vector<std::string> seen;
    buffer.for_each([&](const std::string& value) { seen.push_back(value); });
    EXPECT_EQ(seen, (std::vector<std::string>{"c", "d", "e"}));
}

TEST(RingBuffer, FrontOfEmptyThrows) {
    const dojo::RingBuffer<int, 1> buffer;
    EXPECT_THROW((void)buffer.front(), std::out_of_range);
}

TEST(RingBuffer, HoldsMoveOnlyValues) {
    dojo::RingBuffer<std::unique_ptr<int>, 2> buffer;
    EXPECT_TRUE(buffer.push(std::make_unique<int>(1)));
    buffer.push_overwrite(std::make_unique<int>(2));
    buffer.push_overwrite(std::make_unique<int>(3));
    const auto oldest = buffer.pop();
    ASSERT_TRUE(oldest.has_value());
    EXPECT_EQ(**oldest, 2);
    EXPECT_EQ(*buffer.front(), 3);
}

TEST(RingBuffer, ForEachVisitsOldestToNewest) {
    dojo::RingBuffer<int, 4> buffer;
    buffer.push(1);
    buffer.push(2);
    buffer.pop();
    buffer.push(3);
    buffer.push(4);
    buffer.push(5);
    int total = 0;
    std::vector<int> order;
    buffer.for_each([&](int value) {
        total += value;
        order.push_back(value);
    });
    EXPECT_EQ(order, (std::vector<int>{2, 3, 4, 5}));
    EXPECT_EQ(total, 14);
}
