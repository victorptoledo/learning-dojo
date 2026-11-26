#pragma once

#include <cstddef>
#include <functional>
#include <generator>
#include <map>
#include <memory>
#include <mutex>
#include <optional>
#include <span>
#include <string>
#include <variant>
#include <vector>

namespace dojo {

// The three kinds of event a sensor sends. The set is closed, so it is a variant.
struct Reading {
    std::string sensor;
    double value = 0.0;
};
struct Alarm {
    std::string sensor;
    std::string message;
};
struct Heartbeat {
    std::string sensor;
};
using Event = std::variant<Reading, Alarm, Heartbeat>;

// The sensor of any event.
const std::string& sensor_of(const Event& event);

// One line per event: "temp = 21.5", "temp ALARM: overheat", "temp alive".
std::string describe(const Event& event);

// What the hub knows about one sensor.
struct SensorStats {
    std::string sensor;
    std::size_t readings = 0;
    double min = 0.0;  // of the readings; 0 while there are none
    double max = 0.0;
    double sum = 0.0;
    std::size_t alarms = 0;
    std::size_t heartbeats = 0;

    double mean() const { return readings == 0 ? 0.0 : sum / static_cast<double>(readings); }
};

// A check on one sensor's stats. The set of rules is open (anyone can add one), so it is a
// class hierarchy owned through std::unique_ptr.
class Rule {
public:
    Rule() = default;
    Rule(const Rule&) = delete;
    Rule& operator=(const Rule&) = delete;
    virtual ~Rule() = default;

    // A message when the stats break the rule, nothing otherwise.
    virtual std::optional<std::string> check(const SensorStats& stats) const = 0;
};

// "temp: max 91.5 above 90" when the sensor has readings and its max is above the limit.
class MaxAbove final : public Rule {
public:
    explicit MaxAbove(double limit) : limit_(limit) {}
    std::optional<std::string> check(const SensorStats& stats) const override;

private:
    double limit_;
};

// "temp: 3 alarms (limit 2)" when the sensor raised more alarms than the limit.
class TooManyAlarms final : public Rule {
public:
    explicit TooManyAlarms(std::size_t limit) : limit_(limit) {}
    std::optional<std::string> check(const SensorStats& stats) const override;

private:
    std::size_t limit_;
};

// "temp: no heartbeat" when the sensor never sent a heartbeat.
class Silent final : public Rule {
public:
    std::optional<std::string> check(const SensorStats& stats) const override;
};

// Collects events from many threads at once.
class Hub {
public:
    using Listener = std::move_only_function<void(const Event&)>;

    Hub() = default;
    Hub(const Hub&) = delete;
    Hub& operator=(const Hub&) = delete;

    // Adds a rule; alerts() checks the rules in the order they were added.
    void add_rule(std::unique_ptr<Rule> rule);

    // Calls `listener` with every event published from now on. Listeners are called one at a
    // time (never two at once), in the order they subscribed. Returns how many there are.
    std::size_t subscribe(Listener listener);

    // Updates the sensor's stats and calls the listeners. Safe to call from many threads.
    void publish(const Event& event);

    // A copy of every sensor's stats, sorted by sensor name. Safe to call while others publish.
    std::vector<SensorStats> snapshot() const;

    // How many events were published.
    std::size_t published() const;

    // Every alert, lazily: for each sensor of a snapshot taken when iteration starts (by name),
    // each rule in order. Nothing is locked while the caller holds an alert, so the caller may
    // publish from inside the loop.
    std::generator<std::string> alerts() const;

private:
    mutable std::mutex mutex_;
    std::map<std::string, SensorStats, std::less<>> stats_;
    std::vector<std::unique_ptr<Rule>> rules_;
    std::vector<Listener> listeners_;
    std::size_t published_ = 0;
};

// The names of the `n` sensors with the highest max reading, highest first (ties by name).
// Sensors without readings are left out.
std::vector<std::string> top_by_max(std::span<const SensorStats> stats, std::size_t n);

// Publishes every event, split into `threads` chunks (0 counts as 1) published on their own
// std::jthread. Returns when all of them are done.
void publish_all(Hub& hub, std::span<const Event> events, std::size_t threads);

}  // namespace dojo
