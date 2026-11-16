#pragma once

#include <array>
#include <concepts>
#include <cstddef>
#include <functional>
#include <optional>
#include <ranges>
#include <stdexcept>
#include <type_traits>
#include <utility>

namespace dojo {

// A number you can add up: any integral or floating-point type, but not bool.
// todo: write the real constraint (it accepts every type for now).
template <typename T>
concept Number = true;

// The sum of the values; T{} for an empty range. Only for ranges of Number.
template <std::ranges::input_range R>
    requires Number<std::ranges::range_value_t<R>>
std::ranges::range_value_t<R> sum(R&& values) {
    (void)values;
    throw std::logic_error("todo: write sum");
}

// The mean as a double, or nothing for an empty range.
template <std::ranges::input_range R>
    requires Number<std::ranges::range_value_t<R>>
std::optional<double> mean(R&& values) {
    (void)values;
    throw std::logic_error("todo: write mean");
}

// A copy of the element whose projection is largest (the first one on a tie), or nothing
// for an empty range. max_by(people, &Person::age) compares ages; with no projection, the
// elements themselves.
template <std::ranges::forward_range R, typename Proj = std::identity>
std::optional<std::ranges::range_value_t<R>> max_by(R&& range, Proj proj = {}) {
    (void)range;
    (void)proj;
    throw std::logic_error("todo: write max_by");
}

// A queue of at most N values that never allocates: when it is full, push refuses and
// push_overwrite drops the oldest value. Works with move-only types such as unique_ptr.
template <typename T, std::size_t N>
    requires(N > 0)
class RingBuffer {
public:
    static constexpr std::size_t capacity() { return N; }

    std::size_t size() const { return size_; }
    bool empty() const { return size_ == 0; }
    bool full() const { return size_ == N; }

    // Adds value at the back. False (and value is not used) when the buffer is full.
    bool push(T value) {
        (void)value;
        throw std::logic_error("todo: write RingBuffer::push");
    }

    // Adds value at the back, dropping the oldest value first when the buffer is full.
    void push_overwrite(T value) {
        (void)value;
        throw std::logic_error("todo: write RingBuffer::push_overwrite");
    }

    // Removes and returns the oldest value, or nothing when the buffer is empty.
    std::optional<T> pop() { throw std::logic_error("todo: write RingBuffer::pop"); }

    // The oldest value; std::out_of_range when the buffer is empty.
    const T& front() const { throw std::logic_error("todo: write RingBuffer::front"); }

    // Calls f(value) for each value, oldest first.
    template <std::invocable<const T&> F>
    void for_each(F&& f) const {
        (void)f;
        throw std::logic_error("todo: write RingBuffer::for_each");
    }

private:
    std::array<std::optional<T>, N> slots_{};
    std::size_t head_ = 0;  // index of the oldest value
    std::size_t size_ = 0;
};

}  // namespace dojo
