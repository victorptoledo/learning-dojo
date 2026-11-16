# ES Modules, package.json and npm

A Node.js program is a set of **modules**: files that export some values and import others.
Node.js has two module systems, and every Node.js developer meets both. On top of them,
**npm** installs the packages of the largest software registry in the world, described by a
`package.json`.

## ES modules (ESM)

ES modules are the JavaScript standard, the same as in the browser:

```ts
// math.ts
export function add(a: number, b: number): number {
  return a + b;
}
export const PI = 3.14159;
export default function square(x: number) {
  return x * x;
}
```

```ts
// main.ts
import square, { add, PI } from "./math.ts";    // default + named
import * as math from "./math.ts";               // namespace
import { readFile } from "node:fs/promises";      // a built-in module
import express from "express";                    // a package from node_modules

const config = JSON.parse(await readFile("config.json", "utf8"));  // top-level await
```

- Imports are **static** and hoisted: they are resolved before any code runs. For a module
  chosen at runtime, use `await import(path)`.
- Relative imports name the **file with its extension**: `./math.ts`, `./util.js`. Node.js does
  not guess extensions or `index` files for ES modules.
- Built-in modules use the **`node:` prefix** (`node:fs`, `node:path`). It cannot be confused
  with an npm package of the same name.
- `import.meta.dirname` and `import.meta.filename` replace CommonJS's `__dirname` and
  `__filename`.

## CommonJS (CJS)

CommonJS is Node.js's original module system, still used by many packages and older code:

```js
// math.cjs
const { readFileSync } = require("node:fs");
function add(a, b) { return a + b; }
module.exports = { add };

// main.cjs
const { add } = require("./math.cjs");
```

`require` is a **function call**: it runs at runtime, synchronously, and caches the module.

## Which one is a file?

| File | System |
| --- | --- |
| `.mjs`, `.mts` | Always ES module |
| `.cjs`, `.cts` | Always CommonJS |
| `.js`, `.ts` | ES module if the nearest `package.json` has `"type": "module"`, CommonJS otherwise |

New projects should use ES modules: set `"type": "module"`. Interop works both ways with
limits:

- ESM can `import` a CommonJS module (its `module.exports` becomes the default export).
- CommonJS can `require()` an ES module since Node.js 22.12 and 20.19, **if** that module has no
  top-level `await`. Otherwise it needs `await import()`.

## package.json

```json
{
  "name": "orders-service",
  "version": "1.4.0",
  "private": true,
  "type": "module",
  "engines": { "node": ">=26" },
  "scripts": {
    "start": "node src/main.ts",
    "dev": "node --watch src/main.ts",
    "test": "node --test",
    "typecheck": "tsc --noEmit"
  },
  "dependencies": { "fastify": "^5.6.0" },
  "devDependencies": { "typescript": "^7.0.2", "@types/node": "^26.6.6" },
  "exports": { ".": "./src/index.ts" }
}
```

| Field | Meaning |
| --- | --- |
| `type` | How `.js`/`.ts` files are loaded (see above) |
| `scripts` | Commands for `npm run <name>` (or `node --run <name>`); `npm test` and `npm start` are shortcuts. `node_modules/.bin` is on the `PATH` inside them |
| `dependencies` | Needed at runtime: installed everywhere, including production |
| `devDependencies` | Needed only to build and test (compilers, test tools, linters) |
| `exports` | A library's public entry points: what `import "pkg"` and `import "pkg/sub"` resolve to. Anything not listed cannot be imported |
| `engines` | The Node.js versions the package supports |
| `private` | `true` stops an accidental `npm publish` |

## Versions and ranges

npm packages use **semantic versioning**: `MAJOR.MINOR.PATCH`. A major release may break you,
a minor adds features, a patch fixes bugs. Dependencies are declared with **ranges**:

| Range | Accepts |
| --- | --- |
| `^5.6.0` (the default of `npm install`) | `>=5.6.0 <6.0.0` |
| `~5.6.0` | `>=5.6.0 <5.7.0` |
| `5.6.0` | exactly that version |
| `*`, `latest` | anything: never in a real project |

Ranges make installs **non-reproducible**: today `^5.6.0` gives 5.6.0, next month 5.9.2. That
is what the **lockfile** (`package-lock.json`) fixes: it records the exact version, source and
integrity hash of every package in the tree.

- **Commit the lockfile** for applications.
- `npm install` resolves ranges and may update the lockfile; use it when you add or upgrade
  packages.
- **`npm ci`** installs exactly what the lockfile says, deletes `node_modules` first, and fails
  if the lockfile and `package.json` disagree. Use it in CI and in Docker images, with
  `--omit=dev` for production.

```bash
npm install fastify            # add a dependency (updates package.json and the lockfile)
npm install -D typescript      # a devDependency
npm outdated                   # what has newer versions
npm ci --omit=dev              # reproducible production install
npx tsc --version              # run a package's binary
```

pnpm and Yarn are alternative package managers with the same registry; pnpm saves disk space
and is strict about undeclared dependencies.

## How a bare import is found

`import express from "express"` looks for `node_modules/express` in the current folder, then
in each parent folder up to the root. Inside it, the package's `exports` field (or `main`)
says which file to load. That is why a dependency you use must be declared: an undeclared
package that happens to be in `node_modules` (a "phantom dependency") disappears when the
tree changes.

## Summary

- ES modules (`import`/`export`) are the standard; set `"type": "module"`. CommonJS
  (`require`) is everywhere in older code, and the two interoperate with limits.
- Name relative imports with their extension and built-ins with `node:`.
- `package.json` declares scripts, runtime and development dependencies, exports and engines.
- Ranges plus a committed lockfile plus `npm ci` give reproducible installs.

## Further reading

- Node.js API, "Modules: ECMAScript modules": <https://nodejs.org/api/esm.html>
- Node.js API, "Modules: Packages" (type, exports): <https://nodejs.org/api/packages.html>
- Node.js API, "Modules: CommonJS modules": <https://nodejs.org/api/modules.html>
- npm Docs, "package.json": <https://docs.npmjs.com/cli/v11/configuring-npm/package-json>
- npm Docs, "npm ci": <https://docs.npmjs.com/cli/v11/commands/npm-ci>
- Semantic Versioning 2.0.0: <https://semver.org/>
- npm semver calculator: <https://semver.npmjs.com/>
