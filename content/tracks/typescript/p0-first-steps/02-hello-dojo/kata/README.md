# Hello, Dojo

Write three functions in `greet.ts`.

| Function | Does |
| --- | --- |
| `greet(name?, options?)` | Returns `"Welcome to the dojo, <name>!"`. The name is trimmed; when it is missing or blank, use `stranger`. With `{ shout: true }` the whole message is in upper case |
| `parseArgs(argv)` | Returns `{ name, shout }`: `shout` is true when `"--shout"` appears anywhere, and the other words joined with one space form the name |
| `main(argv, log?)` | Prints the greeting for `argv` with `log` (default `console.log`) and returns the exit code `0` |

```ts
greet("  Ada ");                    // "Welcome to the dojo, Ada!"
greet("", { shout: true });         // "WELCOME TO THE DOJO, STRANGER!"
parseArgs(["--shout", "Ada", "L"]); // { name: "Ada L", shout: true }
```

## How TypeScript katas are checked

1. `tsc` type-checks every `.ts` file with strict settings. A type error means the code
   "does not compile", and no test runs.
2. The tests in `tests/` run with `node:test`. Node.js runs `.ts` files directly: it strips
   the types, so there is no build step.

To run them on your machine (Node.js 26 or later): `npm install` once, then `npm test`. There
are 8 tests: 1 in `tests/visible.test.ts` and 7 hidden ones. The functions that still throw
`new Error("todo ...")` are yours to write.
