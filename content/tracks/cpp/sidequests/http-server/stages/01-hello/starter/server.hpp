#pragma once

#include <cstdint>
#include <expected>
#include <functional>
#include <string>
#include <utility>
#include <vector>

namespace dojo {

struct Request {
    std::string method;
    std::string path;
};

struct Response {
    int status = 200;
    std::vector<std::pair<std::string, std::string>> headers;
    std::string body;
};

using Handler = std::function<Response(const Request&)>;

class Server {
public:
    Server() = default;
    ~Server();
    Server(const Server&) = delete;
    Server& operator=(const Server&) = delete;

    // Adds a route; call it before start().
    void route(std::string method, std::string path, Handler handler);

    // Listens on 127.0.0.1:port (0: any free port) and serves in the background. The port, or
    // why it could not start.
    std::expected<std::uint16_t, std::string> start(std::uint16_t port = 0);

    // Stops accepting, finishes the request being handled, and waits for the thread.
    void stop();

private:
    // Your members: the routes, the listening socket (owned by a small RAII class of yours),
    // a stop flag and the background thread.
};

}  // namespace dojo
