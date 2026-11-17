#include <gtest/gtest.h>

#include <atomic>
#include <chrono>
#include <optional>
#include <string>
#include <thread>
#include <vector>

#include "http_testing.hpp"
#include "server.hpp"

using dojo::Request;
using dojo::Response;
using dojo::Server;
using dojo::ServerConfig;
using namespace std::chrono_literals;

TEST(Stage3Workers, SlowRequestsRunAtTheSameTime) {
    Server server(ServerConfig{.workers = 4});
    std::atomic<int> running{0};
    std::atomic<int> most{0};
    server.route("GET", "/slow", [&](const Request&) {
        const int now = ++running;
        int seen = most.load();
        while (now > seen && !most.compare_exchange_weak(seen, now)) {
        }
        std::this_thread::sleep_for(300ms);
        --running;
        return Response{.status = 200, .headers = {}, .body = "done"};
    });
    const auto port = server.start();
    ASSERT_TRUE(port.has_value()) << port.error();

    const auto started = std::chrono::steady_clock::now();
    std::vector<std::optional<dojo_testing::Reply>> replies(4);
    {
        std::vector<std::jthread> clients;
        for (auto& reply : replies) {
            clients.emplace_back([&reply, &port] { reply = dojo_testing::get(*port, "/slow"); });
        }
    }
    const auto took = std::chrono::steady_clock::now() - started;
    for (const auto& reply : replies) {
        ASSERT_TRUE(reply.has_value());
        EXPECT_EQ(reply->body, "done");
    }
    EXPECT_GE(most.load(), 2) << "the handlers never ran at the same time";
    EXPECT_LT(took, 1000ms) << "four 300 ms requests on four workers took "
                            << std::chrono::duration_cast<std::chrono::milliseconds>(took).count()
                            << " ms";
}

TEST(Stage3Workers, NeverMoreHandlersAtOnceThanWorkers) {
    Server server(ServerConfig{.workers = 2});
    std::atomic<int> running{0};
    std::atomic<int> most{0};
    server.route("GET", "/slow", [&](const Request&) {
        const int now = ++running;
        int seen = most.load();
        while (now > seen && !most.compare_exchange_weak(seen, now)) {
        }
        std::this_thread::sleep_for(50ms);
        --running;
        return Response{.status = 200, .headers = {}, .body = "ok"};
    });
    const auto port = server.start();
    ASSERT_TRUE(port.has_value()) << port.error();
    std::atomic<int> ok{0};
    {
        std::vector<std::jthread> clients;
        for (int i = 0; i < 6; ++i) {
            clients.emplace_back([&] {
                const auto reply = dojo_testing::get(*port, "/slow");
                if (reply && reply->status == 200) {
                    ++ok;
                }
            });
        }
    }
    EXPECT_EQ(ok.load(), 6) << "a connection waiting for a worker must still be served";
    EXPECT_LE(most.load(), 2);
}

TEST(Stage3Workers, ManyClientsGetTheirOwnAnswers) {
    Server server(ServerConfig{.workers = 3});
    server.route("GET", "/echo", [](const Request& request) {
        return Response{
            .status = 200, .headers = {}, .body = request.path + "?" + request.query.at("n")};
    });
    const auto port = server.start();
    ASSERT_TRUE(port.has_value()) << port.error();
    std::atomic<int> right{0};
    {
        std::vector<std::jthread> clients;
        for (int c = 0; c < 5; ++c) {
            clients.emplace_back([&, c] {
                for (int i = 0; i < 10; ++i) {
                    const std::string n = std::to_string((c * 100) + i);
                    const auto reply = dojo_testing::get(*port, "/echo?n=" + n);
                    if (reply && reply->body == "/echo?" + n) {
                        ++right;
                    }
                }
            });
        }
    }
    EXPECT_EQ(right.load(), 50);
}

TEST(Stage3Workers, StopWaitsForTheRequestBeingHandled) {
    Server server(ServerConfig{.workers = 2});
    std::atomic<bool> entered{false};
    server.route("GET", "/slow", [&](const Request&) {
        entered = true;
        std::this_thread::sleep_for(200ms);
        return Response{.status = 200, .headers = {}, .body = "finished"};
    });
    const auto port = server.start();
    ASSERT_TRUE(port.has_value()) << port.error();
    std::optional<dojo_testing::Reply> reply;
    std::jthread client([&] { reply = dojo_testing::get(*port, "/slow"); });
    while (!entered) {
        std::this_thread::sleep_for(5ms);
    }
    server.stop();
    client.join();
    ASSERT_TRUE(reply.has_value()) << "stop() cut a request in the middle";
    EXPECT_EQ(reply->body, "finished");
    EXPECT_FALSE(dojo_testing::accepts_connections(*port));
}
