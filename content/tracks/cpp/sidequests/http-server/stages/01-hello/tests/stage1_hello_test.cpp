#include <gtest/gtest.h>

#include <cstdint>
#include <string>

#include "http_testing.hpp"
#include "server.hpp"

using dojo::Request;
using dojo::Response;
using dojo::Server;
using dojo_testing::get;

namespace {

void add_routes(Server& server) {
    server.route("GET", "/hello", [](const Request&) {
        return Response{
            .status = 200, .headers = {{"Content-Type", "text/plain"}}, .body = "hello"};
    });
    server.route("GET", "/echo-path", [](const Request& request) {
        return Response{.status = 200, .headers = {}, .body = request.method + " " + request.path};
    });
    server.route("POST", "/things", [](const Request&) {
        return Response{.status = 201, .headers = {{"Location", "/things/1"}}, .body = ""};
    });
}

}  // namespace

TEST(Stage1Hello, StartListensOnAFreePort) {
    Server server;
    add_routes(server);
    const auto port = server.start();
    ASSERT_TRUE(port.has_value()) << port.error();
    EXPECT_NE(*port, 0);
    EXPECT_TRUE(dojo_testing::accepts_connections(*port));
}

TEST(Stage1Hello, AnswersARoute) {
    Server server;
    add_routes(server);
    const auto port = server.start();
    ASSERT_TRUE(port.has_value()) << port.error();
    const auto reply = get(*port, "/hello");
    ASSERT_TRUE(reply.has_value()) << "no complete response";
    EXPECT_EQ(reply->status, 200);
    EXPECT_EQ(reply->body, "hello");
    EXPECT_EQ(reply->header("content-type"), "text/plain");
    EXPECT_EQ(reply->header("content-length"), "5");
}

TEST(Stage1Hello, TheHandlerSeesTheMethodAndPath) {
    Server server;
    add_routes(server);
    const auto port = server.start();
    ASSERT_TRUE(port.has_value()) << port.error();
    const auto reply = get(*port, "/echo-path");
    ASSERT_TRUE(reply.has_value());
    EXPECT_EQ(reply->body, "GET /echo-path");
}

TEST(Stage1Hello, StatusLineAndConnectionClose) {
    Server server;
    add_routes(server);
    const auto port = server.start();
    ASSERT_TRUE(port.has_value()) << port.error();
    dojo_testing::Connection connection(*port);
    ASSERT_TRUE(connection.send("POST /things HTTP/1.1\r\nConnection: close\r\n\r\n"));
    const auto reply = connection.read_reply();
    ASSERT_TRUE(reply.has_value());
    EXPECT_EQ(reply->status, 201);
    EXPECT_EQ(reply->header("location"), "/things/1");
    EXPECT_EQ(reply->header("content-length"), "0");
    EXPECT_EQ(reply->header("connection"), "close");
    EXPECT_TRUE(connection.closed_by_server());
}

TEST(Stage1Hello, UnknownRoutesAreNotFound) {
    Server server;
    add_routes(server);
    const auto port = server.start();
    ASSERT_TRUE(port.has_value()) << port.error();
    const auto reply = get(*port, "/nowhere");
    ASSERT_TRUE(reply.has_value());
    EXPECT_EQ(reply->status, 404);
    EXPECT_EQ(reply->body, "Not Found");
}

TEST(Stage1Hello, ServesRequestsOneAfterAnother) {
    Server server;
    add_routes(server);
    const auto port = server.start();
    ASSERT_TRUE(port.has_value()) << port.error();
    for (int i = 0; i < 20; ++i) {
        const auto reply = get(*port, "/hello");
        ASSERT_TRUE(reply.has_value()) << "request " << i;
        EXPECT_EQ(reply->body, "hello");
    }
}

TEST(Stage1Hello, StartRefusesAPortInUseOrASecondStart) {
    Server first;
    const auto port = first.start();
    ASSERT_TRUE(port.has_value()) << port.error();
    Server second;
    const auto clash = second.start(*port);
    EXPECT_FALSE(clash.has_value()) << "two servers on port " << *port;
    if (!clash) {
        EXPECT_FALSE(clash.error().empty()) << "say why start failed";
    }
    EXPECT_FALSE(first.start().has_value()) << "start on a running server must fail";
}

TEST(Stage1Hello, StopClosesThePort) {
    std::uint16_t port = 0;
    {
        Server server;
        add_routes(server);
        const auto started = server.start();
        ASSERT_TRUE(started.has_value()) << started.error();
        port = *started;
        server.stop();
        EXPECT_FALSE(dojo_testing::accepts_connections(port)) << "still listening after stop()";
        server.stop();  // a second stop does nothing
    }
    Server again;
    add_routes(again);
    const auto restarted = again.start(port);
    ASSERT_TRUE(restarted.has_value()) << "the port should be free again: " << restarted.error();
    const auto reply = get(port, "/hello");
    ASSERT_TRUE(reply.has_value());
    EXPECT_EQ(reply->body, "hello");
}

TEST(Stage1Hello, TheDestructorStopsTheServer) {
    std::uint16_t port = 0;
    {
        Server server;
        const auto started = server.start();
        ASSERT_TRUE(started.has_value()) << started.error();
        port = *started;
    }
    EXPECT_FALSE(dojo_testing::accepts_connections(port));
}
