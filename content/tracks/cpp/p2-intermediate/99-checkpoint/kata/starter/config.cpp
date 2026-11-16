#include "config.hpp"

namespace dojo {

std::expected<Config, ConfigError> Config::parse(std::string_view text) {
    (void)text;
    throw std::logic_error("todo: write Config::parse");
}

std::optional<std::string> Config::raw(std::string_view key) const {
    (void)key;
    throw std::logic_error("todo: write Config::raw");
}

void Config::set(std::string key, std::string value) {
    (void)key;
    (void)value;
    throw std::logic_error("todo: write Config::set");
}

bool Config::erase(std::string_view key) {
    (void)key;
    throw std::logic_error("todo: write Config::erase");
}

Transaction::Transaction(Transaction&& other) noexcept : config_(other.config_) {
    // todo: take over other's changes and leave other with nothing to undo
}

Transaction::~Transaction() {
    // todo: roll back when neither commit() nor rollback() was called (a destructor must
    // not throw, so this one is empty for now)
}

void Transaction::set(std::string key, std::string value) {
    (void)key;
    (void)value;
    throw std::logic_error("todo: write Transaction::set");
}

void Transaction::erase(std::string_view key) {
    (void)key;
    throw std::logic_error("todo: write Transaction::erase");
}

void Transaction::commit() {
    throw std::logic_error("todo: write Transaction::commit");
}

void Transaction::rollback() {
    throw std::logic_error("todo: write Transaction::rollback");
}

}  // namespace dojo
