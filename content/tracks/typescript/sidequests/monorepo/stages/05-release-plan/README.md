When core changes, the CLI and the API must be released too, or they keep pointing at the
old core. Doing that by hand in the right order is how releases go wrong. Create
`packages/release/`, the package `@tally/release` (a project of the solution like the others;
it needs no other workspace package), that plans and applies version bumps.

It exports:

- `type Bump = "major" | "minor" | "patch"` and `bumpVersion(version: string, bump: Bump): string`
  for plain `major.minor.patch` versions (numbers without leading zeros): `1.2.3` becomes `2.0.0`, `1.3.0` or `1.2.4`.
  Anything else (`1.2`, `v1.2.3`, `1.2.3-beta`) throws an `Error` with message
  `invalid version: <version>`.
- `type WorkspacePackage = { name: string; version: string; dir: string; dependencies: Record<string, string> }`
  and `readWorkspace(root: string): WorkspacePackage[]`: the packages the root
  `package.json`'s `workspaces` name (folders, or folders ending in `/*`), sorted by name,
  `dir` relative to `root` with `/` separators (`packages/core`) and `dependencies` `{}` when
  the field is missing.
- `type Change = { package: string; bump: Bump }`,
  `type PlannedRelease = { name: string; from: string; to: string; reason: "changed" | "dependency" }`
  and `planRelease(packages: readonly WorkspacePackage[], changes: readonly Change[]): PlannedRelease[]`:
  - each changed package gets the **highest** bump of its changes (`major` > `minor` > `patch`),
    reason `changed`;
  - every package that depends on a released package, directly or through others, and is
    not changed itself, gets a `patch`, reason `dependency`;
  - the plan lists dependencies before their dependents, and packages that can go in either
    order by name; no changes is an empty plan;
  - a change of a package that is not in the workspace throws `unknown package <name>`; a
    dependency cycle between workspace packages throws
    `dependency cycle: <every package in a cycle, sorted, joined by ", ">`.
- `applyRelease(root: string, plan: readonly PlannedRelease[]): void`: writes the new
  `version` of each planned package and, in every workspace package, sets the range of each
  dependency on a planned package to `^<new version>`. Every other field stays as it was,
  and a changed file is written as `JSON.stringify(json, null, 2)` plus a newline.
  Files with nothing to change are not touched.
- `tagsFor(plan): string[]`: the git tags of the release, `<name>@<version>` in plan order
  (`@tally/core@1.1.0`), the way monorepos tag each package.
