#include <gtest/gtest.h>

#include <atomic>
#include <chrono>
#include <cstdint>
#include <memory>
#include <optional>
#include <string>
#include <thread>

#include "http_testing.hpp"
#include "server.hpp"

using dojo::Request;
using dojo::Response;
using dojo::Server;
using dojo::ServerConfig;
using namespace std::chrono_literals;

namespace {

ServerConfig quick() {
    return ServerConfig{.workers = 3,
                        .max_header_bytes = 8192,
                        .max_body_bytes = 1 << 20,
                        .read_timeout = 1000ms,
                        .idle_timeout = 300ms};
}

void add_routes(Server& server) {
    server.route(
        "GET", "/count", [counter = std::make_shared<std::atomic<int>>(0)](const Request&) {
            return Response{.status = 200, .headers = {}, .body = std::to_string(++*counter)};
        });
    server.route("POST", "/echo", [](const Request& request) {
        return Response{.status = 200, .headers = {}, .body = request.body};
    });
}

}  // namespace

TEST(Stage5KeepAlive, OneConnectionCarriesSeveralRequests) {
    Server server(quick());
    add_routes(server);
    const auto port = server.start();
    ASSERT_TRUE(port.has_value()) << port.error();
    dojo_testing::Connection connection(*port);
    for (int i = 1; i <= 3; ++i) {
        ASSERT_TRUE(connection.send("GET /count HTTP/1.1\r\nHost: dojo\r\n\r\n"));
        const auto reply = connection.read_reply();
        ASSERT_TRUE(reply.has_value()) << "request " << i << " on the same connection";
        EXPECT_EQ(reply->body, std::to_string(i));
        EXPECT_EQ(reply->header("connection"), "keep-alive");
    }
}

TEST(Stage5KeepAlive, PipelinedRequestsAreAnsweredInOrder) {
    Server server(quick());
    add_routes(server);
    const auto port = server.start();
    ASSERT_TRUE(port.has_value()) << port.error();
    dojo_testing::Connection connection(*port);
    ASSERT_TRUE(
        connection.send("POST /echo HTTP/1.1\r\nContent-Length: 3\r\n\r\none"
                        "POST /echo HTTP/1.1\r\nContent-Length: 3\r\n\r\ntwo"
                        "POST /echo HTTP/1.1\r\nContent-Length: 5\r\nConnection: close\r\n"
                        "\r\nthree"));
    for (const char* expected : {"one", "two", "three"}) {
        const auto reply = connection.read_reply();
        ASSERT_TRUE(reply.has_value()) << expected;
        EXPECT_EQ(reply->body, expected);
    }
    EXPECT_TRUE(connection.closed_by_server());
}

TEST(Stage5KeepAlive, ConnectionCloseAndHttp10CloseAfterTheResponse) {
    Server server(quick());
    add_routes(server);
    const auto port = server.start();
    ASSERT_TRUE(port.has_value()) << port.error();
    for (const char* raw :
         {"GET /count HTTP/1.1\r\nConnection: Close\r\n\r\n", "GET /count HTTP/1.0\r\n\r\n"}) {
        dojo_testing::Connection connection(*port);
        ASSERT_TRUE(connection.send(raw));
        const auto reply = connection.read_reply();
        ASSERT_TRUE(reply.has_value()) << raw;
        EXPECT_EQ(reply->header("connection"), "close") << raw;
        EXPECT_TRUE(connection.closed_by_server()) << raw;
    }
}

TEST(Stage5KeepAlive, AnIdleConnectionIsClosedQuietly) {
    Server server(quick());
    add_routes(server);
    const auto port = server.start();
    ASSERT_TRUE(port.has_value()) << port.error();
    dojo_testing::Connection connection(*port);
    ASSERT_TRUE(connection.send("GET /count HTTP/1.1\r\n\r\n"));
    ASSERT_TRUE(connection.read_reply().has_value());
    const auto started = std::chrono::steady_clock::now();
    EXPECT_TRUE(connection.closed_by_server()) << "no close after the idle timeout";
    const auto took = std::chrono::steady_clock::now() - started;
    EXPECT_GE(took, 250ms);
    EXPECT_LT(took, 900ms) << "closed after the read timeout instead of the idle timeout";
    EXPECT_FALSE(connection.read_reply().has_value()) << "an idle close sends nothing (no 408)";
}

TEST(Stage5KeepAlive, StopFinishesTheRequestAndClosesIdleConnections) {
    Server server(quick());
    std::atomic<bool> entered{false};
    server.route("GET", "/slow", [&](const Request&) {
        entered = true;
        std::this_thread::sleep_for(200ms);
        return Response{.status = 200, .headers = {}, .body = "finished"};
    });
    add_routes(server);
    const auto port = server.start();
    ASSERT_TRUE(port.has_value()) << port.error();

    dojo_testing::Connection idle(*port);
    ASSERT_TRUE(idle.send("GET /count HTTP/1.1\r\n\r\n"));
    ASSERT_TRUE(idle.read_reply().has_value());

    dojo_testing::Connection busy(*port);
    ASSERT_TRUE(busy.send("GET /slow HTTP/1.1\r\n\r\n"));
    while (!entered) {
        std::this_thread::sleep_for(5ms);
    }
    const auto started = std::chrono::steady_clock::now();
    server.stop();
    EXPECT_LT(std::chrono::steady_clock::now() - started, 900ms)
        << "stop() waited for the idle connection to time out";
    const auto reply = busy.read_reply();
    ASSERT_TRUE(reply.has_value()) << "stop() cut the request being handled";
    EXPECT_EQ(reply->body, "finished");
    EXPECT_EQ(reply->header("connection"), "close");
    EXPECT_TRUE(idle.closed_by_server());
    EXPECT_TRUE(busy.closed_by_server());
}

TEST(Stage5KeepAlive, CountsTheResponsesItSent) {
    Server server(quick());
    add_routes(server);
    const auto port = server.start();
    ASSERT_TRUE(port.has_value()) << port.error();
    EXPECT_EQ(server.requests_served(), 0U);
    dojo_testing::Connection connection(*port);
    for (int i = 0; i < 3; ++i) {
        ASSERT_TRUE(connection.send("GET /count HTTP/1.1\r\n\r\n"));
        ASSERT_TRUE(connection.read_reply().has_value());
    }
    ASSERT_TRUE(dojo_testing::get(*port, "/missing").has_value());
    ASSERT_TRUE(dojo_testing::send_raw(*port, "BROKEN\r\n\r\n").has_value());
    EXPECT_EQ(server.requests_served(), 5U) << "every response counts, errors too";
}
