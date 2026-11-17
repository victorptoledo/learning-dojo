#include <gtest/gtest.h>

#include <chrono>
#include <cstdint>
#include <stdexcept>
#include <string>
#include <thread>

#include "http_testing.hpp"
#include "server.hpp"

using dojo::Request;
using dojo::Response;
using dojo::Server;
using dojo_testing::send_raw;

namespace {

// Echoes what the server parsed: method, path, query, headers and body.
Response echo(const Request& request) {
    std::string text = request.method + " " + request.path + "\n";
    for (const auto& [name, value] : request.query) {
        text += "query " + name + "=" + value + "\n";
    }
    for (const auto& [name, value] : request.headers) {
        text += "header " + name + "=" + value + "\n";
    }
    text += "body " + request.body;
    return Response{.status = 200, .headers = {}, .body = text};
}

class Stage2Requests : public testing::Test {
protected:
    void SetUp() override {
        server_.route("GET", "/echo", echo);
        server_.route("POST", "/echo", echo);
        server_.route("DELETE", "/echo", echo);
        server_.route("GET", "/fail", [](const Request&) -> Response {
            throw std::runtime_error("broken handler");
        });
        const auto port = server_.start();
        ASSERT_TRUE(port.has_value()) << port.error();
        port_ = *port;
    }

    std::uint16_t port_ = 0;
    Server server_;
};

}  // namespace

TEST_F(Stage2Requests, SplitsTheQueryFromThePath) {
    const auto reply = dojo_testing::get(port_, "/echo?q=cats&page=2&flag&empty=");
    ASSERT_TRUE(reply.has_value());
    EXPECT_EQ(reply->body,
              "GET /echo\nquery empty=\nquery flag=\nquery page=2\nquery q=cats\n"
              "header connection=close\nheader host=dojo\nbody ");
}

TEST_F(Stage2Requests, HeaderNamesAreLowerCaseAndValuesTrimmed) {
    const auto reply =
        send_raw(port_,
                 "GET /echo HTTP/1.1\r\nX-Request-ID:   abc 123  \r\nACCEPT:text/plain\r\n"
                 "Connection: close\r\n\r\n");
    ASSERT_TRUE(reply.has_value());
    EXPECT_EQ(reply->body,
              "GET /echo\nheader accept=text/plain\nheader connection=close\n"
              "header x-request-id=abc 123\nbody ");
}

TEST_F(Stage2Requests, ReadsTheBodyByContentLength) {
    const auto reply = send_raw(
        port_, "POST /echo HTTP/1.1\r\nContent-Length: 11\r\nConnection: close\r\n\r\nhello world");
    ASSERT_TRUE(reply.has_value());
    EXPECT_TRUE(reply->body.ends_with("body hello world")) << reply->body;
}

TEST_F(Stage2Requests, ARequestMayArriveInPieces) {
    dojo_testing::Connection connection(port_);
    const std::string request =
        "POST /echo HTTP/1.1\r\nContent-Length: 5\r\nConnection: close\r\n\r\nabcde";
    for (const char c : request) {
        ASSERT_TRUE(connection.send(std::string(1, c)));
        if (c == '\n') {
            std::this_thread::sleep_for(std::chrono::milliseconds(2));
        }
    }
    const auto reply = connection.read_reply();
    ASSERT_TRUE(reply.has_value());
    EXPECT_TRUE(reply->body.ends_with("body abcde")) << reply->body;
}

TEST_F(Stage2Requests, MalformedRequestsAreBadRequests) {
    for (const char* raw :
         {"GET\r\n\r\n", "GET /echo\r\n\r\n", "GET /echo HTTP/2.0\r\n\r\n",
          "GET echo HTTP/1.1\r\n\r\n", "GET /echo HTTP/1.1 extra\r\n\r\n",
          "GET /echo HTTP/1.1\r\nno colon here\r\n\r\n", "GET /echo HTTP/1.1\r\n: no name\r\n\r\n",
          "POST /echo HTTP/1.1\r\nContent-Length: ten\r\n\r\n",
          "POST /echo HTTP/1.1\r\nContent-Length: -1\r\n\r\n"}) {
        const auto reply = send_raw(port_, raw);
        ASSERT_TRUE(reply.has_value()) << raw;
        EXPECT_EQ(reply->status, 400) << raw;
        EXPECT_EQ(reply->header("connection"), "close") << raw;
    }
}

TEST_F(Stage2Requests, AKnownPathWithAnotherMethodIsNotAllowed) {
    const auto reply = send_raw(port_, "PUT /echo HTTP/1.1\r\nConnection: close\r\n\r\n");
    ASSERT_TRUE(reply.has_value());
    EXPECT_EQ(reply->status, 405);
    EXPECT_EQ(reply->header("allow"), "DELETE, GET, POST");
    const auto unknown = send_raw(port_, "PUT /other HTTP/1.1\r\nConnection: close\r\n\r\n");
    ASSERT_TRUE(unknown.has_value());
    EXPECT_EQ(unknown->status, 404);
}

TEST_F(Stage2Requests, AThrowingHandlerIsAnInternalErrorAndTheServerGoesOn) {
    const auto failed = dojo_testing::get(port_, "/fail");
    ASSERT_TRUE(failed.has_value());
    EXPECT_EQ(failed->status, 500);
    EXPECT_EQ(failed->body, "Internal Server Error");
    const auto next = dojo_testing::get(port_, "/echo");
    ASSERT_TRUE(next.has_value()) << "the server died with its handler";
    EXPECT_EQ(next->status, 200);
}
