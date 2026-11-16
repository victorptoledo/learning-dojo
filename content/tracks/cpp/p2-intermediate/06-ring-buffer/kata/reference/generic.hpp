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
template <typename T>
concept Number = (std::integral<T> || std::floating_point<T>) && !std::same_as<T, bool>;

// The sum of the values; T{} for an empty range. Only for ranges of Number.
template <std::ranges::input_range R>
    requires Number<std::ranges::range_value_t<R>>
std::ranges::range_value_t<R> sum(R&& values) {
    std::ranges::range_value_t<R> total{};
    for (const auto& value : values) {
        total += value;
    }
    return total;
}

// The mean as a double, or nothing for an empty range.
template <std::ranges::input_range R>
    requires Number<std::ranges::range_value_t<R>>
std::optional<double> mean(R&& values) {
    double total = 0;
    std::size_t count = 0;
    for (const auto& value : values) {
        total += static_cast<double>(value);
        ++count;
    }
    if (count == 0) {
        return std::nullopt;
    }
    return total / static_cast<double>(count);
}

// A copy of the element whose projection is largest (the first one on a tie), or nothing
// for an empty range. max_by(people, &Person::age) compares ages; with no projection, the
// elements themselves.
template <std::ranges::forward_range R, typename Proj = std::identity>
std::optional<std::ranges::range_value_t<R>> max_by(R&& range, Proj proj = {}) {
    auto best = std::ranges::begin(range);
    const auto end = std::ranges::end(range);
    if (best == end) {
        return std::nullopt;
    }
    for (auto it = std::ranges::next(best); it != end; ++it) {
        if (std::invoke(proj, *best) < std::invoke(proj, *it)) {
            best = it;
        }
    }
    return *best;
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
        if (full()) {
            return false;
        }
        slots_[(head_ + size_) % N].emplace(std::move(value));
        ++size_;
        return true;
    }

    // Adds value at the back, dropping the oldest value first when the buffer is full.
    void push_overwrite(T value) {
        if (full()) {
            (void)pop();
        }
        push(std::move(value));
    }

    // Removes and returns the oldest value, or nothing when the buffer is empty.
    std::optional<T> pop() {
        if (empty()) {
            return std::nullopt;
        }
        std::optional<T> oldest = std::move(slots_[head_]);
        slots_[head_].reset();
        head_ = (head_ + 1) % N;
        --size_;
        return oldest;
    }

    // The oldest value; std::out_of_range when the buffer is empty.
    const T& front() const {
        if (empty()) {
            throw std::out_of_range("RingBuffer::front: empty buffer");
        }
        return *slots_[head_];
    }

    // Calls f(value) for each value, oldest first.
    template <std::invocable<const T&> F>
    void for_each(F&& f) const {
        for (std::size_t i = 0; i < size_; ++i) {
            std::invoke(f, *slots_[(head_ + i) % N]);
        }
    }

private:
    std::array<std::optional<T>, N> slots_{};
    std::size_t head_ = 0;  // index of the oldest value
    std::size_t size_ = 0;
};

}  // namespace dojo
