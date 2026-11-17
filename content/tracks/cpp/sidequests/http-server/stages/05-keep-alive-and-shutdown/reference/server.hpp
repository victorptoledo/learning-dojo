#pragma once

#include <atomic>
#include <chrono>
#include <condition_variable>
#include <cstddef>
#include <cstdint>
#include <deque>
#include <expected>
#include <functional>
#include <map>
#include <mutex>
#include <string>
#include <thread>
#include <utility>
#include <vector>

namespace dojo {

struct Request {
    std::string method;
    std::string path;                                         // the target without the query
    std::map<std::string, std::string, std::less<>> query;    // ?a=1&b=2, not decoded
    std::map<std::string, std::string, std::less<>> headers;  // names in lower case
    std::string body;
};

struct Response {
    int status = 200;
    std::vector<std::pair<std::string, std::string>> headers;
    std::string body;
};

using Handler = std::function<Response(const Request&)>;

struct ServerConfig {
    unsigned workers = 4;                          // requests handled at the same time
    std::size_t max_header_bytes = 8192;           // the request line and the headers
    std::size_t max_body_bytes = 1 << 20;          // the body
    std::chrono::milliseconds read_timeout{5000};  // to receive one whole request
    std::chrono::milliseconds idle_timeout{5000};  // between requests on one connection
};

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
    explicit Server(ServerConfig config = {}) : config_(config) {}
    ~Server() { stop(); }
    Server(const Server&) = delete;
    Server& operator=(const Server&) = delete;

    // Adds a route; call it before start().
    void route(std::string method, std::string path, Handler handler);

    // Listens on 127.0.0.1:port (0: any free port) and serves in the background. The port, or
    // why it could not start.
    std::expected<std::uint16_t, std::string> start(std::uint16_t port = 0);

    // Stops accepting, lets the requests being handled finish, and waits for every thread.
    void stop();

    // How many responses the server has sent.
    std::size_t requests_served() const { return served_.load(); }

private:
    void accept_loop();
    void work();
    void serve(Socket connection);
    Response respond(const Request& request) const;
    bool reply(const Socket& connection, const Response& response, bool keep_alive);

    ServerConfig config_;
    std::map<std::string, std::map<std::string, Handler>, std::less<>> routes_;  // path, method
    Socket listener_;
    std::atomic<bool> stopping_{false};
    std::atomic<std::size_t> served_{0};
    std::mutex mutex_;
    std::condition_variable ready_;
    std::deque<Socket> pending_;  // accepted connections waiting for a worker
    std::vector<std::jthread> workers_;
    std::jthread acceptor_;
};

}  // namespace dojo
