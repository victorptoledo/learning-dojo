// Your own tests: add as many as you like. The dojo adds its contract tests to this folder
// when it checks a stage. Try the server by hand too: start it from a small test and run
// `curl -v http://127.0.0.1:<port>/hello` while it sleeps.
#include <gtest/gtest.h>

#include "server.hpp"

TEST(MyServer, Starts) {
    dojo::Server server;
    server.route("GET", "/hello", [](const dojo::Request&) {
        return dojo::Response{.status = 200, .headers = {}, .body = "hello"};
    });
    const auto port = server.start();
    ASSERT_TRUE(port.has_value()) << port.error();
}
