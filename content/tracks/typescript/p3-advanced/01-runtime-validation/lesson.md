# Runtime Validation at the Boundaries

Inside your program, the compiler guarantees that a `User` has a `name`. But types are erased,
so nothing checks the data that **enters** the program: an HTTP body, a JSON file, a message
from a queue, `process.env`, `localStorage`, a third-party API response. Typing those values
with `as User` is a promise the compiler cannot verify, and the bug shows up far from its
cause. The fix is a simple architecture rule:

> **Parse, don't validate.** At every boundary, turn `unknown` into a typed value with code
> that checks it, once. Everything behind the boundary trusts the types.

## Where any sneaks in

```ts
const body = await request.json();        // Promise<any>
const config = JSON.parse(text);          // any
const port = process.env.PORT;            // string | undefined (better, but still unparsed)
```

`any` from these APIs spreads silently. Assign them to `unknown` immediately
(`const data: unknown = JSON.parse(text)`) so the compiler forces a check.

## Writing the check by hand

For small shapes, type predicates are enough (phase 1):

```ts
function isUser(value: unknown): value is User {
  return (
    typeof value === "object" && value !== null &&
    "id" in value && typeof value.id === "number" &&
    "name" in value && typeof value.name === "string"
  );
}
```

It gets long fast, the error messages are poor ("not a user"), and the type and the check can
drift apart because they are written twice. A **schema library** solves all three.

## zod

zod (version 4) describes the shape once, as a runtime value, and **infers** the TypeScript
type from it:

```ts
import { z } from "zod";

const User = z.object({
  id: z.number().int().positive(),
  name: z.string().min(1),
  email: z.email(),
  role: z.enum(["admin", "member"]).default("member"),
  tags: z.array(z.string()).max(10).optional(),
});

type User = z.infer<typeof User>;        // the type after parsing
type UserInput = z.input<typeof User>;   // what callers may send (role optional)
```

A value and a type can share the name `User`; they live in different namespaces.

### parse and safeParse

```ts
const user = User.parse(data);             // returns a User or throws a ZodError

const result = User.safeParse(data);       // never throws
if (!result.success) {
  console.log(result.error.issues);        // [{ path: ["email"], message: "Invalid email address", ... }]
  console.log(z.prettifyError(result.error));
} else {
  result.data.name;                        // User
}
```

`safeParse` returns a discriminated union on `success`, so the compiler forces you to check it.
Each issue has a `path` (`["address", "zip"]`, `["tags", 3]`) you can show next to a form field
or in an API error response.

### What a schema can do

| Need | zod |
| --- | --- |
| Defaults for missing values | `.default(10)`, applied when the value is `undefined` |
| Optional / nullable | `.optional()`, `.nullable()` |
| Unknown keys | `z.object` strips them, `z.strictObject` rejects them, `z.looseObject` keeps them |
| Unions | `z.union([...])`, `z.discriminatedUnion("kind", [...])` |
| Strings from the environment | `z.coerce.number()` (careful: `""` becomes `0`) or an explicit transform |
| Custom rules | `.refine((v) => v.start < v.end, { message: "start must be before end" })` |
| Changing the value | `.transform((s) => s.trim())` (the input and output types then differ) |
| Distinct ids | `.brand<"UserId">()` |
| JSON Schema for OpenAPI | `z.toJSONSchema(User)` |

### Branded types from schemas

```ts
const UserId = z.string().regex(/^usr_[a-z0-9]{8}$/).brand<"UserId">();
type UserId = z.infer<typeof UserId>;     // string & z.$brand<"UserId">

function loadUser(id: UserId) {}
loadUser("usr_12345678");                 // error: a plain string is not a UserId
loadUser(UserId.parse(input));            // OK: only a successful parse creates one
```

The brand turns "this string was validated" into a fact the compiler tracks.

## Where to validate

| Boundary | Validate |
| --- | --- |
| HTTP handlers | The body, query and path parameters, before your logic runs |
| Configuration | `process.env` and config files, **once at start-up**: fail fast with every problem listed |
| Outgoing calls | Responses of third-party APIs you do not control |
| Storage | Data read back from caches, `localStorage`, queues written by older versions |

Inside the boundary, pass the typed values along and do not validate again. Validating the
same data at every layer is noise; not validating at the boundary is a bug.

## Alternatives

zod is the most used option; valibot is smaller (tree-shakable functions), and ArkType writes
schemas with a TypeScript-like syntax. All three implement **Standard Schema**, a shared
interface, so frameworks such as tRPC, Hono or form libraries accept any of them. The ideas in
this lesson do not depend on the library.

## Summary

- Types protect nothing at runtime; data that enters the program is `unknown` until checked.
- Parse at the boundary into typed values, then trust the types inside.
- zod: one schema, inferred types (`z.infer`, `z.input`), `safeParse` with paths for errors,
  defaults, transforms, refinements and brands.
- Validate configuration once at start-up, listing every problem.

## Further reading

- zod documentation: <https://zod.dev/>
- zod, "Basic usage" (parse, safeParse, inferring types): <https://zod.dev/basics>
- Standard Schema: <https://standardschema.dev/>
- TypeScript Handbook, "Narrowing", using type predicates: <https://www.typescriptlang.org/docs/handbook/2/narrowing.html#using-type-predicates>
- Alexis King, "Parse, don't validate": <https://lexi-lambda.github.io/blog/2019/11/05/parse-don-t-validate/>
- MDN, `JSON.parse()`: <https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/JSON/parse>
- roadmap.sh, TypeScript topic "Useful Packages": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/typescript/content/useful-packages@PCX3KcvMUW3mmQEepLTXp.md>
