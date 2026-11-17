#include "server.hpp"

namespace dojo {

Server::~Server() {
    stop();
}

void Server::route(std::string method, std::string path, Handler handler) {
    (void)method;
    (void)path;
    (void)handler;
}

std::expected<std::uint16_t, std::string> Server::start(std::uint16_t port) {
    (void)port;
    return std::unexpected("todo: start the server");
}

void Server::stop() {}

}  // namespace dojo
