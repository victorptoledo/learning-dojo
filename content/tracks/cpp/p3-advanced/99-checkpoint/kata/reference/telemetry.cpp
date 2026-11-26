#include "telemetry.hpp"

#include <algorithm>
#include <format>
#include <ranges>
#include <thread>
#include <utility>

namespace dojo {

namespace {

template <typename... Fs>
struct Overloaded : Fs... {
    using Fs::operator()...;
};

}  // namespace

const std::string& sensor_of(const Event& event) {
    return std::visit([](const auto& e) -> const std::string& { return e.sensor; }, event);
}

std::string describe(const Event& event) {
    return std::visit(
        Overloaded{
            [](const Reading& r) { return std::format("{} = {}", r.sensor, r.value); },
            [](const Alarm& a) { return std::format("{} ALARM: {}", a.sensor, a.message); },
            [](const Heartbeat& h) { return std::format("{} alive", h.sensor); },
        },
        event);
}

std::optional<std::string> MaxAbove::check(const SensorStats& stats) const {
    if (stats.readings == 0 || stats.max <= limit_) {
        return std::nullopt;
    }
    return std::format("{}: max {} above {}", stats.sensor, stats.max, limit_);
}

std::optional<std::string> TooManyAlarms::check(const SensorStats& stats) const {
    if (stats.alarms <= limit_) {
        return std::nullopt;
    }
    return std::format("{}: {} alarms (limit {})", stats.sensor, stats.alarms, limit_);
}

std::optional<std::string> Silent::check(const SensorStats& stats) const {
    if (stats.heartbeats > 0) {
        return std::nullopt;
    }
    return std::format("{}: no heartbeat", stats.sensor);
}

void Hub::add_rule(std::unique_ptr<Rule> rule) {
    const std::scoped_lock lock(mutex_);
    rules_.push_back(std::move(rule));
}

std::size_t Hub::subscribe(Listener listener) {
    const std::scoped_lock lock(mutex_);
    listeners_.push_back(std::move(listener));
    return listeners_.size();
}

void Hub::publish(const Event& event) {
    const std::scoped_lock lock(mutex_);
    const std::string& sensor = sensor_of(event);
    auto found = stats_.find(sensor);
    if (found == stats_.end()) {
        found = stats_.emplace(sensor, SensorStats{.sensor = sensor}).first;
    }
    SensorStats& stats = found->second;
    std::visit(Overloaded{
                   [&stats](const Reading& r) {
                       if (stats.readings == 0) {
                           stats.min = r.value;
                           stats.max = r.value;
                       } else {
                           stats.min = std::min(stats.min, r.value);
                           stats.max = std::max(stats.max, r.value);
                       }
                       stats.sum += r.value;
                       ++stats.readings;
                   },
                   [&stats](const Alarm&) { ++stats.alarms; },
                   [&stats](const Heartbeat&) { ++stats.heartbeats; },
               },
               event);
    ++published_;
    for (auto& listener : listeners_) {
        listener(event);
    }
}

std::vector<SensorStats> Hub::snapshot() const {
    const std::scoped_lock lock(mutex_);
    return stats_ | std::views::values | std::ranges::to<std::vector>();
}

std::size_t Hub::published() const {
    const std::scoped_lock lock(mutex_);
    return published_;
}

std::generator<std::string> Hub::alerts() const {
    // The copy is taken under the lock (inside snapshot) and lives in this coroutine's frame;
    // the lock is already released when the first alert reaches the caller.
    const std::vector<SensorStats> stats = snapshot();
    std::vector<const Rule*> rules;
    {
        const std::scoped_lock lock(mutex_);
        rules = rules_ | std::views::transform(&std::unique_ptr<Rule>::get) |
                std::ranges::to<std::vector<const Rule*>>();
    }
    for (const SensorStats& sensor : stats) {
        for (const Rule* rule : rules) {
            if (auto message = rule->check(sensor)) {
                co_yield std::move(*message);
            }
        }
    }
}

std::vector<std::string> top_by_max(std::span<const SensorStats> stats, std::size_t n) {
    auto with_readings = stats |
                         std::views::filter([](const SensorStats& s) { return s.readings > 0; }) |
                         std::ranges::to<std::vector>();
    std::ranges::sort(with_readings, [](const SensorStats& a, const SensorStats& b) {
        return a.max != b.max ? a.max > b.max : a.sensor < b.sensor;
    });
    return with_readings | std::views::take(n) | std::views::transform(&SensorStats::sensor) |
           std::ranges::to<std::vector>();
}

void publish_all(Hub& hub, std::span<const Event> events, std::size_t threads) {
    if (events.empty()) {
        return;
    }
    threads = std::max<std::size_t>(threads, 1);
    const std::size_t chunk = (events.size() + threads - 1) / threads;
    std::vector<std::jthread> workers;
    workers.reserve(threads);
    for (auto part : events | std::views::chunk(chunk)) {
        workers.emplace_back([&hub, part] {
            for (const Event& event : part) {
                hub.publish(event);
            }
        });
    }
}  // the jthreads join here

}  // namespace dojo
