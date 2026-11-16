#pragma once

#include <cstdint>
#include <functional>
#include <map>
#include <optional>
#include <string>
#include <string_view>
#include <vector>

namespace dojo {

// Money is kept in cents: never use floating point for money.
using Cents = std::int64_t;

struct Item {
    std::string sku;
    std::string name;
    int quantity = 0;
    Cents unit_price = 0;

    bool operator==(const Item&) const = default;
};

// Parses "12.34", "7", "0.5" or "3.05" into cents. Rejects empty text, signs, more than two
// decimals, anything that is not a digit, and amounts that do not fit in Cents.
std::optional<Cents> parse_cents(std::string_view text);

class Inventory {
public:
    // Adds a new item. False (and nothing changes) if the sku exists, the sku or name is
    // empty, the quantity is negative or the price is negative.
    bool add(std::string sku, std::string name, int quantity, Cents unit_price);

    // Adds units to an existing item. False if the sku is unknown or amount <= 0.
    bool restock(std::string_view sku, int amount);

    // Removes units of an item. False (and nothing changes) if the sku is unknown,
    // amount <= 0, or there are fewer than amount units.
    bool sell(std::string_view sku, int amount);

    // A copy of the item, or nothing if the sku is unknown.
    std::optional<Item> find(std::string_view sku) const;

    // The value of everything in stock: the sum of quantity * unit_price.
    Cents total_value() const;

    // The skus with quantity <= threshold, in sku order.
    std::vector<std::string> low_stock(int threshold) const;

    // Runs one command per line and returns the 1-based numbers of the lines that failed.
    // Blank lines and lines starting with '#' are skipped. Words are split by spaces:
    //   add SKU NAME QUANTITY PRICE   (NAME is one word, PRICE as parse_cents reads it)
    //   restock SKU AMOUNT
    //   sell SKU AMOUNT
    // A line with an unknown command, the wrong number of words or a bad number fails.
    std::vector<int> apply(std::string_view script);

    std::size_t size() const { return items_.size(); }

private:
    // std::less<> lets find() look up a std::string_view without building a std::string.
    std::map<std::string, Item, std::less<>> items_;
};

}  // namespace dojo
