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
        std::unique_lock lock(mutex_);
        not_full_.wait(lock,
                       [this] { return closed_ || capacity_ == 0 || items_.size() < capacity_; });
        if (closed_) {
            return false;
        }
        items_.push_back(std::move(value));
        lock.unlock();
        not_empty_.notify_one();
        return true;
    }

    // Waits for a value and removes it. Nothing once the queue is closed and empty: values
    // pushed before close() are still handed out.
    std::optional<T> pop() {
        std::unique_lock lock(mutex_);
        not_empty_.wait(lock, [this] { return closed_ || !items_.empty(); });
        return take(lock);
    }

    // Removes a value if there is one, without waiting.
    std::optional<T> try_pop() {
        std::unique_lock lock(mutex_);
        return take(lock);
    }

    // No more pushes; wakes every thread waiting in push or pop. Calling it again is fine.
    void close() {
        {
            const std::lock_guard lock(mutex_);
            closed_ = true;
        }
        not_empty_.notify_all();
        not_full_.notify_all();
    }

    bool closed() const {
        const std::lock_guard lock(mutex_);
        return closed_;
    }

    std::size_t size() const {
        const std::lock_guard lock(mutex_);
        return items_.size();
    }

private:
    // Called with the lock held; releases it before waking a producer.
    std::optional<T> take(std::unique_lock<std::mutex>& lock) {
        if (items_.empty()) {
            return std::nullopt;
        }
        std::optional<T> value(std::move(items_.front()));
        items_.pop_front();
        lock.unlock();
        not_full_.notify_one();
        return value;
    }

    mutable std::mutex mutex_;
    std::condition_variable not_empty_;
    std::condition_variable not_full_;
    std::deque<T> items_;
    std::size_t capacity_;
    bool closed_ = false;
};

}  // namespace dojo
