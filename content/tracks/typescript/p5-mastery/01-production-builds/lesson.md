# TypeScript in Production: Builds, Bundlers and CI

In development, Node.js runs your `.ts` files and the editor checks them. Production asks
harder questions: what exactly gets deployed, how fast does the service start, how are errors
mapped back to source lines, and what stops a type error from reaching the main branch? This
lesson walks through the options and the CI that holds them together.

## Three ways to ship

| Strategy | How | Good for | Watch out for |
| --- | --- | --- | --- |
| **Run the source** | Deploy `.ts` files; `node src/main.ts` strips types at start-up | Services and scripts on Node 26 | Only erasable syntax; dependencies cannot be `.ts` (Node refuses TypeScript under `node_modules`) |
| **Compile with tsc** | `tsc` emits `.js` (and `.d.ts`) into `dist/` | Libraries published to npm; any runtime | `rewriteRelativeImportExtensions` if the source imports `./x.ts` |
| **Bundle** | esbuild, tsdown (Rolldown) or Vite produce one or a few files | Front ends, serverless functions, CLIs | Bundlers only strip types: type-check separately |

The important point is shared by all three: **only `tsc` checks types**. Node, esbuild, SWC,
Vite and Bun all strip them. So the build pipeline always has two independent steps:
type-check, then run or produce JavaScript.

### Compiling with tsc

```jsonc
// tsconfig.build.json
{
  "extends": "./tsconfig.json",
  "compilerOptions": {
    "noEmit": false,
    "rootDir": "src",
    "outDir": "dist",
    "declaration": true,
    "sourceMap": true,
    "rewriteRelativeImportExtensions": true   // ./money.ts becomes ./money.js in the output
  },
  "include": ["src"]
}
```

In TypeScript 7 `rootDir` defaults to the folder of `tsconfig.json`, so set it when the sources
live in `src/`, or the output gets an extra `dist/src/` level. Use `"target"` matching the
oldest runtime you support (`es2024` for Node 24 and later), and source maps so stack traces
point at `.ts` lines (`node --enable-source-maps dist/main.js`).

### Running the source

On Node 26 this is now a real option for services: no build step, no `dist/` folder, stack
traces already show the right lines (stripping replaces types with spaces, so positions do not
move). The rules from phase 0 apply: `erasableSyntaxOnly`, `verbatimModuleSyntax`, `.ts`
extensions, `import type`, and no `paths` aliases (use `#` imports in `package.json`).

## The TypeScript 7 toolchain in practice

- `tsc` is the native compiler: type-checking that took a minute now takes seconds. It checks
  files in parallel; `--checkers 8` raises the number of type-checking workers (the default is
  4), and `--singleThreaded` turns parallelism off for debugging or small CI machines.
- 7.0 has **no programmatic API**. Tools that import `typescript` as a library (typescript-eslint,
  some framework plugins, `ts-morph`) still need TypeScript 6.0. Install it alongside with the
  compatibility package and an npm alias:

```json
{
  "devDependencies": {
    "typescript": "npm:@typescript/typescript6@^6.0.2",
    "@typescript/native": "npm:typescript@^7.0.2"
  }
}
```

  Tools then load 6.0 through `typescript`, while `npx tsc` runs 7.0 (and `tsc6` runs 6.0).
  Expect this to go away once 7.1 ships its new API.
- Upgrading from 5.x: first move to 6.0 and fix its deprecation warnings (`baseUrl`,
  `moduleResolution: "node"`, `target: "es5"`, AMD/UMD), set `types` explicitly, then switch to
  7.0. Settings removed in 7.0 are hard errors, not warnings.

## Type-checking in CI

```yaml
# .github/workflows/check.yml (the steps)
- uses: actions/setup-node@v7
  with: { node-version: 26, cache: npm }
- run: npm ci
- run: npx tsc --noEmit        # or `tsc -b` for project references
- run: npx eslint .
- run: node --test
- run: npm audit --omit=dev    # or a dependency scanner
```

- `npm ci` installs exactly the lock file; commit `package-lock.json`.
- Pin the TypeScript version in `devDependencies` (`"typescript": "7.0.2"` or `~7.0.2`): a minor
  release can add new errors, and it should do so in a pull request, not on Monday morning.
- Use `incremental` (a `.tsbuildinfo` file) or `tsc -b` to make repeated local checks faster;
  in CI, cache the build info if checks are slow.

## Runtime concerns that types do not solve

- **Validate configuration** at start-up (phase 3) and crash with a clear message when it is
  wrong.
- **Handle signals**: close servers and pools on `SIGTERM` before exiting.
- **Unhandled rejections** crash the process by default on Node; keep it that way, log them,
  and let the orchestrator restart the service.
- **Dependencies** are JavaScript: their `.d.ts` files can be wrong. Pin, update regularly,
  and audit.
- **Structured logs** with an error's `cause` chain and a request id.

## Summary

- Only `tsc` checks types; every runtime and bundler only strips them. Type-check in a separate
  step.
- Run the source on Node 26 for services, compile with `tsc` for libraries, bundle for front
  ends and functions; emit source maps.
- TypeScript 7 is native and parallel; tools that need the compiler API use 6.0 through
  `@typescript/typescript6` for now.
- CI: `npm ci`, `tsc`, lint, tests, audit, with TypeScript pinned.

## Further reading

- Announcing TypeScript 7.0 (performance, flags, the 6.0 compatibility package): <https://devblogs.microsoft.com/typescript/announcing-typescript-7-0/>
- Announcing TypeScript 6.0 (deprecations before 7.0): <https://devblogs.microsoft.com/typescript/announcing-typescript-6-0/>
- TSConfig reference, `rewriteRelativeImportExtensions`: <https://www.typescriptlang.org/tsconfig/#rewriteRelativeImportExtensions>
- Node.js, "Modules: TypeScript": <https://nodejs.org/api/typescript.html>
- Node.js CLI, `--enable-source-maps`: <https://nodejs.org/api/cli.html#--enable-source-maps>
- roadmap.sh, TypeScript topic "Build Tools": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/typescript/content/build-tools@I5uwfej5XMwR2N2gpBILh.md>
