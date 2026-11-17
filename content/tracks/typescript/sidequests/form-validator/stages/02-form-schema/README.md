Now the whole form. A **schema** maps each field name to its rules, and the types follow the
names you wrote. Export from `validator.ts`:

- `type Schema`: any object whose values are `readonly Rule[]`.
- `defineSchema(schema)`: returns the schema it gets, unchanged, typed so that the field
  names are known and the rule kinds stay literal (a generic constrained by `Schema`):
  `{ kind: "required" }` written inside the call must compile without `as const`.
- `type FieldError<K extends string> = { field: K; messages: string[] }`.
- `type ValidationResult<K extends string>`, a discriminated union on `ok`:
  `{ ok: true; values: Record<K, string> }` or `{ ok: false; errors: FieldError<K>[] }`.
- `validate(schema, input: unknown)`: checks every field of the schema with `checkField` and
  returns a `ValidationResult` whose `K` is the schema's field names. `result.values.name`
  must compile for a schema with `name`, `result.values.phone` must not, and neither must
  reading `values` before checking `result.ok`.

How it behaves:

- `input` comes from outside (`JSON.parse`, a request body): when it is not a plain object
  (`null`, an array, a string...), every field is treated as missing. Only the input's own
  properties count: a field the input only inherits (from its prototype) is missing.
- `values` has every field of the schema, and only those, with the trimmed text; a missing
  optional field is `""`. Extra fields in the input are ignored.
- `errors` lists only the fields with messages, in the schema's order, and is never empty
  when `ok` is `false`.
