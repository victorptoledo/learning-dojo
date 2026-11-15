# Enums vs Unions, as const, satisfies and ES Modules

This lesson is about two kinds of boundaries: between a set of **named constants** and the
types that describe them, and between **modules**. Both changed recently. Node.js now runs
TypeScript by erasing types, which favours unions over enums, and the long ESM versus CommonJS
transition is nearly over.

## Enums and why modern code avoids them

```ts
enum Level { Debug, Info, Warn }     // Debug = 0, Info = 1, ...
enum Color { Red = "red", Green = "green" }
```

An `enum` is one of the few TypeScript features that **generates JavaScript**: an object (and,
for numeric enums, a reverse mapping from numbers to names). Consequences:

- Node.js type stripping rejects it, and `erasableSyntaxOnly` reports it.
- Numeric enums are loosely typed: any `number` can be assigned to `Level` in older versions,
  and `Level[5]` is `undefined` with type `string`.
- String enums are nominal, unlike everything else: a function taking `Color` refuses the
  literal `"red"`, which surprises callers and complicates JSON.
- `const enum` is inlined at compile time and breaks with tools that compile one file at a time
  (`isolatedModules`), such as esbuild and Node.

### The replacement: unions and as const objects

```ts
type Level = "debug" | "info" | "warn";            // when only the type is needed

const LEVELS = ["debug", "info", "warn"] as const; // when you also need the list at runtime
type Level2 = (typeof LEVELS)[number];             // "debug" | "info" | "warn"

const Color = { Red: "red", Green: "green" } as const; // when you want named constants
type Color = (typeof Color)[keyof typeof Color];      // "red" | "green"
Color.Red;                                            // "red"
```

`as const` makes a literal **deeply readonly** and keeps every literal type: arrays become
readonly tuples and strings stay literals. A value and a type may share a name (`Color` above),
because values and types live in different namespaces.

## satisfies

Annotating a constant checks it, but **widens** it to the annotation:

```ts
type Route = { path: string; auth: boolean };

const routes: Record<string, Route> = {
  home: { path: "/", auth: false },
  admin: { path: "/admin", auth: true },
};
routes.admni;          // no error: every string key is allowed
```

`satisfies` (TypeScript 4.9) checks the value against a type **without changing its inferred
type**:

```ts
const routes = {
  home: { path: "/", auth: false },
  admin: { path: "/admin", auth: true },
} satisfies Record<string, Route>;

routes.admni;          // error: Property 'admni' does not exist
routes.admin.path;     // string
```

Combine both for configuration tables: `as const satisfies T` keeps the literal types *and*
checks the shape, including that every key of a union is present:

```ts
type Status = "pending" | "paid" | "shipped";
const NEXT = {
  pending: ["paid"],
  paid: ["shipped"],
  shipped: [],
} as const satisfies Record<Status, readonly Status[]>;
// forget a status, or misspell one, and this stops compiling
```

Rule of thumb: annotate (`: T`) when you want the variable to *be* a `T` (for example, it will
be reassigned or exported as that type); use `satisfies T` when you want the compiler to check
the value but keep its precise type.

## ES modules

Every file with a top-level `import` or `export` is a module with its own scope:

```ts
// money.ts
export type Cents = number;
export function format(cents: Cents): string { return (cents / 100).toFixed(2); }
export default class Wallet {}

// app.ts
import Wallet, { format, type Cents } from "./money.ts";
import * as money from "./money.ts";
```

With `verbatimModuleSyntax` (and with Node.js type stripping), a type-only import **must** say
so, either `import type { Cents }` or the inline `type` modifier, because the import line is
kept as written and Node must not try to load a value that does not exist. Prefer named exports
over default exports: they are renamed consistently by refactorings and cannot be imported
under a wrong name.

### Which module system?

A `.ts` file follows the same rule as a `.js` file: **ESM when the nearest `package.json` has
`"type": "module"`**, CommonJS otherwise. `.mts` is always ESM and `.cts` always CommonJS.
`"module": "nodenext"` tells `tsc` to apply exactly Node's rules, including:

- Relative imports need the **file extension** (`./money.ts` when Node runs the source,
  `./money.js` when you compile to JavaScript, or `.ts` plus `rewriteRelativeImportExtensions`).
- Directory imports (`./utils` meaning `./utils/index.js`) do not work in ESM.

For code that a bundler (Vite, esbuild, webpack) processes, `"module": "preserve"` with
`"moduleResolution": "bundler"` models the bundler's looser rules instead.

### CommonJS interop

Plenty of npm packages are still CommonJS. From ESM you can import them:

```ts
import express from "express";      // the CommonJS module.exports object, as the default import
import { readFile } from "node:fs/promises";
```

`esModuleInterop` is always on in TypeScript 7, which makes default imports of CommonJS modules
match Node's behaviour. The other direction became easy too: since Node.js 22.12, `require()`
can load an ES module as long as it has no top-level `await`, and TypeScript 5.8 models it under
`nodenext`. New packages should simply publish ESM.

### package.json exports

A package declares its public entry points and types with `exports`; anything not listed cannot
be imported:

```json
{
  "name": "@acme/money",
  "type": "module",
  "exports": {
    ".": { "types": "./dist/index.d.ts", "default": "./dist/index.js" },
    "./format": { "types": "./dist/format.d.ts", "default": "./dist/format.js" }
  },
  "imports": { "#internal/*": "./src/internal/*.ts" }
}
```

`imports` with a `#` prefix gives a package private aliases that both Node and TypeScript
understand, the standard replacement for `paths` aliases.

### Namespaces

`namespace` predates ES modules. A namespace with values generates code (not erasable); modules
replace it completely. You will still meet `declare namespace` in declaration files, where it
only describes types (phase 3).

## Summary

- Prefer union types and `as const` objects to `enum`: they are erasable, structural and
  JSON-friendly.
- `satisfies` checks a value without widening it; `as const satisfies T` is the pattern for
  typed constant tables.
- ESM with `"type": "module"`, explicit extensions, `import type` for types, named exports.
- `nodenext` for code Node runs, `bundler` for code a bundler processes; CommonJS interop works
  both ways now; describe packages with `exports` and `imports`.

## Further reading

- TypeScript Handbook, "Enums": <https://www.typescriptlang.org/docs/handbook/enums.html>
- TypeScript 4.9 release notes, the `satisfies` operator: <https://www.typescriptlang.org/docs/handbook/release-notes/typescript-4-9.html>
- TypeScript Handbook, "Modules - Theory": <https://www.typescriptlang.org/docs/handbook/modules/theory.html>
- TypeScript 5.8 release notes, `require()` of ECMAScript modules in `nodenext`: <https://www.typescriptlang.org/docs/handbook/release-notes/typescript-5-8.html>
- Node.js, "Modules: Packages" (`exports` and `imports`): <https://nodejs.org/api/packages.html>
- MDN, "JavaScript modules": <https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Modules>
- roadmap.sh, TypeScript topic "satisfies keyword": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/typescript/content/satisfies-keyword@HD1UGOidp7JGKdW6CEdQ_.md>
