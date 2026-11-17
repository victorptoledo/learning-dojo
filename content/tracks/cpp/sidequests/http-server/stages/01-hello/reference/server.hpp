#pragma once

#include <atomic>
#include <cstddef>
#include <cstdint>
#include <expected>
#include <functional>
#include <map>
#include <string>
#include <thread>
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

// An owned file descriptor: closed by the destructor, moved but never copied.
class Socket {
public:
    Socket() = default;
    explicit Socket(int fd) : fd_(fd) {}
    ~Socket() { close(); }
    Socket(Socket&& other) noexcept : fd_(std::exchange(other.fd_, -1)) {}
    Socket& operator=(Socket&& other) noexcept;
    Socket(const Socket&) = delete;
    Socket& operator=(const Socket&) = delete;

    int fd() const { return fd_; }
    explicit operator bool() const { return fd_ >= 0; }
    void close();

private:
    int fd_ = -1;
};

class Server {
public:
    Server() = default;
    ~Server() { stop(); }
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
    void accept_loop();
    void serve(Socket connection);
    Response respond(const Request& request) const;
    static bool reply(const Socket& connection, const Response& response);

    std::map<std::string, std::map<std::string, Handler>, std::less<>> routes_;  // path, method
    Socket listener_;
    std::atomic<bool> stopping_{false};
    std::jthread acceptor_;
};

}  // namespace dojo
