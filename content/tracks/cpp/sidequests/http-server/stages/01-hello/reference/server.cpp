#include "server.hpp"

#include <arpa/inet.h>
#include <netinet/in.h>
#include <poll.h>
#include <sys/socket.h>
#include <unistd.h>

#include <algorithm>
#include <cerrno>
#include <format>
#include <string_view>
#include <system_error>

namespace dojo {

Socket& Socket::operator=(Socket&& other) noexcept {
    if (this != &other) {
        close();
        fd_ = std::exchange(other.fd_, -1);
    }
    return *this;
}

void Socket::close() {
    if (fd_ >= 0) {
        ::close(fd_);
        fd_ = -1;
    }
}

namespace {

// How long a blocked thread waits before it looks at the stop flag again.
constexpr std::chrono::milliseconds kSlice{25};

std::string_view reason(int status) {
    switch (status) {
        case 200:
            return "OK";
        case 201:
            return "Created";
        case 204:
            return "No Content";
        case 400:
            return "Bad Request";
        case 404:
            return "Not Found";
        default:
            return "Unknown";
    }
}

Response error(int status) {
    return Response{.status = status,
                    .headers = {{"Content-Type", "text/plain"}},
                    .body = std::string(reason(status))};
}

bool send_all(int fd, std::string_view data) {
    while (!data.empty()) {
        const auto sent = ::send(fd, data.data(), data.size(), MSG_NOSIGNAL);
        if (sent < 0 && errno == EINTR) {
            continue;
        }
        if (sent <= 0) {
            return false;
        }
        data.remove_prefix(static_cast<std::size_t>(sent));
    }
    return true;
}

enum class Read { Data, Closed, Stopping };

// Appends what arrives on `fd` to `buffer`, waiting as long as it takes (but no longer than a
// stop).
Read read_some(int fd, std::string& buffer, const std::atomic<bool>& stopping) {
    while (true) {
        if (stopping.load()) {
            return Read::Stopping;
        }
        pollfd waiting{.fd = fd, .events = POLLIN, .revents = 0};
        const int ready = ::poll(&waiting, 1, static_cast<int>(kSlice.count()));
        if (ready == 0 || (ready < 0 && errno == EINTR)) {
            continue;
        }
        if (ready < 0) {
            return Read::Closed;
        }
        char chunk[4096];
        const auto got = ::recv(fd, chunk, sizeof chunk, 0);
        if (got < 0 && errno == EINTR) {
            continue;
        }
        if (got <= 0) {
            return Read::Closed;
        }
        buffer.append(chunk, static_cast<std::size_t>(got));
        return Read::Data;
    }
}

// The method and path of the request line (the headers are not read yet), or the status
// that refuses it.
std::expected<Request, int> parse_head(std::string_view text) {
    const std::string_view line = text.substr(0, text.find("\r\n"));
    const std::size_t space1 = line.find(' ');
    const std::size_t space2 = line.find(' ', space1 + 1);
    if (space1 == 0 || space1 == std::string_view::npos || space2 == std::string_view::npos) {
        return std::unexpected(400);
    }
    const std::string_view target = line.substr(space1 + 1, space2 - space1 - 1);
    return Request{.method = std::string(line.substr(0, space1)),
                   .path = std::string(target.substr(0, target.find('?')))};
}

}  // namespace

void Server::route(std::string method, std::string path, Handler handler) {
    routes_[std::move(path)][std::move(method)] = std::move(handler);
}

std::expected<std::uint16_t, std::string> Server::start(std::uint16_t port) {
    if (listener_) {
        return std::unexpected("the server is already running");
    }
    Socket listener(::socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC, 0));
    if (!listener) {
        return std::unexpected(std::system_category().message(errno));
    }
    const int on = 1;
    ::setsockopt(listener.fd(), SOL_SOCKET, SO_REUSEADDR, &on, sizeof on);
    sockaddr_in address{};
    address.sin_family = AF_INET;
    address.sin_port = htons(port);
    address.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    socklen_t size = sizeof address;
    auto* const raw = reinterpret_cast<sockaddr*>(&address);
    if (::bind(listener.fd(), raw, size) != 0 || ::listen(listener.fd(), 128) != 0 ||
        ::getsockname(listener.fd(), raw, &size) != 0) {
        return std::unexpected(std::system_category().message(errno));
    }
    listener_ = std::move(listener);
    stopping_ = false;
    acceptor_ = std::jthread([this] { accept_loop(); });
    return ntohs(address.sin_port);
}

void Server::stop() {
    stopping_ = true;
    if (acceptor_.joinable()) {
        acceptor_.join();
    }
    listener_.close();  // from here on, connecting is refused
}

void Server::accept_loop() {
    while (!stopping_.load()) {
        pollfd waiting{.fd = listener_.fd(), .events = POLLIN, .revents = 0};
        if (::poll(&waiting, 1, static_cast<int>(kSlice.count())) <= 0) {
            continue;
        }
        Socket connection(::accept4(listener_.fd(), nullptr, nullptr, SOCK_CLOEXEC));
        if (!connection) {
            continue;
        }
        serve(std::move(connection));  // one at a time: the next one waits in the backlog
    }
}

Response Server::respond(const Request& request) const {
    const auto path = routes_.find(request.path);
    if (path == routes_.end()) {
        return error(404);
    }
    const auto handler = path->second.find(request.method);
    if (handler == path->second.end()) {
        return error(404);
    }
    return handler->second(request);
}

bool Server::reply(const Socket& connection, const Response& response) {
    std::string text = std::format("HTTP/1.1 {} {}\r\n", response.status, reason(response.status));
    for (const auto& [name, value] : response.headers) {
        text += std::format("{}: {}\r\n", name, value);
    }
    text += std::format("Content-Length: {}\r\nConnection: close\r\n\r\n", response.body.size());
    text += response.body;
    return send_all(connection.fd(), text);
}

void Server::serve(Socket connection) {
    std::string buffer;
    std::size_t head_end = std::string::npos;
    while ((head_end = buffer.find("\r\n\r\n")) == std::string::npos) {
        if (read_some(connection.fd(), buffer, stopping_) != Read::Data) {
            return;
        }
    }
    const auto request = parse_head(std::string_view(buffer).substr(0, head_end));
    reply(connection, request ? respond(*request) : error(request.error()));
}

}  // namespace dojo
