#include "metrics.hpp"

#include <algorithm>
#include <cmath>
#include <cstring>
#include <format>
#include <iterator>
#include <limits>
#include <new>

#include "metrics_c.hpp"

namespace dojo {

Histogram::Histogram(std::span<const double> bounds) : bounds_(bounds.begin(), bounds.end()) {
    if (bounds_.empty()) {
        throw std::invalid_argument("a histogram needs at least one bound");
    }
    if (!std::ranges::all_of(bounds_, [](double b) { return std::isfinite(b); }) ||
        std::ranges::adjacent_find(bounds_, std::greater_equal<>{}) != bounds_.end()) {
        throw std::invalid_argument("bounds must be finite and strictly increasing");
    }
    counts_.assign(bounds_.size() + 1, 0);  // every allocation happens here, not in record
}

void Histogram::record(double value) noexcept {
    if (std::isnan(value)) {
        return;
    }
    // The first bound >= value: O(log n) on contiguous memory, and no allocation.
    const auto bucket = std::ranges::lower_bound(bounds_, value) - bounds_.begin();
    ++counts_[static_cast<std::size_t>(bucket)];
    ++count_;
    sum_ += value;
}

double Histogram::quantile(double q) const {
    if (!(q >= 0.0 && q <= 1.0)) {
        throw std::invalid_argument("quantile needs 0 <= q <= 1");
    }
    if (count_ == 0) {
        return 0.0;
    }
    const auto rank = std::max<std::uint64_t>(
        1, static_cast<std::uint64_t>(std::ceil(q * static_cast<double>(count_))));
    std::uint64_t seen = 0;
    for (std::size_t i = 0; i < bounds_.size(); ++i) {
        seen += counts_[i];
        if (seen >= rank) {
            return bounds_[i];
        }
    }
    return std::numeric_limits<double>::infinity();
}

Histogram& Registry::define(std::string_view name, std::span<const double> bounds) {
    if (!valid_name(name)) {
        throw std::invalid_argument(std::format("invalid metric name '{}'", name));
    }
    if (metrics_.contains(name)) {
        throw std::invalid_argument(std::format("metric '{}' is already defined", name));
    }
    return metrics_.emplace(std::string(name), Histogram(bounds)).first->second;
}

Histogram* Registry::find(std::string_view name) noexcept {
    const auto found = metrics_.find(name);  // heterogeneous lookup: no std::string is built
    return found == metrics_.end() ? nullptr : &found->second;
}

const Histogram* Registry::find(std::string_view name) const noexcept {
    const auto found = metrics_.find(name);
    return found == metrics_.end() ? nullptr : &found->second;
}

std::string Registry::render() const {
    std::vector<const decltype(metrics_)::value_type*> sorted;
    sorted.reserve(metrics_.size());
    for (const auto& entry : metrics_) {
        sorted.push_back(&entry);
    }
    std::ranges::sort(sorted, {}, [](const auto* entry) { return std::string_view(entry->first); });
    std::string out;
    auto it = std::back_inserter(out);
    for (const auto* entry : sorted) {
        const std::string& name = entry->first;
        const Histogram& h = entry->second;
        std::uint64_t cumulative = 0;
        for (std::size_t i = 0; i < h.bounds().size(); ++i) {
            cumulative += h.buckets()[i];
            std::format_to(it, "{}_bucket{{le=\"{}\"}} {}\n", name, h.bounds()[i], cumulative);
        }
        std::format_to(it, "{}_bucket{{le=\"+Inf\"}} {}\n", name, h.count());
        std::format_to(it, "{}_sum {}\n{}_count {}\n", name, h.sum(), name, h.count());
    }
    return out;
}

}  // namespace dojo

// The handle C code holds. Only this file knows what is inside.
struct dojo_registry {
    dojo::Registry registry;
};

extern "C" {

dojo_registry* dojo_registry_new(void) {
    try {
        return new dojo_registry{};
    } catch (...) {
        return nullptr;
    }
}

void dojo_registry_free(dojo_registry* registry) {
    delete registry;
}

int dojo_define(dojo_registry* registry, const char* name, const double* bounds, size_t count) {
    if (registry == nullptr || name == nullptr || (bounds == nullptr && count > 0)) {
        return DOJO_INVALID_ARGUMENT;
    }
    try {
        registry->registry.define(name, std::span<const double>(bounds, count));
        return DOJO_OK;
    } catch (const std::bad_alloc&) {
        return DOJO_OUT_OF_MEMORY;
    } catch (const std::invalid_argument&) {
        return DOJO_INVALID_ARGUMENT;
    } catch (...) {
        return DOJO_INVALID_ARGUMENT;  // nothing may cross into C
    }
}

int dojo_record(dojo_registry* registry, const char* name, double value) {
    if (registry == nullptr || name == nullptr || std::isnan(value)) {
        return DOJO_INVALID_ARGUMENT;
    }
    dojo::Histogram* histogram = registry->registry.find(name);
    if (histogram == nullptr) {
        return DOJO_UNKNOWN_METRIC;
    }
    histogram->record(value);
    return DOJO_OK;
}

size_t dojo_render(const dojo_registry* registry, char* buffer, size_t capacity) {
    std::string text;
    if (registry != nullptr) {
        try {
            text = registry->registry.render();
        } catch (...) {
            text.clear();
        }
    }
    if (buffer != nullptr && capacity > 0) {
        const std::size_t n = std::min(text.size(), capacity - 1);
        std::memcpy(buffer, text.data(), n);
        buffer[n] = '\0';
    }
    return text.size();
}

}  // extern "C"
