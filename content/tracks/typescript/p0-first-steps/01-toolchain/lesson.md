# The Toolchain: TypeScript 7, Node.js and tsconfig

TypeScript is JavaScript with a **static type system** on top. You write `.ts` files with type
annotations; a checker reads them before the program runs and reports mistakes such as calling
a method that does not exist or passing a string where a number is expected. The types then
**disappear**: what runs is plain JavaScript. Two consequences shape everything in this track:

- Types cost nothing at runtime, and they also **protect nothing at runtime**. Data from
  outside (JSON, user input, environment variables) must still be checked by code.
- Every JavaScript program is almost a TypeScript program. You can adopt types gradually.

## The versions in this track

| Tool | Version | Notes |
| --- | --- | --- |
| TypeScript | **7.0** | The compiler and language service were ported from TypeScript to **Go**: typical full builds are about 10 times faster. The npm package is still `typescript` and the command is still `tsc` |
| Node.js | **26 LTS** | Runs `.ts` files directly by **stripping the types** (stable since 25.2 and 24.12) |

TypeScript 6.0 (March 2026) was the last release written in TypeScript. It was a bridge: it
deprecated old options so that 7.0 (July 2026) could remove them. 7.0 ships **no programmatic
API** yet (one is planned for 7.1), so tools that import the compiler as a library, such as
`typescript-eslint` or Vue's tooling, still use 6.0 through the `@typescript/typescript6`
package. You will meet that again in the production lesson.

## Install and run

```bash
node --version                 # v26.x
npm init -y                    # creates package.json
npm pkg set type=module        # .ts and .js files are ES modules
npm install -D typescript @types/node
npx tsc --init                 # creates tsconfig.json
```

`@types/node` holds the type declarations of Node's APIs (`node:fs`, `process`, ...).

Write `hello.ts`:

```ts
function greet(name: string, times = 1): string {
  return `Hello, ${name}! `.repeat(times).trim();
}

console.log(greet("dojo", 2));
console.log(greet(42)); // error TS2345: Argument of type 'number' is not assignable to parameter of type 'string'.
```

There are two separate jobs, done by two separate tools:

| Job | Command | What it does |
| --- | --- | --- |
| **Type-check** | `npx tsc` (with `noEmit`) | Reads every file in the project and reports type errors. Writes nothing |
| **Run** | `node hello.ts` | Replaces the type annotations with spaces and runs the JavaScript. It does **not** check types |

So `node hello.ts` happily runs the broken line above and prints `Hello, 42!`. Running and
checking are separate on purpose: the editor and `tsc` check, Node runs. In CI you run both.

### What type stripping cannot do

Node only **erases** types, it never rewrites code. TypeScript features that generate
JavaScript are rejected: `enum`, `namespace` with values, constructor **parameter
properties** (`constructor(private name: string)`), `import x = require()` and legacy
decorators. The option `erasableSyntaxOnly` makes `tsc` report them, so you find out before
running. Modern TypeScript code avoids them anyway: you will see the alternatives (unions,
`as const` objects, ES modules, explicit fields).

Imports need the real file extension (`import { greet } from "./greet.ts"`), and type-only
imports need the `type` keyword (`import type { User } from "./user.ts"`), because Node
deletes them and must know they hold no values.

## tsconfig.json

`tsconfig.json` marks the root of a project and holds the compiler options. TypeScript 7
changed several defaults: `strict` is now `true`, `module` defaults to `esnext`, `types`
defaults to `[]` (so you must list `"node"` to get Node's globals), and old options such as
`moduleResolution: "node10"`, `baseUrl`, `target: "es5"` and AMD/UMD modules are gone.

This is the configuration the dojo uses for every kata:

```jsonc
{
  "compilerOptions": {
    "target": "esnext",              // emit (or check) modern JavaScript
    "module": "nodenext",            // follow Node's ESM/CommonJS rules
    "lib": ["esnext"],
    "types": ["node"],               // include @types/node
    "strict": true,                  // the whole family of strict checks
    "noEmit": true,                  // only check; Node runs the .ts files
    "allowImportingTsExtensions": true,
    "erasableSyntaxOnly": true,      // reject syntax Node cannot strip
    "verbatimModuleSyntax": true,    // force `import type` for types
    "noUncheckedIndexedAccess": true,// arr[i] is T | undefined
    "noImplicitReturns": true,
    "noImplicitOverride": true,
    "noFallthroughCasesInSwitch": true,
    "noUnusedLocals": true,
    "skipLibCheck": true             // do not re-check .d.ts files of dependencies
  },
  "include": ["**/*.ts"]
}
```

### What strict turns on

`strict: true` enables a family of checks. The important ones:

| Option | Without it |
| --- | --- |
| `strictNullChecks` | `null` and `undefined` fit every type, so `user.name` may crash at runtime |
| `noImplicitAny` | Parameters without annotations silently become `any` |
| `strictFunctionTypes` | Function parameters are checked bivariantly (unsoundly) |
| `useUnknownInCatchVariables` | `catch (e)` gives `e: any` instead of `unknown` |
| `strictPropertyInitialization` | Class fields may stay `undefined` without saying so |

Never turn `strict` off in new code. When you migrate a JavaScript project, start with
`allowJs` and `checkJs`, then tighten one option at a time.

## The editor

The editor is where TypeScript pays off: completions, inline errors, go to definition and
rename all come from the **language service**. For TypeScript 7, install the "TypeScript 7"
extension (`typescriptteam.native-preview`) in VS Code; it replaces the built-in JavaScript and
TypeScript support with the native language server.

## How the dojo checks a kata

The `dojo-node` image runs `tsc` with the settings above, then the tests:

1. A type error in your code means the kata **does not compile**, and no test runs.
2. `tests/*.test.ts` run with `node:test`, Node's built-in test runner.
3. In some katas, `tests/*.types.ts` hold **type-level tests**: each `// test:` section passes
   when `tsc` finds no error in it.

## Summary

- TypeScript checks types before the program runs, then the types are erased.
- TypeScript 7.0 is the native (Go) compiler, still installed as `typescript` and run as `tsc`.
- Node.js 26 runs `.ts` files by stripping types, without checking them: run `tsc` too.
- Use `strict`, `erasableSyntaxOnly`, `verbatimModuleSyntax` and `.ts` import extensions.

## Further reading

- TypeScript Handbook, "The Basics": <https://www.typescriptlang.org/docs/handbook/2/basic-types.html>
- Announcing TypeScript 7.0: <https://devblogs.microsoft.com/typescript/announcing-typescript-7-0/>
- TSConfig reference: <https://www.typescriptlang.org/tsconfig/>
- Node.js, "Modules: TypeScript": <https://nodejs.org/api/typescript.html>
- roadmap.sh, TypeScript topic "Installation and Configuration": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/typescript/content/installation-and-configuration@dA8CNQOgJRDa0T7rmyS7f.md>
