#include <gtest/gtest.h>

#include <atomic>
#include <chrono>
#include <future>
#include <memory>
#include <numeric>
#include <optional>
#include <stdexcept>
#include <string>
#include <thread>
#include <vector>

#include "pool.hpp"

using namespace std::chrono_literals;
using dojo::BlockingQueue;
using dojo::WorkerPool;

TEST(Queue, TryPopOnEmptyReturnsNothing) {
    BlockingQueue<int> queue;
    EXPECT_EQ(queue.try_pop(), std::nullopt);
    EXPECT_EQ(queue.size(), 0U);
}

TEST(Queue, PopWaitsForAPush) {
    BlockingQueue<std::string> queue;
    std::optional<std::string> got;
    std::thread consumer([&] { got = queue.pop(); });
    std::this_thread::sleep_for(50ms);
    queue.push("hello");
    consumer.join();
    EXPECT_EQ(got, "hello");
}

TEST(Queue, CloseWakesWaitingPoppers) {
    BlockingQueue<int> queue;
    std::vector<std::optional<int>> results(3, 7);
    std::vector<std::thread> waiting;
    for (std::size_t i = 0; i < results.size(); ++i) {
        waiting.emplace_back([&queue, &results, i] { results[i] = queue.pop(); });
    }
    std::this_thread::sleep_for(50ms);
    queue.close();
    for (auto& thread : waiting) {
        thread.join();
    }
    for (const auto& result : results) {
        EXPECT_EQ(result, std::nullopt);
    }
    EXPECT_TRUE(queue.closed());
}

TEST(Queue, ClosedQueueStillHandsOutWhatItHas) {
    BlockingQueue<int> queue;
    queue.push(1);
    queue.push(2);
    queue.close();
    queue.close();
    EXPECT_FALSE(queue.push(3));
    EXPECT_EQ(queue.pop(), 1);
    EXPECT_EQ(queue.pop(), 2);
    EXPECT_EQ(queue.pop(), std::nullopt);
}

TEST(Queue, BoundedPushWaitsForRoom) {
    BlockingQueue<int> queue(1);
    ASSERT_TRUE(queue.push(1));
    std::atomic<bool> pushed{false};
    std::thread producer([&] {
        queue.push(2);
        pushed = true;
    });
    std::this_thread::sleep_for(50ms);
    EXPECT_FALSE(pushed.load());
    EXPECT_EQ(queue.pop(), 1);
    producer.join();
    EXPECT_TRUE(pushed.load());
    EXPECT_EQ(queue.pop(), 2);
}

TEST(Queue, CloseReleasesABlockedPush) {
    BlockingQueue<int> queue(1);
    queue.push(1);
    std::optional<bool> accepted;
    std::thread producer([&] { accepted = queue.push(2); });
    std::this_thread::sleep_for(50ms);
    queue.close();
    producer.join();
    EXPECT_EQ(accepted, false);
}

TEST(Queue, ManyProducersAndConsumersLoseNothing) {
    BlockingQueue<int> queue(16);
    constexpr int kPerProducer = 2000;
    std::atomic<long long> total{0};
    std::vector<std::thread> consumers;
    for (int c = 0; c < 3; ++c) {
        consumers.emplace_back([&] {
            while (auto value = queue.pop()) {
                total += *value;
            }
        });
    }
    std::vector<std::thread> producers;
    for (int p = 0; p < 3; ++p) {
        producers.emplace_back([&] {
            for (int i = 1; i <= kPerProducer; ++i) {
                queue.push(i);
            }
        });
    }
    for (auto& thread : producers) {
        thread.join();
    }
    queue.close();
    for (auto& thread : consumers) {
        thread.join();
    }
    EXPECT_EQ(total.load(), 3LL * kPerProducer * (kPerProducer + 1) / 2);
}

TEST(Queue, MoveOnlyValues) {
    BlockingQueue<std::unique_ptr<int>> queue;
    queue.push(std::make_unique<int>(5));
    auto value = queue.pop();
    ASSERT_TRUE(value.has_value());
    EXPECT_EQ(**value, 5);
}

TEST(Pool, RunsEverySubmittedTask) {
    std::atomic<int> runs{0};
    {
        WorkerPool pool(4);
        for (int i = 0; i < 500; ++i) {
            EXPECT_TRUE(pool.submit([&runs] { ++runs; }));
        }
    }  // the destructor waits for the queued tasks
    EXPECT_EQ(runs.load(), 500);
}

TEST(Pool, AsyncReturnsAFuture) {
    WorkerPool pool(2);
    auto answer = pool.async([] { return 6 * 7; });
    auto text = pool.async([] { return std::string("done"); });
    EXPECT_EQ(answer.get(), 42);
    EXPECT_EQ(text.get(), "done");
}

TEST(Pool, AsyncCarriesExceptions) {
    WorkerPool pool(1);
    auto failing = pool.async([]() -> int { throw std::runtime_error("boom"); });
    EXPECT_THROW(failing.get(), std::runtime_error);
}

TEST(Pool, ThrowingTaskDoesNotKillTheWorker) {
    WorkerPool pool(1);
    pool.submit([] { throw std::runtime_error("bad task"); });
    auto after = pool.async([] { return 1; });
    EXPECT_EQ(after.get(), 1);
    pool.shutdown();
    EXPECT_EQ(pool.failed(), 1U);
    EXPECT_EQ(pool.completed(), 1U);
}

TEST(Pool, ShutdownFinishesQueuedWorkThenRefuses) {
    WorkerPool pool(2);
    std::atomic<int> runs{0};
    for (int i = 0; i < 50; ++i) {
        pool.submit([&runs] {
            std::this_thread::sleep_for(1ms);
            ++runs;
        });
    }
    pool.shutdown();
    EXPECT_EQ(runs.load(), 50);
    EXPECT_EQ(pool.completed(), 50U);
    EXPECT_FALSE(pool.submit([] {}));
    pool.shutdown();
}

TEST(Pool, WorksInParallel) {
    WorkerPool pool(4);
    const auto started = std::chrono::steady_clock::now();
    std::vector<std::future<void>> sleepers;
    for (int i = 0; i < 4; ++i) {
        sleepers.push_back(pool.async([] { std::this_thread::sleep_for(200ms); }));
    }
    for (auto& sleeper : sleepers) {
        sleeper.get();
    }
    EXPECT_LT(std::chrono::steady_clock::now() - started, 700ms) << "the tasks ran one by one";
}

TEST(ParallelSum, MatchesTheSequentialSum) {
    std::vector<int> values(200'001);
    std::iota(values.begin(), values.end(), -100'000);
    values.push_back(7);
    for (const std::size_t threads : {0U, 1U, 3U, 8U}) {
        EXPECT_EQ(dojo::parallel_sum(values, threads), 7) << threads << " threads";
    }
}

TEST(ParallelSum, MoreThreadsThanValues) {
    EXPECT_EQ(dojo::parallel_sum({1, 2, 3}, 16), 6);
    EXPECT_EQ(dojo::parallel_sum({}, 4), 0);
    const std::vector<int> big(100'000, 30'000);
    EXPECT_EQ(dojo::parallel_sum(big, 4), 3'000'000'000LL);
}
