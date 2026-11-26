#pragma once

#include <array>
#include <cstddef>
#include <cstdint>
#include <functional>
#include <span>
#include <stdexcept>
#include <string>
#include <string_view>
#include <unordered_map>
#include <vector>

namespace dojo {

// A metric name: 1 to 64 characters, a lower-case letter first, then lower-case letters,
// digits and underscores. Usable at compile time: static_assert(valid_name("http_requests")).
constexpr bool valid_name(std::string_view name) {
    if (name.empty() || name.size() > 64 || name[0] < 'a' || name[0] > 'z') {
        return false;
    }
    for (const char c : name) {
        if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_')) {
            return false;
        }
    }
    return true;
}

// N bucket bounds: start, start * factor, start * factor^2 ... Needs start > 0 and factor > 1:
// otherwise std::invalid_argument, which inside a constant expression is a compile error.
template <std::size_t N>
constexpr std::array<double, N> exponential_bounds(double start, double factor) {
    static_assert(N > 0, "a histogram needs at least one bound");
    if (!(start > 0.0) || !(factor > 1.0)) {
        throw std::invalid_argument("exponential_bounds needs start > 0 and factor > 1");
    }
    std::array<double, N> bounds{};
    double bound = start;
    for (double& b : bounds) {
        b = bound;
        bound *= factor;
    }
    return bounds;
}

// A fixed-bucket histogram. Bucket i counts values <= bounds[i] (and above bounds[i - 1]); one
// more bucket counts values above the last bound. Recording never allocates.
class Histogram {
public:
    // The bounds must be finite and strictly increasing, and there must be at least one:
    // std::invalid_argument otherwise.
    explicit Histogram(std::span<const double> bounds);

    // Counts the value in its bucket and adds it to the sum. NaN is ignored.
    void record(double value) noexcept;

    std::uint64_t count() const noexcept { return count_; }
    double sum() const noexcept { return sum_; }
    std::span<const double> bounds() const noexcept { return bounds_; }
    // bounds().size() + 1 counts, the last one for values above every bound.
    std::span<const std::uint64_t> buckets() const noexcept { return counts_; }

    // The upper bound of the bucket where the q-th fraction of the values falls (the bucket
    // holding the ceil(q * count)-th smallest value, at least the first): +infinity for the
    // last bucket, 0 when nothing was recorded. std::invalid_argument unless 0 <= q <= 1.
    double quantile(double q) const;

private:
    std::vector<double> bounds_;
    std::vector<std::uint64_t> counts_;
    std::uint64_t count_ = 0;
    double sum_ = 0.0;
};

// Lets an unordered_map keyed by std::string be searched with a std::string_view, without
// building a std::string (and allocating) for every lookup.
struct StringHash {
    using is_transparent = void;
    std::size_t operator()(std::string_view text) const noexcept {
        return std::hash<std::string_view>{}(text);
    }
};

class Registry {
public:
    // Adds a histogram. std::invalid_argument for an invalid name or bounds, and for a name
    // that is already defined.
    Histogram& define(std::string_view name, std::span<const double> bounds);

    // The histogram, or nullptr. Never allocates.
    Histogram* find(std::string_view name) noexcept;
    const Histogram* find(std::string_view name) const noexcept;

    std::size_t size() const noexcept { return metrics_.size(); }

    // Every histogram, by name, in the Prometheus text format, with cumulative buckets:
    //   latency_ms_bucket{le="1"} 3
    //   latency_ms_bucket{le="2.5"} 5
    //   latency_ms_bucket{le="+Inf"} 6
    //   latency_ms_sum 12.5
    //   latency_ms_count 6
    // Numbers are printed with std::format's {} (the shortest form that reads back exactly).
    std::string render() const;

private:
    std::unordered_map<std::string, Histogram, StringHash, std::equal_to<>> metrics_;
};

}  // namespace dojo
