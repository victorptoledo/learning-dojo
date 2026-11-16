#include "inventory.hpp"

#include <charconv>
#include <limits>

namespace dojo {

namespace {

bool all_digits(std::string_view text) {
    if (text.empty()) {
        return false;
    }
    for (const char c : text) {
        if (c < '0' || c > '9') {
            return false;
        }
    }
    return true;
}

// A non-negative int, digits only.
std::optional<int> parse_int(std::string_view text) {
    if (!all_digits(text)) {
        return std::nullopt;
    }
    int value = 0;
    const auto [end, error] = std::from_chars(text.data(), text.data() + text.size(), value);
    if (error != std::errc{} || end != text.data() + text.size()) {
        return std::nullopt;
    }
    return value;
}

std::vector<std::string_view> split(std::string_view line) {
    std::vector<std::string_view> parts;
    std::size_t start = 0;
    while (start < line.size()) {
        const auto begin = line.find_first_not_of(" \t\r", start);
        if (begin == std::string_view::npos) {
            break;
        }
        const auto end = line.find_first_of(" \t\r", begin);
        parts.push_back(line.substr(begin, end - begin));
        start = end == std::string_view::npos ? line.size() : end;
    }
    return parts;
}

}  // namespace

std::optional<Cents> parse_cents(std::string_view text) {
    const auto dot = text.find('.');
    const std::string_view units = text.substr(0, dot);
    std::string_view decimals;
    if (dot != std::string_view::npos) {
        decimals = text.substr(dot + 1);
        if (decimals.empty() || decimals.size() > 2 || !all_digits(decimals)) {
            return std::nullopt;
        }
    }
    if (!all_digits(units)) {
        return std::nullopt;
    }
    Cents whole = 0;
    const auto [end, error] = std::from_chars(units.data(), units.data() + units.size(), whole);
    if (error != std::errc{} || end != units.data() + units.size()) {
        return std::nullopt;
    }
    Cents fraction = 0;
    for (std::size_t i = 0; i < 2; ++i) {
        fraction = fraction * 10 + (i < decimals.size() ? decimals[i] - '0' : 0);
    }
    constexpr Cents kMax = std::numeric_limits<Cents>::max();
    if (whole > (kMax - fraction) / 100) {
        return std::nullopt;
    }
    return whole * 100 + fraction;
}

bool Inventory::add(std::string sku, std::string name, int quantity, Cents unit_price) {
    if (sku.empty() || name.empty() || quantity < 0 || unit_price < 0 || items_.contains(sku)) {
        return false;
    }
    Item item{sku, std::move(name), quantity, unit_price};
    items_.emplace(std::move(sku), std::move(item));
    return true;
}

bool Inventory::restock(std::string_view sku, int amount) {
    const auto found = items_.find(sku);
    if (found == items_.end() || amount <= 0) {
        return false;
    }
    found->second.quantity += amount;
    return true;
}

bool Inventory::sell(std::string_view sku, int amount) {
    const auto found = items_.find(sku);
    if (found == items_.end() || amount <= 0 || found->second.quantity < amount) {
        return false;
    }
    found->second.quantity -= amount;
    return true;
}

std::optional<Item> Inventory::find(std::string_view sku) const {
    const auto found = items_.find(sku);
    if (found == items_.end()) {
        return std::nullopt;
    }
    return found->second;
}

Cents Inventory::total_value() const {
    Cents total = 0;
    for (const auto& [sku, item] : items_) {
        total += item.quantity * item.unit_price;
    }
    return total;
}

std::vector<std::string> Inventory::low_stock(int threshold) const {
    std::vector<std::string> skus;
    for (const auto& [sku, item] : items_) {
        if (item.quantity <= threshold) {
            skus.push_back(sku);
        }
    }
    return skus;
}

std::vector<int> Inventory::apply(std::string_view script) {
    std::vector<int> failed;
    int number = 0;
    while (!script.empty()) {
        const auto newline = script.find('\n');
        const std::string_view line = script.substr(0, newline);
        script.remove_prefix(newline == std::string_view::npos ? script.size() : newline + 1);
        ++number;

        const auto words = split(line);
        if (words.empty() || words[0].starts_with('#')) {
            continue;
        }
        bool ok = false;
        if (words[0] == "add" && words.size() == 5) {
            const auto quantity = parse_int(words[3]);
            const auto price = parse_cents(words[4]);
            ok = quantity && price &&
                 add(std::string(words[1]), std::string(words[2]), *quantity, *price);
        } else if ((words[0] == "restock" || words[0] == "sell") && words.size() == 3) {
            const auto amount = parse_int(words[2]);
            ok = amount &&
                 (words[0] == "restock" ? restock(words[1], *amount) : sell(words[1], *amount));
        }
        if (!ok) {
            failed.push_back(number);
        }
    }
    return failed;
}

}  // namespace dojo
