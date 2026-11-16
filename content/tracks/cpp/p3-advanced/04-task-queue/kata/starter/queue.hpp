#pragma once

#include <condition_variable>
#include <cstddef>
#include <deque>
#include <mutex>
#include <optional>
#include <stdexcept>
#include <utility>

namespace dojo {

// A queue that threads share: pop() waits for a value, push() waits for room when the queue
// has a capacity, and close() lets every waiting thread go.
template <typename T>
class BlockingQueue {
public:
    // capacity 0 means unbounded.
    explicit BlockingQueue(std::size_t capacity = 0) : capacity_(capacity) {}

    BlockingQueue(const BlockingQueue&) = delete;
    BlockingQueue& operator=(const BlockingQueue&) = delete;

    // Adds value at the back, waiting while the queue is full. False (value unused) once the
    // queue is closed, also when it closes while push is waiting.
    bool push(T value) {
        (void)value;
        throw std::logic_error("todo: write BlockingQueue::push");
    }

    // Waits for a value and removes it. Nothing once the queue is closed and empty: values
    // pushed before close() are still handed out.
    std::optional<T> pop() { throw std::logic_error("todo: write BlockingQueue::pop"); }

    // Removes a value if there is one, without waiting.
    std::optional<T> try_pop() { throw std::logic_error("todo: write BlockingQueue::try_pop"); }

    // No more pushes; wakes every thread waiting in push or pop. Calling it again is fine.
    void close() { throw std::logic_error("todo: write BlockingQueue::close"); }

    bool closed() const { throw std::logic_error("todo: write BlockingQueue::closed"); }
    std::size_t size() const { throw std::logic_error("todo: write BlockingQueue::size"); }

private:
    mutable std::mutex mutex_;
    std::condition_variable not_empty_;
    std::condition_variable not_full_;
    std::deque<T> items_;
    std::size_t capacity_;
    bool closed_ = false;
};

}  // namespace dojo
