Some rules need the rest of the form. Two new rule kinds:

| `kind` | Other fields | Breaks when | Message |
|---|---|---|---|
| `sameAs` | `field: K` | the value differs from the value of `field` | `must match <field>` |
| `custom` | `check: (value: string, form: Record<K, string>) => string \| null` | `check` returns a string | that string |

`K` is the schema's field names, so `Rule` becomes generic: `Rule<K extends string = string>`
(the default keeps `Rule` and `checkField` from the first stage working). Then:

- `sameAs` may only name a field of the same schema: `{ kind: "sameAs", field: "pasword" }`
  in a schema without `pasword` must not compile.
- `check` gets the trimmed value and every value of the form, typed: inside it,
  `form.password` is a `string` and `form.nope` does not compile.
- `validate` gives the rules of each field the trimmed values of the whole form, a missing
  field being `""`, the same object it returns as `values`.
- Like the other rules, both only run on values that are there: an empty optional field is
  still valid, and an empty required one only gets `is required`.

```ts
const signup = defineSchema({
  password: [{ kind: "required" }, { kind: "minLength", min: 8 }],
  confirm: [{ kind: "required" }, { kind: "sameAs", field: "password" }],
  nickname: [
    { kind: "custom", check: (value, form) => (value === form.password ? "must not be the password" : null) },
  ],
});
```
