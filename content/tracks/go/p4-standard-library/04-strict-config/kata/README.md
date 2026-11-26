# Strict Config: JSON, Custom Types and Durations

A typo in a configuration file should stop a service at start-up, not make it run with a
default nobody chose. Write a strict loader for this JSON in `config.go`:

```json
{
  "name": "billing",
  "listen": ":8080",
  "timeout": "1m30s",
  "retries": 0,
  "tags": ["eu", "payments"]
}
```

| Function or method | Does |
| --- | --- |
| `Duration.MarshalText` | Writes the duration like `time.Duration.String`: `"1m30s"`, `"0s"`. |
| `Duration.UnmarshalText` | Reads it with `time.ParseDuration`; a negative duration is an error that wraps `ErrNegativeDuration`. Because `Duration` is a `TextMarshaler`, JSON keeps it a **string**: `"timeout": 30` (no unit) is a `*json.UnmarshalTypeError`. |
| struct tags | `name`, `listen`, `timeout`, `retries` and `tags`. `Save` leaves out a zero `timeout` (`omitzero`), a nil `retries` and empty `tags` (`omitempty`). |
| `Load(r)` | Decodes **one** JSON object. Unknown fields are errors (`unknown field "listn"`), and so is anything but white space after the object (`ErrTrailingData`). Decoding errors are wrapped as `"decode config: ..."`, so `errors.As` still finds a `*json.SyntaxError`. Then the defaults: a missing `timeout` becomes `DefaultTimeout`, and a missing `retries` becomes `DefaultRetries`, while an explicit `"retries": 0` stays 0 (that is why the field is a pointer). Last, `Validate`. |
| `Validate()` | Reports **every** problem at once, as `errors.Join` of `*FieldError` values: `name: is required`; `listen: must be host:port` when `net.SplitHostPort` fails, or `listen: port must be a number from 1 to 65535`; `timeout: must be at most 1m0s`; `retries: must be from 0 to 10`; `tags: must not be empty` and `tags: "eu" appears twice`, once per bad tag. |
| `Save(w, c)` | Writes `c` as JSON indented with two spaces, ending in a newline (what `json.Encoder` with `SetIndent("", "  ")` writes). `Load(Save(c))` gives `c` back. |

Run the visible test with `go test`. The dojo runs 11 tests: 1 in `config_test.go` and 10
hidden ones, plus `go vet` and `gofmt` on `config.go`.
