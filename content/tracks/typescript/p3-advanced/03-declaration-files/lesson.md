# Declaration Files, Ambient Types and Augmentation

Most code you call was not written in your project: the JavaScript standard library, Node.js,
npm packages, browser APIs. TypeScript knows their types from **declaration files** (`.d.ts`):
files that contain only types, no implementation. This lesson shows how to read them, where
they come from, how to write one, and how to extend types you do not own.

## What a .d.ts file is

```ts
// greet.d.ts: describes greet.js
export interface GreetOptions {
  shout?: boolean;
}
export declare function greet(name: string, options?: GreetOptions): string;
export declare const VERSION: string;
```

`declare` says "this exists at runtime, here is its type". A `.d.ts` file may contain only
declarations: no function bodies, no initialisers. The compiler never emits JavaScript for it.

## Where types come from

| Source | Example | How |
| --- | --- | --- |
| The `lib` option | `Array`, `Promise`, `Map` (`lib.es2024.d.ts` and the others), the DOM | Chosen by `target` and `lib` in tsconfig |
| A package that ships its types | `zod`, `vitest`, most modern packages | `"types"` / `"exports"` conditions in its package.json |
| DefinitelyTyped | `@types/node`, `@types/express` | A separate package `@types/<name>`, maintained by the community |
| Your own `.d.ts` | An untyped internal script, a global set by a `<script>` tag | A file in your project |

In TypeScript 7 the `types` option defaults to `[]`: global type packages such as `@types/node`
are only included when listed (`"types": ["node"]`). Types of imported packages are still
found through the import.

`skipLibCheck: true` tells `tsc` not to re-check declaration files of dependencies. It is the
common setting: it saves time and protects you from errors inside `node_modules` you cannot fix.

## Reading declaration files

"Go to definition" on a library function opens its `.d.ts`. Learning to read them is a skill:

```ts
// from @types/node (simplified)
export function readFile(
  path: PathLike | FileHandle,
  options?: ({ encoding?: null; flag?: OpenMode } & Abortable) | null,
): Promise<Buffer>;
export function readFile(
  path: PathLike | FileHandle,
  options: ({ encoding: BufferEncoding; flag?: OpenMode } & Abortable) | BufferEncoding,
): Promise<string>;
```

Two overloads: without an encoding you get a `Buffer`, with one a `string`. Library typings use
every feature from phase 2: overloads, generics, conditional and mapped types.

## Writing declarations for untyped code

When a package has no types and no `@types` package, the quickest fix is a **shorthand ambient
module**, which types everything as `any`:

```ts
// types/legacy-lib.d.ts
declare module "legacy-lib";
```

Better, describe the part you use:

```ts
declare module "legacy-lib" {
  export function slugify(text: string, options?: { lower?: boolean }): string;
}
```

Wildcard modules type non-code imports that a bundler handles:

```ts
declare module "*.svg" {
  const url: string;
  export default url;
}
```

## Global declarations

A file **without** top-level `import` or `export` is a script: its declarations are global.
To declare globals from inside a module, use `declare global`:

```ts
// globals.d.ts
export {};   // makes this file a module

declare global {
  var __BUILD_ID__: string;            // a global variable set by the build
  interface Window {
    analytics?: { track(event: string): void };
  }
}
```

`interface Window` here **merges** with the DOM's declaration: declaration merging (phase 1)
is what makes augmentation possible.

## Module augmentation

Libraries often expect you to add properties to their types. Augment the module where the type
is declared:

```ts
// express-augment.d.ts
import "express";

declare module "express-serve-static-core" {
  interface Request {
    user?: { id: string; roles: string[] };
  }
}
```

The same pattern adds fields to a test framework's matchers, a router's metadata or a config
object. Only interfaces (and namespaces) merge, which is one reason libraries expose extension
points as interfaces.

## Generating declarations for your library

When you publish a package, ship its types:

```jsonc
{
  "compilerOptions": {
    "declaration": true,        // emit .d.ts next to the .js
    "declarationMap": true,     // "go to definition" jumps to your .ts source
    "isolatedDeclarations": true // each file's declarations can be produced without type inference across files
  }
}
```

`isolatedDeclarations` (TypeScript 5.5) requires explicit types on exports, so tools other than
`tsc` (and `tsc` itself, in parallel) can produce `.d.ts` files one file at a time, which speeds
up large builds. In `package.json`, point the `"types"` condition of each `"exports"` entry at
its declaration file, listed **first**.

## Triple-slash directives

`/// <reference types="node" />` at the top of a file includes a type package for that file.
You will meet it in older declaration files; in your own code, prefer `types` in tsconfig and
imports.

## Summary

- `.d.ts` files describe JavaScript with types only; `declare` means "exists at runtime".
- Types come from `lib`, from packages that ship them, from `@types/*` and from your own files;
  TypeScript 7 includes global `@types` only when listed in `types`.
- `declare module "x"` types an untyped package or a wildcard import; `declare global` and
  interface merging add globals; module augmentation extends a library's interfaces.
- Publish `.d.ts` with `declaration`, `declarationMap`, and consider `isolatedDeclarations`.

## Further reading

- TypeScript Handbook, "Declaration Files: Introduction": <https://www.typescriptlang.org/docs/handbook/declaration-files/introduction.html>
- TypeScript Handbook, "Declaration Merging" (module augmentation and global augmentation): <https://www.typescriptlang.org/docs/handbook/declaration-merging.html>
- TypeScript 5.5 release notes, isolated declarations: <https://www.typescriptlang.org/docs/handbook/release-notes/typescript-5-5.html>
- TSConfig reference, `types`: <https://www.typescriptlang.org/tsconfig/#types>
- DefinitelyTyped: <https://github.com/DefinitelyTyped/DefinitelyTyped>
- roadmap.sh, TypeScript topic "Ambient Modules": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/typescript/content/ambient-modules@k_5y77k8ZZ9_O2WpWXWTY.md>
