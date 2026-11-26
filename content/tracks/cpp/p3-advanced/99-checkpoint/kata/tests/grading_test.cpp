#include <gtest/gtest.h>

#include <algorithm>
#include <atomic>
#include <cstddef>
#include <format>
#include <memory>
#include <ranges>
#include <string>
#include <thread>
#include <type_traits>
#include <vector>

#include "telemetry.hpp"

using dojo::Alarm;
using dojo::Event;
using dojo::Heartbeat;
using dojo::Hub;
using dojo::Reading;
using dojo::SensorStats;

namespace {

// A rule that counts how often it is checked and always fires.
class Counting final : public dojo::Rule {
public:
    explicit Counting(std::atomic<int>& calls) : calls_(&calls) {}
    std::optional<std::string> check(const SensorStats& stats) const override {
        ++*calls_;
        return std::format("{}: checked", stats.sensor);
    }

private:
    std::atomic<int>* calls_;
};

std::vector<std::string> all_alerts(const Hub& hub) {
    std::vector<std::string> out;
    for (std::string alert : hub.alerts()) {
        out.push_back(std::move(alert));
    }
    return out;
}

}  // namespace

TEST(Events, DescribeEveryKind) {
    EXPECT_EQ(dojo::describe(Reading{"temp", 21.5}), "temp = 21.5");
    EXPECT_EQ(dojo::describe(Alarm{"temp", "overheat"}), "temp ALARM: overheat");
    EXPECT_EQ(dojo::describe(Heartbeat{"fan"}), "fan alive");
    EXPECT_EQ(dojo::sensor_of(Alarm{"door", "open"}), "door");
}

TEST(Rules, AreAnOpenHierarchyWithAVirtualDestructor) {
    static_assert(std::has_virtual_destructor_v<dojo::Rule>);
    static_assert(std::is_abstract_v<dojo::Rule>);
    static_assert(!std::is_copy_constructible_v<dojo::Rule>, "copying a Rule would slice it");
    const SensorStats hot{.sensor = "temp", .readings = 2, .min = 80, .max = 91.5, .sum = 171.5};
    EXPECT_EQ(dojo::MaxAbove(90).check(hot), "temp: max 91.5 above 90");
    EXPECT_EQ(dojo::MaxAbove(91.5).check(hot), std::nullopt);
    EXPECT_EQ(dojo::MaxAbove(-5).check(SensorStats{.sensor = "idle"}), std::nullopt);
    EXPECT_EQ(dojo::TooManyAlarms(2).check(SensorStats{.sensor = "temp", .alarms = 3}),
              "temp: 3 alarms (limit 2)");
    EXPECT_EQ(dojo::TooManyAlarms(3).check(SensorStats{.sensor = "temp", .alarms = 3}),
              std::nullopt);
    EXPECT_EQ(dojo::Silent().check(SensorStats{.sensor = "fan"}), "fan: no heartbeat");
    EXPECT_EQ(dojo::Silent().check(SensorStats{.sensor = "fan", .heartbeats = 1}), std::nullopt);
}

TEST(Hub, KeepsStatsPerSensorSortedByName) {
    Hub hub;
    hub.publish(Reading{"temp", 20});
    hub.publish(Reading{"temp", 25});
    hub.publish(Reading{"temp", 18});
    hub.publish(Alarm{"door", "open"});
    hub.publish(Heartbeat{"temp"});
    const auto stats = hub.snapshot();
    ASSERT_EQ(stats.size(), 2U);
    EXPECT_EQ(stats[0].sensor, "door");
    EXPECT_EQ(stats[0].alarms, 1U);
    EXPECT_EQ(stats[0].readings, 0U);
    EXPECT_EQ(stats[1].sensor, "temp");
    EXPECT_EQ(stats[1].readings, 3U);
    EXPECT_DOUBLE_EQ(stats[1].min, 18);
    EXPECT_DOUBLE_EQ(stats[1].max, 25);
    EXPECT_DOUBLE_EQ(stats[1].mean(), 21);
    EXPECT_EQ(stats[1].heartbeats, 1U);
    EXPECT_EQ(hub.published(), 5U);
}

TEST(Hub, MinAndMaxStartFromTheFirstReading) {
    Hub hub;
    hub.publish(Reading{"cold", -4});
    hub.publish(Reading{"cold", -9});
    hub.publish(Reading{"hot", 7});
    const auto stats = hub.snapshot();
    EXPECT_DOUBLE_EQ(stats[0].max, -4) << "max must not start at 0";
    EXPECT_DOUBLE_EQ(stats[0].min, -9);
    EXPECT_DOUBLE_EQ(stats[1].min, 7) << "min must not start at 0";
}

TEST(Hub, SnapshotIsACopy) {
    Hub hub;
    hub.publish(Reading{"a", 1});
    const auto before = hub.snapshot();
    hub.publish(Reading{"a", 5});
    EXPECT_EQ(before[0].readings, 1U);
    EXPECT_EQ(hub.snapshot()[0].readings, 2U);
}

TEST(Hub, ListenersSeeEveryEventInOrderAndMayBeMoveOnly) {
    Hub hub;
    auto seen = std::make_unique<std::vector<std::string>>();
    auto* view = seen.get();
    std::vector<int> order;
    hub.subscribe([log = std::move(seen)](const Event& e) { log->push_back(dojo::describe(e)); });
    EXPECT_EQ(hub.subscribe([&order](const Event&) { order.push_back(2); }), 2U);
    hub.publish(Heartbeat{"fan"});
    hub.publish(Reading{"fan", 3});
    EXPECT_EQ(*view, (std::vector<std::string>{"fan alive", "fan = 3"}));
    EXPECT_EQ(order.size(), 2U);
}

TEST(Alerts, EverySensorThenEveryRuleInOrder) {
    Hub hub;
    hub.add_rule(std::make_unique<dojo::MaxAbove>(50));
    hub.add_rule(std::make_unique<dojo::Silent>());
    hub.publish(Reading{"pump", 70});
    hub.publish(Reading{"boiler", 99});
    hub.publish(Heartbeat{"pump"});
    EXPECT_EQ(all_alerts(hub), (std::vector<std::string>{"boiler: max 99 above 50",
                                                         "boiler: no heartbeat",
                                                         "pump: max 70 above 50"}));
}

TEST(Alerts, AreLazy) {
    Hub hub;
    std::atomic<int> calls{0};
    hub.add_rule(std::make_unique<Counting>(calls));
    for (int i = 0; i < 50; ++i) {
        hub.publish(Heartbeat{std::format("s{:02}", i)});
    }
    std::vector<std::string> first;
    for (std::string alert : hub.alerts() | std::views::take(2)) {
        first.push_back(std::move(alert));
    }
    EXPECT_EQ(first, (std::vector<std::string>{"s00: checked", "s01: checked"}));
    EXPECT_LE(calls.load(), 3) << "alerts() must check the rules one alert at a time";
}

TEST(Alerts, TheCallerMayPublishWhileIterating) {
    Hub hub;
    hub.add_rule(std::make_unique<dojo::Silent>());
    hub.publish(Reading{"a", 1});
    hub.publish(Reading{"b", 2});
    std::vector<std::string> seen;
    for (std::string alert : hub.alerts()) {
        hub.publish(Heartbeat{"c"});  // would deadlock if the lock were held across co_yield
        seen.push_back(std::move(alert));
    }
    EXPECT_EQ(seen, (std::vector<std::string>{"a: no heartbeat", "b: no heartbeat"}))
        << "the snapshot is taken when iteration starts";
    EXPECT_EQ(hub.published(), 4U);
}

TEST(Alerts, OutliveTheExpressionThatCreatedThem) {
    Hub hub;
    hub.add_rule(std::make_unique<dojo::Silent>());
    hub.publish(Reading{"x", 1});
    auto pending = hub.alerts();  // nothing runs yet
    hub.publish(Reading{"y", 1});
    std::vector<std::string> seen;
    for (std::string alert : pending) {
        seen.push_back(std::move(alert));
    }
    EXPECT_EQ(seen, (std::vector<std::string>{"x: no heartbeat", "y: no heartbeat"}));
}

TEST(TopByMax, HighestFirstTiesByNameNoEmptySensors) {
    const std::vector<SensorStats> stats{
        {.sensor = "c", .readings = 1, .max = 5},
        {.sensor = "b", .readings = 2, .max = 9},
        {.sensor = "idle", .readings = 0, .max = 0, .alarms = 4},
        {.sensor = "a", .readings = 1, .max = 9},
        {.sensor = "d", .readings = 1, .max = -1},
    };
    EXPECT_EQ(dojo::top_by_max(stats, 3), (std::vector<std::string>{"a", "b", "c"}));
    EXPECT_EQ(dojo::top_by_max(stats, 10), (std::vector<std::string>{"a", "b", "c", "d"}));
    EXPECT_TRUE(dojo::top_by_max(stats, 0).empty());
    EXPECT_EQ(stats[0].sensor, "c") << "top_by_max must not reorder its input";
}

TEST(PublishAll, CountsEveryEventFromManyThreads) {
    std::vector<Event> events;
    for (int i = 0; i < 4000; ++i) {
        const std::string sensor = std::format("s{}", i % 7);
        if (i % 10 == 0) {
            events.emplace_back(Alarm{sensor, "x"});
        } else {
            events.emplace_back(Reading{sensor, static_cast<double>(i)});
        }
    }
    Hub hub;
    std::size_t heard = 0;  // the listeners are serialised, so a plain counter is fine
    hub.subscribe([&heard](const Event&) { ++heard; });
    dojo::publish_all(hub, events, 4);
    EXPECT_EQ(hub.published(), 4000U);
    EXPECT_EQ(heard, 4000U);
    std::size_t readings = 0;
    std::size_t alarms = 0;
    for (const SensorStats& s : hub.snapshot()) {
        readings += s.readings;
        alarms += s.alarms;
    }
    EXPECT_EQ(readings, 3600U);
    EXPECT_EQ(alarms, 400U);
    EXPECT_DOUBLE_EQ(hub.snapshot()[0].max, 3997);  // s0: i = 3997 is the last i % 7 == 0
}

TEST(PublishAll, ReallyUsesThreads) {
    std::vector<Event> events(64, Event{Heartbeat{"h"}});
    Hub hub;
    std::vector<std::thread::id> ids;
    hub.subscribe([&ids](const Event&) { ids.push_back(std::this_thread::get_id()); });
    dojo::publish_all(hub, events, 4);
    std::ranges::sort(ids);
    const auto distinct = std::ranges::distance(ids.begin(), std::ranges::unique(ids).begin());
    EXPECT_EQ(distinct, 4) << "64 events on 4 threads are 4 chunks of 16";
    EXPECT_EQ(std::ranges::count(ids, std::this_thread::get_id()), 0);
}

TEST(PublishAll, EdgeCases) {
    Hub hub;
    dojo::publish_all(hub, {}, 3);
    EXPECT_EQ(hub.published(), 0U);
    std::vector<Event> two{Reading{"a", 1}, Reading{"a", 2}};
    dojo::publish_all(hub, two, 0);
    EXPECT_EQ(hub.published(), 2U);
    dojo::publish_all(hub, two, 8);  // more threads than events
    EXPECT_EQ(hub.published(), 4U);
}

TEST(Hub, SnapshotsWhileOthersPublish) {
    Hub hub;
    std::atomic<bool> stop{false};
    std::jthread reader([&hub, &stop] {
        while (!stop.load()) {
            for (const SensorStats& s : hub.snapshot()) {
                EXPECT_GE(s.max, s.min);
            }
            (void)hub.published();
        }
    });
    std::vector<Event> events;
    for (int i = 0; i < 2000; ++i) {
        events.emplace_back(Reading{std::format("s{}", i % 3), static_cast<double>(i % 50)});
    }
    dojo::publish_all(hub, events, 3);
    stop = true;
    reader.join();
    EXPECT_EQ(hub.published(), 2000U);
}
