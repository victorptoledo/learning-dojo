Start with one field. Export from `validator.ts`:

- `type Rule`: a **discriminated union** on `kind`, one member per rule below, with exactly
  the fields shown (a `minLength` without `min`, or a `kind` that is not in the table, must
  not compile).
- `checkField(rules: readonly Rule[], value: unknown): string[]`: the messages of every rule
  the value breaks, in the order of `rules`; an empty array when it is valid.

| `kind` | Other fields | Breaks when | Message |
|---|---|---|---|
| `required` | none | the value is empty | `is required` |
| `minLength` | `min: number` | it has fewer than `min` characters | `must be at least <min> characters` |
| `maxLength` | `max: number` | it has more than `max` characters | `must be at most <max> characters` |
| `pattern` | `regex: RegExp`, `message: string` | `regex` does not match it | the rule's `message` |
| `number` | `min?: number`, `max?: number` | it is not a number | `must be a number` |
| | | it is below `min` | `must be at least <min>` |
| | | it is above `max` | `must be at most <max>` |
| `oneOf` | `options: readonly string[]` | it is not one of `options` | `must be one of: <options joined by ", ">` |

How a value is read:

- Form values are text. `undefined`, `null` and a string with only spaces are **empty**.
  Any other value that is not a string (a number, a boolean, an object) gives the single
  message `must be text`, whatever the rules.
- Strings are trimmed first: `"  ana "` is checked as `"ana"`.
- An empty value only breaks `required`. Without `required`, an empty field is valid: the
  other rules only check values that are there.
- Lengths count characters as `[...text].length` does, so an emoji is one character.
- A number is an optional `-`, digits, and optionally `.` and more digits: `42`, `-3`, `2.5`.
  `1e3`, `0x10`, `.5` and `12abc` are not numbers. `min` and `max` are inclusive, and a value
  that is not a number only gets `must be a number`.
