# Sidequest: Monorepo with a shared package

Tally tracks expenses. Its money rules (cents, currencies, parsing, formatting) are needed by
a CLI and by an HTTP API, and copying them twice is how two parts of a product start to
disagree about what `12.5 BRL` means. Build Tally as an **npm workspaces monorepo**: one
`@tally/core` package with the rules, consumers that use it only through its public
exports, project references so `tsc -b` builds everything in order, and a release planner
that knows which packages must move when one changes.

```text
tally/
├── package.json            "workspaces": ["packages/*"]
├── tsconfig.json           the solution: references to every package
└── packages/
    ├── core/               @tally/core     money rules
    ├── cli/                @tally/cli      sums a ledger file
    ├── api/                @tally/api      an HTTP handler
    └── release/            @tally/release  plans version bumps
```

The contract tests import each package **by its name** (`import { parseMoney } from
"@tally/core"`), read the `package.json` and `tsconfig.json` files, and run `tsc -b`. The
dojo's checker links every workspace package into `node_modules` by name, as `npm install`
does, so nothing is downloaded: there are no dependencies besides the packages themselves.
Each package's `exports` field points straight at its `src/index.ts` (Node.js runs
TypeScript directly), and imports between files of one package use `.ts` extensions.

The names, files and exports each stage asks for are fixed; everything inside the packages
is yours. Run `npm install` once to link the workspaces locally, then `npm test`.
