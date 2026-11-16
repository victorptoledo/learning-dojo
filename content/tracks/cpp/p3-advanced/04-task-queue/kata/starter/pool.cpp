#include "pool.hpp"

#include <stdexcept>

namespace dojo {

WorkerPool::WorkerPool(std::size_t threads) {
    (void)threads;
    throw std::logic_error("todo: write the WorkerPool constructor");
}

WorkerPool::~WorkerPool() {
    // todo: call shutdown() (a destructor must not throw, so this one is empty for now)
}

bool WorkerPool::submit(std::function<void()> task) {
    (void)task;
    throw std::logic_error("todo: write WorkerPool::submit");
}

void WorkerPool::shutdown() {
    throw std::logic_error("todo: write WorkerPool::shutdown");
}

void WorkerPool::work() {
    throw std::logic_error("todo: write WorkerPool::work");
}

long long parallel_sum(const std::vector<int>& values, std::size_t threads) {
    (void)values;
    (void)threads;
    throw std::logic_error("todo: write parallel_sum");
}

}  // namespace dojo
