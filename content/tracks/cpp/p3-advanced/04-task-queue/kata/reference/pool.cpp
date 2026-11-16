#include "pool.hpp"

#include <algorithm>
#include <numeric>

namespace dojo {

WorkerPool::WorkerPool(std::size_t threads) {
    const std::size_t count = std::max<std::size_t>(threads, 1);
    workers_.reserve(count);
    for (std::size_t i = 0; i < count; ++i) {
        workers_.emplace_back([this] { work(); });
    }
}

WorkerPool::~WorkerPool() {
    shutdown();
}

bool WorkerPool::submit(std::function<void()> task) {
    return queue_.push(std::move(task));
}

void WorkerPool::shutdown() {
    queue_.close();
    for (auto& worker : workers_) {
        if (worker.joinable()) {
            worker.join();
        }
    }
}

void WorkerPool::work() {
    while (auto task = queue_.pop()) {
        try {
            (*task)();
            ++completed_;
        } catch (...) {
            ++failed_;
        }
    }
}

long long parallel_sum(const std::vector<int>& values, std::size_t threads) {
    const std::size_t count =
        std::clamp<std::size_t>(threads, 1, std::max<std::size_t>(values.size(), 1));
    const std::size_t chunk = (values.size() + count - 1) / count;
    std::vector<long long> partial(count, 0);
    {
        std::vector<std::jthread> workers;
        workers.reserve(count);
        for (std::size_t i = 0; i < count; ++i) {
            workers.emplace_back([&values, &partial, chunk, i] {
                const auto begin = std::min(values.size(), i * chunk);
                const auto end = std::min(values.size(), begin + chunk);
                partial[i] =
                    std::accumulate(values.begin() + static_cast<std::ptrdiff_t>(begin),
                                    values.begin() + static_cast<std::ptrdiff_t>(end), 0LL);
            });
        }
    }  // the jthreads join here
    return std::accumulate(partial.begin(), partial.end(), 0LL);
}

}  // namespace dojo
