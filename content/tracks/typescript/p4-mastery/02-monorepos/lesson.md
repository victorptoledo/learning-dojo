# Monorepos, Workspaces and Project References

As a codebase grows, one `tsconfig.json` over everything stops working: checks get slow, any
file can import any other, and shared code is copied between services. A **monorepo** keeps
several packages in one repository: apps (an API, a worker, a web front end) and libraries
(domain types, a validation package, a UI kit). Two separate mechanisms make it work:
**workspaces** (the package manager links packages together) and **project references**
(TypeScript builds and checks packages as separate units).

## Workspaces

```text
repo/
├── package.json            { "private": true, "workspaces": ["packages/*", "apps/*"] }
├── tsconfig.base.json      shared compiler options
├── tsconfig.json           the solution: references every project
├── packages/
│   ├── domain/             @acme/domain: types and pure logic
│   └── validation/         @acme/validation: zod schemas, depends on domain
└── apps/
    └── api/                @acme/api, depends on both
```

With npm, pnpm or Yarn workspaces, `npm install` links `@acme/domain` into `node_modules` as a
symlink to `packages/domain`. Inside `apps/api`, you import it like any package:

```ts
import { type Order, total } from "@acme/domain";
```

Each package declares what it exposes with `exports` in its `package.json`. Deep imports such
as `@acme/domain/src/internal/money` are impossible unless listed, which keeps boundaries
honest. pnpm is popular for monorepos because its strict `node_modules` layout refuses
undeclared dependencies.

### Internal packages without a build

The simplest setup points `exports` straight at the TypeScript source:

```json
{
  "name": "@acme/domain",
  "type": "module",
  "exports": { ".": "./src/index.ts" }
}
```

Bundlers and `tsc` (with `moduleResolution: "bundler"` or a `"development"` condition) can
consume it directly. Node.js type stripping cannot: it refuses `.ts` files under
`node_modules`, and a workspace symlink resolves there. For packages Node runs, either build
them (next section) or keep the shared code as a folder imported with relative paths or `#`
imports inside one package.

## Project references

Project references split the TypeScript program into **projects** that are checked and built
separately, each with its own `tsconfig.json`:

```jsonc
// packages/domain/tsconfig.json
{
  "extends": "../../tsconfig.base.json",
  "compilerOptions": {
    "composite": true,          // required for a referenced project: emits .d.ts and .tsbuildinfo
    "rootDir": "src",
    "outDir": "dist"
  },
  "include": ["src"]
}

// apps/api/tsconfig.json
{
  "extends": "../../tsconfig.base.json",
  "compilerOptions": { "composite": true, "rootDir": "src", "outDir": "dist" },
  "references": [{ "path": "../../packages/domain" }, { "path": "../../packages/validation" }],
  "include": ["src"]
}

// tsconfig.json at the root: a "solution" with no files of its own
{
  "files": [],
  "references": [{ "path": "packages/domain" }, { "path": "packages/validation" }, { "path": "apps/api" }]
}
```

`tsc -b` (build mode) then:

1. Orders the projects by their references (domain, validation, api).
2. Rebuilds only projects whose inputs changed, using each `.tsbuildinfo` file.
3. Type-checks a dependent project against the **declaration files** of its references, not
   their sources, which is much faster and enforces that only exported types are used.

TypeScript 7 builds independent projects in parallel; `--builders` sets how many at once.
Editors understand references too: "go to definition" jumps to the source thanks to
`declarationMap`.

References also forbid cycles: if `domain` tried to import from `api`, the build fails. That is
a feature: dependency direction becomes something the compiler enforces.

## Sharing configuration

`tsconfig.base.json` holds the options every project shares (`strict`, `target`, `module`,
`verbatimModuleSyntax`, `isolatedDeclarations`). Projects `extends` it and add only their own
paths. `isolatedDeclarations` pays off here: every package's `.d.ts` can be produced without
checking its dependencies first.

## Task runners

Workspaces link packages; project references build TypeScript. Running *other* tasks (tests,
lint, bundling) across packages in dependency order, with caching, is the job of a task runner:
`npm run test --workspaces`, or Turborepo and Nx for caching and affected-only runs in CI
("test only packages changed by this pull request, and their dependents").

## When a monorepo is a bad idea

A monorepo helps when packages change together and share types. It hurts when teams release
independently with different cadences and toolchains, or when the tooling (CI caching, code
owners) is not invested in. Start with one package and folders with clear boundaries; split into
packages when a boundary needs enforcing or when a library must be published.

## Summary

- Workspaces link local packages; `exports` defines each package's public surface.
- Internal packages can point at `.ts` sources for bundlers; packages Node runs need building.
- Project references (`composite`, `references`, `tsc -b`) check and build projects separately,
  incrementally, in order and in parallel, against declaration files, and forbid cycles.
- Share options with `extends`; use a task runner for tests and lint across packages.

## Further reading

- TypeScript Handbook, "Project References": <https://www.typescriptlang.org/docs/handbook/project-references.html>
- TSConfig reference, `composite`: <https://www.typescriptlang.org/tsconfig/#composite>
- npm Docs, "workspaces": <https://docs.npmjs.com/cli/v11/using-npm/workspaces>
- pnpm, "Workspace": <https://pnpm.io/workspaces>
- Node.js, "Modules: Packages", conditional exports: <https://nodejs.org/api/packages.html#conditional-exports>
- roadmap.sh, TypeScript topic "tsconfig.json": <https://github.com/nilbuild/developer-roadmap/blob/master/roadmaps/typescript/content/tsconfigjson@DmqrX56d9KnBcOSwlJR2q.md>
