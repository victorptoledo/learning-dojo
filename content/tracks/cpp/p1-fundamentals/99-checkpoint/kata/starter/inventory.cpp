#include "inventory.hpp"

#include <stdexcept>

namespace dojo {

std::optional<Cents> parse_cents(std::string_view text) {
    (void)text;
    throw std::logic_error("todo: write parse_cents");
}

bool Inventory::add(std::string sku, std::string name, int quantity, Cents unit_price) {
    (void)sku;
    (void)name;
    (void)quantity;
    (void)unit_price;
    throw std::logic_error("todo: write Inventory::add");
}

bool Inventory::restock(std::string_view sku, int amount) {
    (void)sku;
    (void)amount;
    throw std::logic_error("todo: write Inventory::restock");
}

bool Inventory::sell(std::string_view sku, int amount) {
    (void)sku;
    (void)amount;
    throw std::logic_error("todo: write Inventory::sell");
}

std::optional<Item> Inventory::find(std::string_view sku) const {
    (void)sku;
    throw std::logic_error("todo: write Inventory::find");
}

Cents Inventory::total_value() const {
    throw std::logic_error("todo: write Inventory::total_value");
}

std::vector<std::string> Inventory::low_stock(int threshold) const {
    (void)threshold;
    throw std::logic_error("todo: write Inventory::low_stock");
}

std::vector<int> Inventory::apply(std::string_view script) {
    (void)script;
    throw std::logic_error("todo: write Inventory::apply");
}

}  // namespace dojo
