# Encoding: JSON, CSV and Struct Tags

Services talk JSON, spreadsheets export CSV, and configuration lives in files. Go's
`encoding` packages turn those bytes into typed values and back, steered by **struct tags**
and by **methods on your own types**.

## JSON in and out

```go
type Order struct {
	ID       int64     `json:"id"`
	Customer string    `json:"customer"`
	Items    []Item    `json:"items"`
	PaidAt   time.Time `json:"paid_at,omitzero"`
	Note     string    `json:"note,omitempty"`
	internal string    // unexported: never encoded or decoded
}

data, err := json.Marshal(order)             // []byte
err = json.Unmarshal(data, &order)           // into a pointer
json.MarshalIndent(order, "", "  ")          // for people
```

- Only **exported** fields take part. Without a tag, the field name is used, and decoding
  matches names **case-insensitively** (`"CUSTOMER"` fills `Customer`), which surprises
  people; the tag fixes the name you write.
- `time.Time` encodes as RFC 3339, `[]byte` as base64, a nil slice as `null` and an empty
  one as `[]`, maps with string (or integer, or `TextMarshaler`) keys as objects.
- Decoding into `any` gives `map[string]any`, `[]any`, `string`, `bool`, `nil` and
  **`float64` for every number**: large IDs lose precision. Decode into typed fields, or call
  `Decoder.UseNumber()`.

## Tags and their options

| Tag | Effect |
| --- | --- |
| `json:"name"` | the key in JSON |
| `json:"-"` | never encoded or decoded |
| `json:"name,omitempty"` | left out when `false`, 0, `""`, nil, or an empty slice or map |
| `json:"name,omitzero"` | left out when the value is zero, or when its `IsZero()` returns true (Go 1.24) |
| `json:"name,string"` | a number or bool written inside a string: `"42"` |

`omitempty` never leaves out a struct, so a zero `time.Time` used to be written as
`"0001-01-01T00:00:00Z"`. `omitzero` fixes that: it calls `IsZero`.

**Missing or zero?** After decoding, a `Retries int` of 0 cannot tell "the file said 0" from
"the file said nothing". When that difference matters (defaults, PATCH requests), use a
pointer: `Retries *int` is nil when the key was absent.

Tags are plain strings that packages read with `reflect.StructTag.Get("json")`. `go vet`
checks their syntax (`structtag`), so a missing quote does not pass silently.

## Strict decoding with json.Decoder

`json.Unmarshal` ignores unknown keys and needs the whole input in memory. For files, bodies
and streams, use a `Decoder`:

```go
dec := json.NewDecoder(r)
dec.DisallowUnknownFields()                  // "listn" is an error, not ignored
if err := dec.Decode(&cfg); err != nil {
	return fmt.Errorf("decode config: %w", err)
}
if err := dec.Decode(&struct{}{}); !errors.Is(err, io.EOF) {
	return errors.New("data after the JSON object") // Decode reads one value at a time
}
```

The errors are typed, so callers can ask what went wrong:

- `*json.SyntaxError` has the byte `Offset` of broken JSON;
- `*json.UnmarshalTypeError` says which `Field` got the wrong kind of value;
- an error your own `UnmarshalJSON` or `UnmarshalText` returns comes back so that
  `errors.Is` still finds it.

A `Decoder` also reads streams of values (`for dec.More()`), the format of many log files.

## Custom forms: Marshaler and TextMarshaler

A type controls its own encoding with methods:

```go
type Duration time.Duration

func (d Duration) MarshalText() ([]byte, error) { return []byte(time.Duration(d).String()), nil }

func (d *Duration) UnmarshalText(b []byte) error {
	v, err := time.ParseDuration(string(b))
	if err != nil {
		return err
	}
	*d = Duration(v)
	return nil
}
```

- Prefer **`MarshalText`** when the JSON form is a string (durations, money, IDs, enums):
  the encoder adds the quotes and escapes, and the same method works for map keys, XML and
  flag values.
- Write `MarshalJSON` and `UnmarshalJSON` only when the form is not a string, and **never**
  call `json.Marshal(t)` on the same type inside it (infinite recursion). Convert first:
  `type plain Order; json.Marshal(plain(o))`.
- Put `UnmarshalText` on the **pointer** receiver; it must change the value.
- `json.RawMessage` keeps a part undecoded, for envelopes such as
  `{"type": "...", "data": {...}}` that you decode in two steps.

## encoding/json/v2

Go 1.25 added `encoding/json/v2` and `encoding/json/jsontext` as an **experiment**, behind
`GOEXPERIMENT=jsonv2`. It is faster, rejects duplicate keys and invalid UTF-8, and matches
names case-sensitively. Check the release notes of your Go version before you depend on it;
the v1 package stays and is what this course uses.

## CSV with encoding/csv

```go
r := csv.NewReader(f)
r.TrimLeadingSpace = true
header, err := r.Read()                      // the first record
for {
	record, err := r.Read()
	if errors.Is(err, io.EOF) {
		break
	}
	if errors.Is(err, csv.ErrFieldCount) {   // wrong number of fields: report, continue
		continue
	}
	if err != nil {
		return err                           // *csv.ParseError with the line
	}
	line, _ := r.FieldPos(0)                 // the line where this record starts
	...
}
```

- Records are `[]string`; there are no tags, so map columns **by header name** once
  (`map[string]int`), never by position: exports change column order.
- Quoted fields may hold commas and even newlines, which is why you cannot split lines by
  `,` yourself and why line numbers come from `FieldPos`, not from a counter.
- `FieldsPerRecord` is 0 by default: the first record sets the count, and later records of
  another length return `ErrFieldCount` **together with** the record.
- Spreadsheets often start the file with a byte order mark, `"﻿"`. Trim it from the
  first header cell.
- `csv.Writer` quotes what needs quoting; call `Flush` and then check `Error()`.

## Summary

- Struct tags name fields and leave them out: `omitempty` for empty, `omitzero` for zero
  (and `IsZero`); pointers tell missing from zero.
- Use a `Decoder` with `DisallowUnknownFields` and a trailing-data check for strict input,
  and `errors.As` for its typed errors.
- Give string-shaped types `MarshalText` and `UnmarshalText`; use `MarshalJSON` only for
  other shapes, without recursing.
- `encoding/csv` handles quoting; map columns by header, use `FieldPos` for lines, trim the
  byte order mark.

## Further reading

- Package encoding/json: <https://pkg.go.dev/encoding/json>
- JSON and Go (Go blog): <https://go.dev/blog/json>
- A new experimental Go API for JSON (Go blog): <https://go.dev/blog/jsonv2-exp>
- Package encoding/csv: <https://pkg.go.dev/encoding/csv>
- roadmap.sh, Go topic "JSON": <https://roadmap.sh/golang>
