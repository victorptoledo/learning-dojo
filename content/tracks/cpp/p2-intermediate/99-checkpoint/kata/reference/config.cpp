#include "config.hpp"

namespace dojo {

namespace {

std::string_view trim(std::string_view text) {
    const auto first = text.find_first_not_of(" \t\r");
    if (first == std::string_view::npos) {
        return {};
    }
    const auto last = text.find_last_not_of(" \t\r");
    return text.substr(first, last - first + 1);
}

bool valid_key(std::string_view key) {
    if (key.empty()) {
        return false;
    }
    for (const char c : key) {
        const bool allowed =
            (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '.';
        if (!allowed) {
            return false;
        }
    }
    return true;
}

}  // namespace

std::expected<Config, ConfigError> Config::parse(std::string_view text) {
    Config config;
    int number = 0;
    while (!text.empty()) {
        const auto newline = text.find('\n');
        const std::string_view line = trim(text.substr(0, newline));
        text.remove_prefix(newline == std::string_view::npos ? text.size() : newline + 1);
        ++number;

        if (line.empty() || line.starts_with('#')) {
            continue;
        }
        const auto equals = line.find('=');
        if (equals == std::string_view::npos) {
            return std::unexpected(ConfigError{ErrorKind::Syntax, number, ""});
        }
        const std::string_view key = trim(line.substr(0, equals));
        if (!valid_key(key)) {
            return std::unexpected(ConfigError{ErrorKind::Syntax, number, ""});
        }
        const auto [where, inserted] =
            config.values_.try_emplace(std::string(key), trim(line.substr(equals + 1)));
        if (!inserted) {
            return std::unexpected(ConfigError{ErrorKind::DuplicateKey, number, where->first});
        }
    }
    return config;
}

std::optional<std::string> Config::raw(std::string_view key) const {
    const auto found = values_.find(key);
    if (found == values_.end()) {
        return std::nullopt;
    }
    return found->second;
}

void Config::set(std::string key, std::string value) {
    values_.insert_or_assign(std::move(key), std::move(value));
}

bool Config::erase(std::string_view key) {
    const auto found = values_.find(key);
    if (found == values_.end()) {
        return false;
    }
    values_.erase(found);
    return true;
}

Transaction::Transaction(Transaction&& other) noexcept
    : config_(other.config_), undo_(std::move(other.undo_)), done_(other.done_) {
    other.undo_.clear();
    other.done_ = true;
}

Transaction::~Transaction() {
    if (!done_) {
        rollback();
    }
}

void Transaction::set(std::string key, std::string value) {
    if (done_) {
        throw std::logic_error("Transaction::set after the transaction ended");
    }
    undo_.emplace_back(key, config_->raw(key));
    config_->set(std::move(key), std::move(value));
}

void Transaction::erase(std::string_view key) {
    if (done_) {
        throw std::logic_error("Transaction::erase after the transaction ended");
    }
    undo_.emplace_back(std::string(key), config_->raw(key));
    config_->erase(key);
}

void Transaction::commit() {
    if (done_) {
        throw std::logic_error("Transaction::commit after the transaction ended");
    }
    undo_.clear();
    done_ = true;
}

void Transaction::rollback() {
    if (done_) {
        throw std::logic_error("Transaction::rollback after the transaction ended");
    }
    done_ = true;
    for (auto it = undo_.rbegin(); it != undo_.rend(); ++it) {
        auto& [key, before] = *it;
        if (before) {
            config_->set(std::move(key), std::move(*before));
        } else {
            config_->erase(key);
        }
    }
    undo_.clear();
}

}  // namespace dojo
