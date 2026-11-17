#include "server.hpp"

#include <arpa/inet.h>
#include <netinet/in.h>
#include <poll.h>
#include <sys/socket.h>
#include <unistd.h>

#include <algorithm>
#include <cctype>
#include <cerrno>
#include <charconv>
#include <format>
#include <ranges>
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
        case 405:
            return "Method Not Allowed";
        case 500:
            return "Internal Server Error";
        default:
            return "Unknown";
    }
}

Response error(int status) {
    return Response{.status = status,
                    .headers = {{"Content-Type", "text/plain"}},
                    .body = std::string(reason(status))};
}

std::string lower(std::string_view text) {
    std::string result(text);
    std::ranges::transform(result, result.begin(),
                           [](unsigned char c) { return static_cast<char>(std::tolower(c)); });
    return result;
}

std::string_view trim(std::string_view text) {
    const auto first = text.find_first_not_of(" \t");
    if (first == std::string_view::npos) {
        return {};
    }
    return text.substr(first, text.find_last_not_of(" \t") - first + 1);
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

void parse_query(std::string_view text, Request& request) {
    while (!text.empty()) {
        const std::size_t amp = text.find('&');
        const std::string_view pair = text.substr(0, amp);
        const std::size_t equals = pair.find('=');
        if (!pair.empty()) {
            request.query[std::string(pair.substr(0, equals))] =
                equals == std::string_view::npos ? "" : std::string(pair.substr(equals + 1));
        }
        text = amp == std::string_view::npos ? "" : text.substr(amp + 1);
    }
}

// The request line and headers (without the blank line), or the status that refuses them.
std::expected<Request, int> parse_head(std::string_view text) {
    const std::size_t line_end = text.find("\r\n");
    const std::string_view line = text.substr(0, line_end);
    const std::size_t space1 = line.find(' ');
    const std::size_t space2 = line.find(' ', space1 + 1);
    if (space1 == 0 || space1 == std::string_view::npos || space2 == std::string_view::npos ||
        line.find(' ', space2 + 1) != std::string_view::npos) {
        return std::unexpected(400);
    }
    const std::string_view target = line.substr(space1 + 1, space2 - space1 - 1);
    const std::string_view version = line.substr(space2 + 1);
    if (!target.starts_with('/') || (version != "HTTP/1.1" && version != "HTTP/1.0")) {
        return std::unexpected(400);
    }
    Request request;
    request.method = std::string(line.substr(0, space1));
    const std::size_t question = target.find('?');
    request.path = std::string(target.substr(0, question));
    if (question != std::string_view::npos) {
        parse_query(target.substr(question + 1), request);
    }
    std::string_view rest = line_end == std::string_view::npos ? "" : text.substr(line_end + 2);
    while (!rest.empty()) {
        const std::size_t end = rest.find("\r\n");
        const std::string_view header = rest.substr(0, end);
        rest = end == std::string_view::npos ? "" : rest.substr(end + 2);
        const std::size_t colon = header.find(':');
        if (colon == 0 || colon == std::string_view::npos ||
            header.substr(0, colon).contains(' ')) {
            return std::unexpected(400);
        }
        request.headers[lower(header.substr(0, colon))] =
            std::string(trim(header.substr(colon + 1)));
    }
    return request;
}

// The body length a request announces, or the status that refuses it.
std::expected<std::size_t, int> body_length(const Request& request) {
    const auto header = request.headers.find("content-length");
    if (header == request.headers.end()) {
        return 0;
    }
    const std::string_view text = header->second;
    const char* const first = text.data();
    std::size_t length = 0;
    const auto [next, problem] = std::from_chars(first, first + text.size(), length);
    if (text.empty() || next != first + text.size() || problem != std::errc{}) {
        return std::unexpected(400);
    }
    return length;
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
    const unsigned workers = std::max(1U, config_.workers);
    for (unsigned i = 0; i < workers; ++i) {
        workers_.emplace_back([this] { work(); });
    }
    acceptor_ = std::jthread([this] { accept_loop(); });
    return ntohs(address.sin_port);
}

void Server::stop() {
    {
        const std::lock_guard lock(mutex_);
        stopping_ = true;
    }
    ready_.notify_all();
    if (acceptor_.joinable()) {
        acceptor_.join();
    }
    listener_.close();  // from here on, connecting is refused
    for (auto& worker : workers_) {
        worker.join();
    }
    workers_.clear();
    pending_.clear();
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
        {
            const std::lock_guard lock(mutex_);
            pending_.push_back(std::move(connection));
        }
        ready_.notify_one();
    }
}

void Server::work() {
    while (true) {
        Socket connection;
        {
            std::unique_lock lock(mutex_);
            ready_.wait(lock, [this] { return stopping_.load() || !pending_.empty(); });
            if (pending_.empty()) {
                return;  // stopping, and nobody is waiting
            }
            connection = std::move(pending_.front());
            pending_.pop_front();
        }
        serve(std::move(connection));
    }
}

Response Server::respond(const Request& request) const {
    const auto path = routes_.find(request.path);
    if (path == routes_.end()) {
        return error(404);
    }
    const auto handler = path->second.find(request.method);
    if (handler == path->second.end()) {
        Response refused = error(405);
        std::string allow;
        for (const auto& method : path->second | std::views::keys) {
            if (!allow.empty()) {
                allow += ", ";
            }
            allow += method;
        }
        refused.headers.emplace_back("Allow", std::move(allow));
        return refused;
    }
    try {
        return handler->second(request);
    } catch (...) {
        return error(500);  // a failing handler costs one response, never the server
    }
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
    const int fd = connection.fd();
    std::string buffer;
    // Reads until `done` says enough; false if the client closed first or the server stops.
    const auto wait_for = [&](auto&& done) {
        while (!done()) {
            if (read_some(fd, buffer, stopping_) != Read::Data) {
                return false;
            }
        }
        return true;
    };
    std::size_t head_end = std::string::npos;
    if (!wait_for([&] { return (head_end = buffer.find("\r\n\r\n")) != std::string::npos; })) {
        return;
    }
    auto request = parse_head(std::string_view(buffer).substr(0, head_end));
    if (!request) {
        reply(connection, error(request.error()));
        return;
    }
    buffer.erase(0, head_end + 4);
    const auto length = body_length(*request);
    if (!length) {
        reply(connection, error(length.error()));
        return;
    }
    if (!wait_for([&] { return buffer.size() >= *length; })) {
        return;
    }
    request->body = buffer.substr(0, *length);
    reply(connection, respond(*request));
}

}  // namespace dojo
