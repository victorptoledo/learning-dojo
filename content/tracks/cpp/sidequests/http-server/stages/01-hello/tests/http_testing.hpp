// A tiny HTTP client for the contract tests of every stage: raw POSIX sockets, so the tests
// see exactly the bytes the server sends.
#pragma once

#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <unistd.h>

#include <cctype>
#include <chrono>
#include <cstdint>
#include <map>
#include <optional>
#include <string>
#include <string_view>
#include <utility>

namespace dojo_testing {

struct Reply {
    int status = 0;
    std::map<std::string, std::string> headers;  // names in lower case
    std::string body;

    // The value of a header (name in lower case), or "" when it is missing.
    std::string header(const std::string& name) const {
        const auto found = headers.find(name);
        return found == headers.end() ? "" : found->second;
    }
};

inline std::string lower(std::string text) {
    for (char& c : text) {
        c = static_cast<char>(std::tolower(static_cast<unsigned char>(c)));
    }
    return text;
}

// One client connection to 127.0.0.1:port, closed in the destructor.
class Connection {
public:
    explicit Connection(std::uint16_t port,
                        std::chrono::milliseconds timeout = std::chrono::milliseconds(5000))
        : fd_(::socket(AF_INET, SOCK_STREAM, 0)) {
        timeval limit{};
        limit.tv_sec = static_cast<time_t>(timeout.count() / 1000);
        limit.tv_usec = static_cast<suseconds_t>((timeout.count() % 1000) * 1000);
        ::setsockopt(fd_, SOL_SOCKET, SO_RCVTIMEO, &limit, sizeof limit);
        sockaddr_in address{};
        address.sin_family = AF_INET;
        address.sin_port = htons(port);
        address.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
        connected_ =
            ::connect(fd_, reinterpret_cast<const sockaddr*>(&address), sizeof address) == 0;
    }
    ~Connection() { ::close(fd_); }
    Connection(const Connection&) = delete;
    Connection& operator=(const Connection&) = delete;

    bool connected() const { return connected_; }

    bool send(std::string_view data) const {
        while (!data.empty()) {
            const auto sent = ::send(fd_, data.data(), data.size(), MSG_NOSIGNAL);
            if (sent <= 0) {
                return false;
            }
            data.remove_prefix(static_cast<std::size_t>(sent));
        }
        return true;
    }

    // The next response on this connection, or nothing if the server closed it first or
    // nothing came within the timeout.
    std::optional<Reply> read_reply() {
        std::size_t end = 0;
        while ((end = buffer_.find("\r\n\r\n")) == std::string::npos) {
            if (!fill()) {
                return std::nullopt;
            }
        }
        Reply reply;
        const std::string head = buffer_.substr(0, end);
        buffer_.erase(0, end + 4);
        std::size_t line_end = head.find("\r\n");
        const std::string status_line = head.substr(0, line_end);
        if (status_line.size() < 12 || !status_line.starts_with("HTTP/1.1 ")) {
            return std::nullopt;
        }
        reply.status = std::stoi(status_line.substr(9, 3));
        while (line_end != std::string::npos) {
            const std::size_t start = line_end + 2;
            line_end = head.find("\r\n", start);
            const std::string line = head.substr(start, line_end - start);
            const std::size_t colon = line.find(':');
            if (colon != std::string::npos) {
                std::string value = line.substr(colon + 1);
                value.erase(0, value.find_first_not_of(' '));
                reply.headers[lower(line.substr(0, colon))] = value;
            }
        }
        const auto length = reply.headers.find("content-length");
        const std::size_t size = length == reply.headers.end() ? 0 : std::stoul(length->second);
        while (buffer_.size() < size) {
            if (!fill()) {
                return std::nullopt;
            }
        }
        reply.body = buffer_.substr(0, size);
        buffer_.erase(0, size);
        return reply;
    }

    // True when the server closes the connection (recv returns 0) before the timeout.
    bool closed_by_server() {
        while (fill()) {
        }
        return eof_;
    }

private:
    bool fill() {
        char chunk[4096];
        const auto got = ::recv(fd_, chunk, sizeof chunk, 0);
        if (got <= 0) {
            eof_ = got == 0;
            return false;
        }
        buffer_.append(chunk, static_cast<std::size_t>(got));
        return true;
    }

    int fd_;
    bool connected_ = false;
    bool eof_ = false;
    std::string buffer_;
};

// Sends one raw request on a new connection and reads the reply.
inline std::optional<Reply> send_raw(std::uint16_t port, std::string_view raw) {
    Connection connection(port);
    if (!connection.connected() || !connection.send(raw)) {
        return std::nullopt;
    }
    return connection.read_reply();
}

inline std::optional<Reply> get(std::uint16_t port, const std::string& path) {
    return send_raw(port, "GET " + path + " HTTP/1.1\r\nHost: dojo\r\nConnection: close\r\n\r\n");
}

inline bool accepts_connections(std::uint16_t port) {
    return Connection(port, std::chrono::milliseconds(500)).connected();
}

}  // namespace dojo_testing
