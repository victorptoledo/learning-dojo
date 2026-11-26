# Telemetry Hub

Write the functions in `telemetry.cpp`. The header already declares everything, with the
members you need.

## Events and rules

| Name | Does |
| --- | --- |
| `sensor_of(event)` | The sensor of any of the three events. |
| `describe(event)` | `"temp = 21.5"`, `"temp ALARM: overheat"` or `"temp alive"` (`std::format`'s `{}` for the number). |
| `MaxAbove(limit)` | `"temp: max 91.5 above 90"` when the sensor has readings and its max is above the limit. |
| `TooManyAlarms(limit)` | `"temp: 3 alarms (limit 2)"` when there are more alarms than the limit. |
| `Silent()` | `"temp: no heartbeat"` when the sensor never sent one. |

## The hub

| Member | Does |
| --- | --- |
| `publish(event)` | Creates the sensor's stats on its first event and updates them: a reading counts and moves `min`, `max` and `sum` (the first reading sets both `min` and `max`), an alarm or a heartbeat counts. Then calls every listener with the event, in the order they subscribed. Many threads call it at once. |
| `subscribe(listener)` | Adds a listener (it may own move-only things) and returns how many there are. |
| `snapshot()` | A copy of every sensor's stats, sorted by sensor name. |
| `published()` | How many events were published. |
| `alerts()` | A `std::generator<std::string>`: for each sensor of a snapshot taken when iteration starts, each rule in the order it was added, one message at a time. The rules are checked lazily, only as the caller asks for the next alert, and nothing is locked while the caller holds one, so it may call `publish` inside the loop. |

## Free functions

| Function | Does |
| --- | --- |
| `top_by_max(stats, n)` | The names of the `n` sensors with the highest max, highest first, ties by name; sensors without readings are left out. The input is not changed. |
| `publish_all(hub, events, threads)` | Splits the events into `threads` chunks (0 counts as 1) of `ceil(size / threads)` events and publishes each chunk on its own `std::jthread`. Returns when all are done. |

```cpp
dojo::Hub hub;
hub.add_rule(std::make_unique<dojo::MaxAbove>(50));
dojo::publish_all(hub, events, 4);
for (const std::string& alert : hub.alerts()) {
    std::println("{}", alert);
}
```

The dojo runs 15 hidden tests and the visible one under ThreadSanitizer, plus the tool checks
on both files.
