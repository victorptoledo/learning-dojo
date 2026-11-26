#include <gtest/gtest.h>

#include <algorithm>
#include <array>
#include <cmath>
#include <cstdlib>
#include <limits>
#include <new>
#include <stdexcept>
#include <string>
#include <type_traits>
#include <vector>

#include "metrics.hpp"
#include "metrics_c.hpp"

// Every allocation in the test program goes through these, so a test can count them, or make
// the next one fail.
namespace {
thread_local long g_allocations = 0;
thread_local bool g_fail_next = false;
}  // namespace

void* operator new(std::size_t size) {
    ++g_allocations;
    if (g_fail_next) {
        g_fail_next = false;
        throw std::bad_alloc();
    }
    if (void* p = std::malloc(size == 0 ? 1 : size)) {
        return p;
    }
    throw std::bad_alloc();
}
void* operator new[](std::size_t size) { return ::operator new(size); }
void* operator new(std::size_t size, const std::nothrow_t&) noexcept {
    ++g_allocations;
    if (g_fail_next) {
        g_fail_next = false;
        return nullptr;
    }
    return std::malloc(size == 0 ? 1 : size);
}
void* operator new[](std::size_t size, const std::nothrow_t& tag) noexcept {
    return ::operator new(size, tag);
}
void operator delete(void* p, const std::nothrow_t&) noexcept { std::free(p); }
void operator delete[](void* p, const std::nothrow_t&) noexcept { std::free(p); }
void operator delete(void* p) noexcept { std::free(p); }
void operator delete(void* p, std::size_t) noexcept { std::free(p); }
void operator delete[](void* p) noexcept { std::free(p); }
void operator delete[](void* p, std::size_t) noexcept { std::free(p); }

using dojo::Histogram;
using dojo::Registry;

namespace {

constexpr std::array<double, 4> kBounds{1.0, 2.5, 5.0, 10.0};

// Counts the allocations made while `f` runs.
template <typename F>
long allocations_in(F&& f) {
    const long before = g_allocations;
    f();
    return g_allocations - before;
}

// A C caller's view of the registry, freed like any RAII handle.
struct Handle {
    dojo_registry* raw = dojo_registry_new();
    Handle() = default;
    Handle(const Handle&) = delete;
    Handle& operator=(const Handle&) = delete;
    ~Handle() { dojo_registry_free(raw); }
};

std::string render(const dojo_registry* registry) {
    const std::size_t size = dojo_render(registry, nullptr, 0);
    std::string text(size + 1, '#');
    EXPECT_EQ(dojo_render(registry, text.data(), text.size()), size);
    EXPECT_EQ(text[size], '\0');
    text.resize(size);
    return text;
}

}  // namespace

TEST(CompileTime, NamesAreCheckedInConstantExpressions) {
    constexpr bool good = dojo::valid_name("http_requests_total");
    constexpr bool digits = dojo::valid_name("p99_latency_2");
    constexpr bool single = dojo::valid_name("x");
    constexpr bool leading_digit = dojo::valid_name("9lives");
    constexpr bool upper = dojo::valid_name("Latency");
    constexpr bool dash = dojo::valid_name("latency-ms");
    constexpr bool empty = dojo::valid_name("");
    constexpr bool underscore_first = dojo::valid_name("_private");
    EXPECT_TRUE(good);
    EXPECT_TRUE(digits);
    EXPECT_TRUE(single);
    EXPECT_FALSE(leading_digit);
    EXPECT_FALSE(upper);
    EXPECT_FALSE(dash);
    EXPECT_FALSE(empty);
    EXPECT_FALSE(underscore_first);
    EXPECT_TRUE(dojo::valid_name(std::string(64, 'a')));
    EXPECT_FALSE(dojo::valid_name(std::string(65, 'a')));
}

TEST(CompileTime, BoundsAreComputedByTheCompiler) {
    constexpr auto bounds = dojo::exponential_bounds<5>(1.0, 2.0);
    EXPECT_EQ(bounds, (std::array<double, 5>{1.0, 2.0, 4.0, 8.0, 16.0}));
    constexpr auto ms = dojo::exponential_bounds<3>(0.5, 10.0);
    EXPECT_EQ(ms, (std::array<double, 3>{0.5, 5.0, 50.0}));
    EXPECT_THROW((void)dojo::exponential_bounds<3>(0.0, 2.0), std::invalid_argument);
    EXPECT_THROW((void)dojo::exponential_bounds<3>(1.0, 1.0), std::invalid_argument);
    EXPECT_THROW((void)dojo::exponential_bounds<3>(std::nan(""), 2.0), std::invalid_argument);
}

TEST(Histogram, CountsEachValueInItsBucket) {
    Histogram h(kBounds);
    for (const double v : {0.2, 1.0, 1.1, 2.5, 7.0, 10.0, 10.5, 1e9, -3.0}) {
        h.record(v);
    }
    EXPECT_EQ(std::vector(h.buckets().begin(), h.buckets().end()),
              (std::vector<std::uint64_t>{3, 2, 0, 2, 2}))
        << "a value equal to a bound belongs to that bound's bucket";
    EXPECT_EQ(h.count(), 9U);
    EXPECT_DOUBLE_EQ(h.sum(), 0.2 + 1.0 + 1.1 + 2.5 + 7.0 + 10.0 + 10.5 + 1e9 - 3.0);
    h.record(std::numeric_limits<double>::quiet_NaN());
    EXPECT_EQ(h.count(), 9U) << "NaN is ignored";
}

TEST(Histogram, RejectsBadBounds) {
    EXPECT_THROW(Histogram(std::vector<double>{}), std::invalid_argument);
    EXPECT_THROW(Histogram(std::vector<double>{1, 1, 2}), std::invalid_argument);
    EXPECT_THROW(Histogram(std::vector<double>{3, 2}), std::invalid_argument);
    EXPECT_THROW(Histogram(std::vector<double>{1, std::numeric_limits<double>::infinity()}),
                 std::invalid_argument);
    EXPECT_THROW(Histogram(std::vector<double>{std::nan(""), 1}), std::invalid_argument);
    EXPECT_NO_THROW(Histogram(std::vector<double>{-1, 0, 1}));
}

TEST(Histogram, RecordNeverAllocatesAndIsNoexcept) {
    static_assert(noexcept(std::declval<Histogram&>().record(1.0)));
    static_assert(std::is_nothrow_move_constructible_v<Histogram>);
    Histogram h(dojo::exponential_bounds<20>(0.001, 2.0));
    const long during = allocations_in([&h] {
        for (int i = 0; i < 200'000; ++i) {
            h.record(static_cast<double>(i % 1000) / 7.0);
        }
    });
    EXPECT_EQ(during, 0);
    EXPECT_EQ(h.count(), 200'000U);
}

TEST(Histogram, Quantiles) {
    Histogram h(kBounds);
    EXPECT_EQ(h.quantile(0.5), 0.0) << "empty";
    for (int i = 0; i < 90; ++i) {
        h.record(0.5);  // bucket le=1
    }
    for (int i = 0; i < 9; ++i) {
        h.record(4.0);  // le=5
    }
    h.record(50.0);  // above every bound
    EXPECT_EQ(h.quantile(0.0), 1.0);
    EXPECT_EQ(h.quantile(0.5), 1.0);
    EXPECT_EQ(h.quantile(0.9), 1.0) << "the 90th value is still in the first bucket";
    EXPECT_EQ(h.quantile(0.91), 5.0);
    EXPECT_EQ(h.quantile(0.99), 5.0);
    EXPECT_EQ(h.quantile(1.0), std::numeric_limits<double>::infinity());
    EXPECT_THROW((void)h.quantile(1.5), std::invalid_argument);
    EXPECT_THROW((void)h.quantile(-0.1), std::invalid_argument);
    EXPECT_THROW((void)h.quantile(std::nan("")), std::invalid_argument);
}

TEST(Registry, DefinesFindsAndRejects) {
    Registry r;
    Histogram& latency = r.define("latency_ms", kBounds);
    latency.record(3.0);
    ASSERT_NE(r.find("latency_ms"), nullptr);
    EXPECT_EQ(r.find("latency_ms")->count(), 1U);
    EXPECT_EQ(r.find("nope"), nullptr);
    EXPECT_THROW(r.define("latency_ms", kBounds), std::invalid_argument);
    EXPECT_THROW(r.define("Bad-Name", kBounds), std::invalid_argument);
    EXPECT_THROW(r.define("empty_bounds", std::span<const double>{}), std::invalid_argument);
    EXPECT_EQ(r.size(), 1U) << "failed definitions add nothing";
    const Registry& view = r;
    EXPECT_EQ(view.find("latency_ms"), &latency);
}

TEST(Registry, FindNeverAllocatesEvenForLongNames) {
    Registry r;
    const std::string name = "http_server_request_duration_seconds";  // longer than any SSO
    r.define(name, kBounds);
    const std::string_view view = name;
    const Histogram* found = nullptr;
    const long during = allocations_in([&] {
        for (int i = 0; i < 1000; ++i) {
            found = r.find(view);
        }
        (void)r.find("http_server_request_duration_seconds_missing");
    });
    EXPECT_EQ(during, 0) << "build no std::string to look up a std::string_view";
    EXPECT_NE(found, nullptr);
}

TEST(Registry, RendersPrometheusTextSortedByName) {
    Registry r;
    r.define("zeta_bytes", std::vector<double>{1024});
    Histogram& latency = r.define("latency_ms", kBounds);
    for (const double v : {0.5, 0.75, 2.0, 7.25, 30.0}) {
        latency.record(v);
    }
    r.find("zeta_bytes")->record(10);
    EXPECT_EQ(r.render(),
              "latency_ms_bucket{le=\"1\"} 2\n"
              "latency_ms_bucket{le=\"2.5\"} 3\n"
              "latency_ms_bucket{le=\"5\"} 3\n"
              "latency_ms_bucket{le=\"10\"} 4\n"
              "latency_ms_bucket{le=\"+Inf\"} 5\n"
              "latency_ms_sum 40.5\n"
              "latency_ms_count 5\n"
              "zeta_bytes_bucket{le=\"1024\"} 1\n"
              "zeta_bytes_bucket{le=\"+Inf\"} 1\n"
              "zeta_bytes_sum 10\n"
              "zeta_bytes_count 1\n");
    EXPECT_EQ(Registry{}.render(), "");
}

TEST(Registry, RenderSortsManyNames) {
    Registry r;
    for (const char* name : {"queue_depth", "bytes_out", "zz_last", "http_errors", "a_first",
                             "cache_hits", "mem_rss", "gc_pause"}) {
        r.define(name, kBounds);
    }
    std::vector<std::string> order;
    const std::string text = r.render();
    for (std::size_t at = text.find("_count "); at != std::string::npos;
         at = text.find("_count ", at + 1)) {
        const std::size_t start = text.rfind('\n', at);
        order.push_back(text.substr(start == std::string::npos ? 0 : start + 1,
                                    at - (start == std::string::npos ? 0 : start + 1)));
    }
    EXPECT_EQ(order, (std::vector<std::string>{"a_first", "bytes_out", "cache_hits", "gc_pause",
                                               "http_errors", "mem_rss", "queue_depth", "zz_last"}));
}

TEST(CInterface, DefineRecordRender) {
    const Handle h;
    ASSERT_NE(h.raw, nullptr);
    const double bounds[] = {1.0, 2.5};
    EXPECT_EQ(dojo_define(h.raw, "jobs", bounds, 2), DOJO_OK);
    EXPECT_EQ(dojo_record(h.raw, "jobs", 2.0), DOJO_OK);
    EXPECT_EQ(dojo_record(h.raw, "jobs", 0.1), DOJO_OK);
    EXPECT_EQ(dojo_record(h.raw, "missing", 1.0), DOJO_UNKNOWN_METRIC);
    EXPECT_EQ(render(h.raw),
              "jobs_bucket{le=\"1\"} 1\n"
              "jobs_bucket{le=\"2.5\"} 2\n"
              "jobs_bucket{le=\"+Inf\"} 2\n"
              "jobs_sum 2.1\n"
              "jobs_count 2\n");
}

TEST(CInterface, NoExceptionCrossesTheBoundary) {
    const Handle h;
    const double bounds[] = {1.0, 2.0};
    const double bad[] = {2.0, 1.0};
    EXPECT_EQ(dojo_define(h.raw, "ok", bounds, 2), DOJO_OK);
    EXPECT_EQ(dojo_define(h.raw, "ok", bounds, 2), DOJO_INVALID_ARGUMENT) << "defined twice";
    EXPECT_EQ(dojo_define(h.raw, "Bad Name", bounds, 2), DOJO_INVALID_ARGUMENT);
    EXPECT_EQ(dojo_define(h.raw, "unsorted", bad, 2), DOJO_INVALID_ARGUMENT);
    EXPECT_EQ(dojo_define(h.raw, "none", bounds, 0), DOJO_INVALID_ARGUMENT);
    EXPECT_EQ(dojo_define(h.raw, "null_bounds", nullptr, 3), DOJO_INVALID_ARGUMENT);
    EXPECT_EQ(dojo_define(nullptr, "x", bounds, 2), DOJO_INVALID_ARGUMENT);
    EXPECT_EQ(dojo_define(h.raw, nullptr, bounds, 2), DOJO_INVALID_ARGUMENT);
    EXPECT_EQ(dojo_record(h.raw, "ok", std::nan("")), DOJO_INVALID_ARGUMENT);
    EXPECT_EQ(dojo_record(nullptr, "ok", 1.0), DOJO_INVALID_ARGUMENT);
    EXPECT_EQ(dojo_record(h.raw, nullptr, 1.0), DOJO_INVALID_ARGUMENT);
}

TEST(CInterface, OutOfMemoryIsAStatusCode) {
    const Handle h;
    const double bounds[] = {1.0, 2.0};
    int status = DOJO_OK;
    EXPECT_NO_THROW(status = [&] {
        g_fail_next = true;  // the next allocation, inside dojo_define, throws std::bad_alloc
        return dojo_define(h.raw, "a_name_long_enough_to_need_the_heap", bounds, 2);
    }());
    g_fail_next = false;
    EXPECT_EQ(status, DOJO_OUT_OF_MEMORY);
    EXPECT_EQ(dojo_record(h.raw, "a_name_long_enough_to_need_the_heap", 1.0), DOJO_UNKNOWN_METRIC)
        << "a failed define leaves nothing behind";
    dojo_registry* none = nullptr;
    EXPECT_NO_THROW(none = [] {
        g_fail_next = true;
        return dojo_registry_new();
    }());
    g_fail_next = false;
    EXPECT_EQ(none, nullptr);
    dojo_registry_free(nullptr);  // must be fine
}

TEST(CInterface, RecordNeverAllocates) {
    const Handle h;
    const double bounds[] = {1.0, 2.0, 4.0};
    ASSERT_EQ(dojo_define(h.raw, "http_server_request_duration_seconds", bounds, 3), DOJO_OK);
    const long during = allocations_in([&h] {
        for (int i = 0; i < 10'000; ++i) {
            (void)dojo_record(h.raw, "http_server_request_duration_seconds", 1.5);
        }
    });
    EXPECT_EQ(during, 0);
}

TEST(CInterface, RenderTruncatesLikeSnprintf) {
    const Handle h;
    const double bounds[] = {1.0};
    ASSERT_EQ(dojo_define(h.raw, "q", bounds, 1), DOJO_OK);
    const std::string full = "q_bucket{le=\"1\"} 0\nq_bucket{le=\"+Inf\"} 0\nq_sum 0\nq_count 0\n";
    char small[8];
    std::fill(std::begin(small), std::end(small), '#');
    EXPECT_EQ(dojo_render(h.raw, small, sizeof small), full.size());
    EXPECT_EQ(std::string(small), full.substr(0, 7)) << "7 characters and a NUL";
    char one[1] = {'#'};
    EXPECT_EQ(dojo_render(h.raw, one, 1), full.size());
    EXPECT_EQ(one[0], '\0');
    char untouched[2] = {'#', '#'};
    EXPECT_EQ(dojo_render(h.raw, untouched, 0), full.size());
    EXPECT_EQ(untouched[0], '#') << "capacity 0 writes nothing";
    char empty[4] = {'#', '#', '#', '#'};
    EXPECT_EQ(dojo_render(nullptr, empty, sizeof empty), 0U);
    EXPECT_EQ(empty[0], '\0');
}
