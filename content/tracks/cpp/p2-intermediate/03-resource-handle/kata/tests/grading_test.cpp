#include <gtest/gtest.h>

#include <stdexcept>
#include <type_traits>
#include <utility>
#include <vector>

#include "buffer.hpp"

using dojo::Buffer;
using dojo::IntList;

namespace {

Buffer counting(std::size_t size) {
    Buffer buffer(size);
    for (std::size_t i = 0; i < size; ++i) {
        buffer[i] = static_cast<int>(i);
    }
    return buffer;
}

}  // namespace

TEST(Buffer, DefaultIsEmpty) {
    const Buffer buffer;
    EXPECT_TRUE(buffer.empty());
    EXPECT_EQ(buffer.data(), nullptr);
}

TEST(Buffer, ZeroSizeOwnsNothing) {
    const Buffer buffer(0);
    EXPECT_TRUE(buffer.empty());
}

TEST(Buffer, AtChecksTheIndex) {
    const Buffer buffer = counting(4);
    EXPECT_EQ(buffer.at(3), 3);
    EXPECT_THROW((void)buffer.at(4), std::out_of_range);
    EXPECT_THROW((void)Buffer().at(0), std::out_of_range);
}

TEST(Buffer, CopyIsDeep) {
    Buffer original = counting(3);
    Buffer copy = original;
    copy[0] = 42;
    EXPECT_EQ(original[0], 0);
    EXPECT_EQ(copy.size(), 3U);
    EXPECT_NE(copy.data(), original.data());
}

TEST(Buffer, CopyAssignmentReplacesAndFrees) {
    Buffer target = counting(10);
    const Buffer source = counting(2);
    target = source;
    EXPECT_EQ(target.size(), 2U);
    EXPECT_EQ(target[1], 1);
    EXPECT_NE(target.data(), source.data());
}

TEST(Buffer, SelfAssignmentKeepsTheData) {
    Buffer buffer = counting(3);
    Buffer& alias = buffer;
    buffer = alias;
    ASSERT_EQ(buffer.size(), 3U);
    EXPECT_EQ(buffer[2], 2);
}

TEST(Buffer, MoveStealsTheMemory) {
    Buffer source = counting(5);
    const int* memory = source.data();
    Buffer target = std::move(source);
    EXPECT_EQ(target.data(), memory);
    EXPECT_EQ(target.size(), 5U);
    EXPECT_TRUE(source.empty());  // NOLINT(bugprone-use-after-move): the moved-from state is tested
    EXPECT_EQ(source.data(), nullptr);
}

TEST(Buffer, MoveAssignmentFreesTheOldMemory) {
    Buffer target = counting(100);
    Buffer source = counting(2);
    target = std::move(source);
    EXPECT_EQ(target.size(), 2U);
    EXPECT_TRUE(source.empty());  // NOLINT(bugprone-use-after-move)
}

TEST(Buffer, MovesAreNoexcept) {
    EXPECT_TRUE(std::is_nothrow_move_constructible_v<Buffer>);
    EXPECT_TRUE(std::is_nothrow_move_assignable_v<Buffer>);
}

TEST(Buffer, VectorOfBuffersGrowsByMoving) {
    std::vector<Buffer> buffers;
    for (std::size_t i = 1; i <= 50; ++i) {
        buffers.push_back(counting(i));
    }
    EXPECT_EQ(buffers[49].size(), 50U);
    EXPECT_EQ(buffers[49][49], 49);
}

TEST(Buffer, SwapExchangesWithoutCopying) {
    Buffer a = counting(2);
    Buffer b = counting(7);
    const int* a_memory = a.data();
    a.swap(b);
    EXPECT_EQ(a.size(), 7U);
    EXPECT_EQ(b.size(), 2U);
    EXPECT_EQ(b.data(), a_memory);
}

TEST(IntList, PushAndPopFromTheFront) {
    IntList list;
    EXPECT_TRUE(list.empty());
    list.push_front(1);
    list.push_front(2);
    EXPECT_EQ(list.size(), 2U);
    EXPECT_EQ(list.front(), 2);
    EXPECT_EQ(list.pop_front(), 2);
    EXPECT_EQ(list.pop_front(), 1);
    EXPECT_TRUE(list.empty());
    EXPECT_EQ(list.size(), 0U);
}

TEST(IntList, EmptyListThrows) {
    IntList list;
    EXPECT_THROW((void)list.front(), std::out_of_range);
    EXPECT_THROW((void)list.pop_front(), std::out_of_range);
}

TEST(IntList, ReverseRelinksTheNodes) {
    IntList list;
    for (int i = 1; i <= 4; ++i) {
        list.push_front(i);  // 4 3 2 1
    }
    list.reverse();  // 1 2 3 4
    EXPECT_EQ(list.size(), 4U);
    for (int i = 1; i <= 4; ++i) {
        EXPECT_EQ(list.pop_front(), i);
    }
    IntList empty;
    empty.reverse();
    EXPECT_TRUE(empty.empty());
}

TEST(IntList, MovingTransfersTheNodes) {
    IntList list;
    list.push_front(7);
    IntList other = std::move(list);
    EXPECT_EQ(other.front(), 7);
}

TEST(IntList, LongListIsFreedWithoutOverflowingTheStack) {
    IntList list;
    for (int i = 0; i < 5'000'000; ++i) {
        list.push_front(i);
    }
    EXPECT_EQ(list.size(), 5'000'000U);
}
