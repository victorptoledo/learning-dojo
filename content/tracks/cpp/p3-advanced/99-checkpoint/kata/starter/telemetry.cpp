#include "telemetry.hpp"

#include <stdexcept>

namespace dojo {

const std::string& sensor_of(const Event& event) {
    (void)event;
    throw std::logic_error("todo: write sensor_of");
}

std::string describe(const Event& event) {
    (void)event;
    throw std::logic_error("todo: write describe");
}

std::optional<std::string> MaxAbove::check(const SensorStats& stats) const {
    (void)stats;
    (void)limit_;
    throw std::logic_error("todo: write MaxAbove::check");
}

std::optional<std::string> TooManyAlarms::check(const SensorStats& stats) const {
    (void)stats;
    (void)limit_;
    throw std::logic_error("todo: write TooManyAlarms::check");
}

std::optional<std::string> Silent::check(const SensorStats& stats) const {
    (void)stats;
    throw std::logic_error("todo: write Silent::check");
}

void Hub::add_rule(std::unique_ptr<Rule> rule) {
    (void)rule;
    throw std::logic_error("todo: write Hub::add_rule");
}

std::size_t Hub::subscribe(Listener listener) {
    (void)listener;
    throw std::logic_error("todo: write Hub::subscribe");
}

void Hub::publish(const Event& event) {
    (void)event;
    throw std::logic_error("todo: write Hub::publish");
}

std::vector<SensorStats> Hub::snapshot() const {
    throw std::logic_error("todo: write Hub::snapshot");
}

std::size_t Hub::published() const {
    throw std::logic_error("todo: write Hub::published");
}

std::generator<std::string> Hub::alerts() const {
    // A function with co_yield in its body is a coroutine; this one is not one yet.
    throw std::logic_error("todo: write Hub::alerts");
}

std::vector<std::string> top_by_max(std::span<const SensorStats> stats, std::size_t n) {
    (void)stats;
    (void)n;
    throw std::logic_error("todo: write top_by_max");
}

void publish_all(Hub& hub, std::span<const Event> events, std::size_t threads) {
    (void)hub;
    (void)events;
    (void)threads;
    throw std::logic_error("todo: write publish_all");
}

}  // namespace dojo
