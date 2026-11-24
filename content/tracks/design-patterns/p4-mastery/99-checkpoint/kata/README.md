# Notification Dispatcher (final exam)

`Dispatcher` sends notifications by email, SMS and push, and holds SMS and push messages during
quiet hours. Three teams each added a layer "to make it extensible": a global `Config`
singleton, a `NotificationStrategy` per channel, a `NotificationStrategyFactory` for the
strategies and a `NotificationStrategyFactoryProvider` singleton for the factory. Under all
that, `send` still switches on the channel name twice, keeps held messages in two parallel
lists, and repeats the quiet-hours rule four times.

This exam is about judgement: **remove** the patterns that cost more than they give, and keep
or add the ones that pay. The visible characterisation tests pass now; keep them green after
every small step.

## The rules

The public API of `Dispatcher` stays exactly as it is: `boolean send(String channel, String
user, String text, int hour)`, `int releaseHeld(int hour)`, `List<String> sent()`,
`List<String> held()` and `long costCents()`, with the same results, the same exceptions and the
same messages. Inside, you are free, with these constraints:

1. **Replace the type code with a sealed type.** Add `sealed interface Channel` whose permitted
   subtypes are three **records** (email, SMS, push), with `static Channel parse(String name)`
   (the exact lower-case names; anything else throws `IllegalArgumentException("unknown channel:
   " + name)`) and the rules that vary by channel as methods: `String format(String user, String
   text)`, `long costCents()` and `boolean waitsForQuietHours()`. Each channel name may appear
   as a string literal at most once in all your sources.
2. **Remove the needless indirection.** No type, nested ones included, whose name ends with
   `Factory`, `Provider` or `Strategy`; no `getInstance`; no static field that is not final.
3. **No long methods.** No method or constructor body has more than 15 non-blank lines.
4. **A smaller dispatcher.** `Dispatcher` has at most 70 non-blank lines (comments count as
   blank), no `switch`, compares no strings with literals, and keeps no parallel lists: at most
   two `List` fields.

## What it does today

- Channels: `email` formats `EMAIL <user>: <text>`, costs 0 and is sent at any hour; `sms`
  formats `SMS <user>: <text>`, cutting a text longer than 160 characters to its first 157 plus
  `...`, and costs 5 cents; `push` formats `PUSH <user>: <text>` with only the first 40
  characters of the text, and costs 1 cent.
- Errors, checked in this order: an unknown channel, a blank or missing text (`"text is
  required"`), an hour outside 0–23 (`"hour must be 0-23"`).
- Quiet hours run from 22:00 to 06:59. During them an SMS or a push is **held**: `send`
  returns `false`, the formatted message goes to `held()`, and it costs nothing yet.
- `releaseHeld(hour)` does nothing and returns 0 during quiet hours; otherwise it sends every
  held message in order, adds their cost and returns how many it sent.
- `sent()` and `held()` return copies.

## What the grading tests check

- Formats, cuts, costs, quiet-hour boundaries, held messages and their release, every error.
- `Channel` as described in rule 1, and each channel's rules on its own.
- Rules 2 to 4, by reading your sources and by reflection.

> Tip: start by moving one channel's format, cost and quiet-hours behaviour into a record that
> implements `Channel`, and make the factory return it. When all three are records, the factory,
> its provider and the strategies have nothing left to do: delete them, then the config.
