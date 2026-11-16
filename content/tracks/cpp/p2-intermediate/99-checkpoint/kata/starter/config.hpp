#pragma once

#include <charconv>
#include <concepts>
#include <cstddef>
#include <expected>
#include <functional>
#include <map>
#include <optional>
#include <stdexcept>
#include <string>
#include <string_view>
#include <utility>
#include <vector>

namespace dojo {

enum class ErrorKind {
    Syntax,        // a line that is not `key = value`, or a key with characters not allowed
    DuplicateKey,  // the same key twice in the text
    MissingKey,    // get() of a key that is not there
    WrongType,     // get<T>() of a value that does not read as a T
};

struct ConfigError {
    ErrorKind kind = ErrorKind::Syntax;
    int line = 0;     // the 1-based line for parse errors, 0 for get() errors
    std::string key;  // the key involved, empty for syntax errors

    bool operator==(const ConfigError&) const = default;
};

// The types get<T>() can read a value as.
template <typename T>
concept ConfigValue = std::same_as<T, int> || std::same_as<T, double> || std::same_as<T, bool> ||
                      std::same_as<T, std::string>;

class Config {
public:
    // Reads `key = value` lines. Blank lines and lines starting with '#' are skipped; spaces
    // around keys and values are ignored. A key is one or more of a-z, 0-9, '_' and '.'.
    // The value is everything after the first '=' (it may be empty).
    static std::expected<Config, ConfigError> parse(std::string_view text);

    // The value of key as a T: int and double must use the whole text (std::from_chars),
    // bool is exactly "true" or "false", std::string is the text itself.
    // MissingKey or WrongType errors carry the key.
    template <ConfigValue T>
    std::expected<T, ConfigError> get(std::string_view key) const {
        (void)key;
        throw std::logic_error("todo: write Config::get");
    }

    // The value as a T, or fallback when the key is missing or the value has the wrong type.
    template <ConfigValue T>
    T get_or(std::string_view key, T fallback) const {
        (void)key;
        (void)fallback;
        throw std::logic_error("todo: write Config::get_or");
    }

    bool contains(std::string_view key) const { return values_.contains(key); }
    std::size_t size() const { return values_.size(); }

    // The raw text of a value, or nothing when the key is missing.
    std::optional<std::string> raw(std::string_view key) const;

    void set(std::string key, std::string value);
    bool erase(std::string_view key);

private:
    std::map<std::string, std::string, std::less<>> values_;
};

// Changes a Config and undoes the changes when it is destroyed without commit(), like a
// database transaction or std::lock_guard. Move-only: exactly one object is responsible.
class Transaction {
public:
    explicit Transaction(Config& config) : config_(&config) {}
    Transaction(const Transaction&) = delete;
    Transaction& operator=(const Transaction&) = delete;
    // The new transaction takes over the changes; the moved-from one does nothing anymore.
    Transaction(Transaction&& other) noexcept;
    Transaction& operator=(Transaction&&) = delete;
    // Rolls back, unless commit() or rollback() was called.
    ~Transaction();

    // Applies the change to the config now, and remembers how to undo it.
    void set(std::string key, std::string value);
    void erase(std::string_view key);

    // Keeps every change. Later calls to set, erase, commit or rollback throw std::logic_error.
    void commit();
    // Undoes every change, the last one first. Later calls throw std::logic_error.
    void rollback();

private:
    Config* config_;
    // The key and its value before the change (nothing when the key did not exist).
    std::vector<std::pair<std::string, std::optional<std::string>>> undo_;
    bool done_ = false;
};

}  // namespace dojo
