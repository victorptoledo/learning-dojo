#include <gtest/gtest.h>

#include <chrono>
#include <cstdint>
#include <string>

#include "http_testing.hpp"
#include "server.hpp"

using dojo::Request;
using dojo::Response;
using dojo::Server;
using dojo::ServerConfig;
using namespace std::chrono_literals;

namespace {

class Stage4Limits : public testing::Test {
protected:
    void SetUp() override {
        server_.route("POST", "/upload", [](const Request& request) {
            return Response{
                .status = 200, .headers = {}, .body = std::to_string(request.body.size())};
        });
        server_.route("GET", "/hello", [](const Request&) {
            return Response{.status = 200, .headers = {}, .body = "hello"};
        });
        const auto port = server_.start();
        ASSERT_TRUE(port.has_value()) << port.error();
        port_ = *port;
    }

    Server server_{ServerConfig{
        .workers = 2, .max_header_bytes = 512, .max_body_bytes = 1000, .read_timeout = 300ms}};
    std::uint16_t port_ = 0;
};

std::string upload(std::size_t size) {
    return "POST /upload HTTP/1.1\r\nConnection: close\r\nContent-Length: " + std::to_string(size) +
           "\r\n\r\n" + std::string(size, 'x');
}

}  // namespace

TEST_F(Stage4Limits, ABodyWithinTheLimitIsRead) {
    const auto reply = dojo_testing::send_raw(port_, upload(1000));
    ASSERT_TRUE(reply.has_value());
    EXPECT_EQ(reply->status, 200);
    EXPECT_EQ(reply->body, "1000");
}

TEST_F(Stage4Limits, ABodyOverTheLimitIsRefusedBeforeItIsRead) {
    dojo_testing::Connection connection(port_);
    ASSERT_TRUE(connection.send("POST /upload HTTP/1.1\r\nContent-Length: 1001\r\n\r\n"));
    const auto reply = connection.read_reply();  // the body was never sent
    ASSERT_TRUE(reply.has_value());
    EXPECT_EQ(reply->status, 413);
    EXPECT_EQ(reply->header("connection"), "close");
    const auto huge = dojo_testing::send_raw(
        port_, "POST /upload HTTP/1.1\r\nContent-Length: 99999999999999999999999\r\n\r\n");
    ASSERT_TRUE(huge.has_value());
    EXPECT_EQ(huge->status, 413);
}

TEST_F(Stage4Limits, HeadersOverTheLimitAreRefused) {
    const std::string big = "GET /hello HTTP/1.1\r\nX-Big: " + std::string(600, 'a') + "\r\n\r\n";
    const auto reply = dojo_testing::send_raw(port_, big);
    ASSERT_TRUE(reply.has_value());
    EXPECT_EQ(reply->status, 431);

    // Headers that never end must not make the server read forever.
    dojo_testing::Connection endless(port_);
    ASSERT_TRUE(endless.send("GET /hello HTTP/1.1\r\n"));
    for (int i = 0; i < 40; ++i) {
        if (!endless.send("X-More: " + std::string(50, 'b') + "\r\n")) {
            break;  // the server may close as soon as it has seen enough
        }
    }
    const auto cut = endless.read_reply();
    ASSERT_TRUE(cut.has_value());
    EXPECT_EQ(cut->status, 431);
}

TEST_F(Stage4Limits, ASilentClientTimesOut) {
    dojo_testing::Connection connection(port_);
    const auto started = std::chrono::steady_clock::now();
    const auto reply = connection.read_reply();
    const auto took = std::chrono::steady_clock::now() - started;
    ASSERT_TRUE(reply.has_value()) << "no 408 for a client that sent nothing";
    EXPECT_EQ(reply->status, 408);
    EXPECT_GE(took, 250ms);
    EXPECT_LT(took, 2000ms);
    EXPECT_TRUE(connection.closed_by_server());
}

TEST_F(Stage4Limits, AStalledBodyTimesOut) {
    dojo_testing::Connection connection(port_);
    ASSERT_TRUE(connection.send("POST /upload HTTP/1.1\r\nContent-Length: 10\r\n\r\nabc"));
    const auto reply = connection.read_reply();
    ASSERT_TRUE(reply.has_value());
    EXPECT_EQ(reply->status, 408);
}

TEST_F(Stage4Limits, ASlowClientDoesNotBlockTheOthers) {
    dojo_testing::Connection slow(port_);
    ASSERT_TRUE(slow.send("GET /hel"));  // and then nothing
    for (int i = 0; i < 5; ++i) {
        const auto started = std::chrono::steady_clock::now();
        const auto reply = dojo_testing::get(port_, "/hello");
        ASSERT_TRUE(reply.has_value());
        EXPECT_EQ(reply->body, "hello");
        EXPECT_LT(std::chrono::steady_clock::now() - started, 250ms)
            << "a request waited for the stalled client";
    }
}
