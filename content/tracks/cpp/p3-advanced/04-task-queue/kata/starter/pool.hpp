#pragma once

#include <atomic>
#include <cstddef>
#include <functional>
#include <future>
#include <memory>
#include <thread>
#include <type_traits>
#include <vector>

#include "queue.hpp"

namespace dojo {

// A fixed set of threads that run submitted tasks from a BlockingQueue.
class WorkerPool {
public:
    // Starts `threads` workers (at least one).
    explicit WorkerPool(std::size_t threads);
    WorkerPool(const WorkerPool&) = delete;
    WorkerPool& operator=(const WorkerPool&) = delete;
    // Calls shutdown().
    ~WorkerPool();

    // Queues a task. False once the pool is shut down. A task that throws is counted in
    // failed() and the worker goes on with the next task.
    bool submit(std::function<void()> task);

    // Queues f and returns a future for its result (or for the exception it throws).
    // The future is ready with std::future_error (broken_promise) if the pool is shut down.
    template <typename F>
    std::future<std::invoke_result_t<F>> async(F f) {
        // std::function needs a copyable callable, and packaged_task is move-only, so it
        // lives in a shared_ptr.
        using Result = std::invoke_result_t<F>;
        auto task = std::make_shared<std::packaged_task<Result()>>(std::move(f));
        auto future = task->get_future();
        submit([task] { (*task)(); });
        return future;
    }

    // Stops taking tasks, lets the workers finish every task already queued, and joins them.
    // Calling it again is fine.
    void shutdown();

    std::size_t completed() const { return completed_.load(); }
    std::size_t failed() const { return failed_.load(); }

private:
    void work();

    BlockingQueue<std::function<void()>> queue_;
    std::atomic<std::size_t> completed_{0};
    std::atomic<std::size_t> failed_{0};
    std::vector<std::jthread> workers_;  // last: the threads start after the other members
};

// The sum of the values, split into `threads` chunks added up on their own threads
// (0 threads counts as 1). Each thread writes only its own partial sum: no locks needed.
long long parallel_sum(const std::vector<int>& values, std::size_t threads);

}  // namespace dojo
