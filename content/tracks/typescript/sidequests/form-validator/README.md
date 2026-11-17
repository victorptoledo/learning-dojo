# Sidequest: Form validator

Every sign-up page needs the same thing: check what the user typed and say, field by field,
what is wrong. Build `validator.ts`, a small validation library where the compiler knows the
rules and the field names, so a typo in a schema is a type error and not a bug in production.

The library is checked from the outside, by contract tests: runtime tests (`node:test`) that
call your exported functions and compare the messages, and type-level tests that check what
compiles and what must not. The names each stage asks you to export are fixed; everything
behind them (helper functions, a `switch` per rule kind, `assertNever`, extra types) is yours.

```ts
import { defineSchema, validate } from "./validator.ts";

const signup = defineSchema({
  name: [{ kind: "required" }, { kind: "minLength", min: 2 }],
  age: [{ kind: "number", min: 18 }],
});

const result = validate(signup, { name: "Ana", age: "17" });
if (!result.ok) console.log(result.errors); // [{ field: "age", messages: ["must be at least 18"] }]
```

No `any`, no libraries (zod would do the job for you): plain TypeScript 7 in strict mode.
`npm test` type-checks and runs the tests in `tests/`, which are yours too: add your own.
