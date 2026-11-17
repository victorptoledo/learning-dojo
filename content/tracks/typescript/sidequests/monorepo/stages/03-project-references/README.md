One `tsc` over the whole repository does not scale and does not know the order of the
packages. Give each package its own project and let `tsc -b` build them in dependency order.

- **Each package** (`packages/<name>/tsconfig.json`) extends `../../tsconfig.base.json` and
  sets `"composite": true`, `"rootDir": "src"`, `"outDir": "dist"` and
  `"emitDeclarationOnly": true` (Node.js runs the sources, so only the `.d.ts` files are
  built), with `"include": ["src"]`. Its `references` list exactly the workspace packages its
  `package.json` depends on: `@tally/cli` references `{ "path": "../core" }`, and core
  references nothing.
- **The root `tsconfig.json`** becomes a solution file: `"files": []` and a reference to
  every folder of `packages/`.
- `tsc -b`, run at the root, must succeed and write `dist/index.d.ts` in every package.
  Without the reference, a composite project that imports another package's sources fails
  with TS6307, which is how you know the graph is right.
- The root `package.json` scripts build that way: `"test"` runs `tsc -b` before the tests.

These rules hold for every package in `packages/`, including the ones the next stages add.
