import assert from "node:assert/strict";
import { mkdirSync, mkdtempSync, readFileSync, rmSync, statSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { after, describe, test } from "node:test";
import { applyRelease, bumpVersion, planRelease, readWorkspace, tagsFor, type WorkspacePackage } from "@tally/release";
import { boundaryViolations, field, readJson, ROOT } from "./repo.ts";

const temporary: string[] = [];
after(() => {
  for (const dir of temporary) {
    rmSync(dir, { recursive: true, force: true });
  }
});

/** A workspace on disk: root package.json with "workspaces" and one folder per package. */
function workspace(workspaces: string[], packages: Record<string, object>): string {
  const root = mkdtempSync(path.join(tmpdir(), "tally-"));
  temporary.push(root);
  writeFileSync(path.join(root, "package.json"), JSON.stringify({ name: "root", private: true, workspaces }));
  for (const [dir, json] of Object.entries(packages)) {
    mkdirSync(path.join(root, dir), { recursive: true });
    writeFileSync(path.join(root, dir, "package.json"), JSON.stringify(json));
  }
  return root;
}

const pkg = (name: string, version: string, dependencies: Record<string, string> = {}): WorkspacePackage => ({
  name,
  version,
  dir: `packages/${name.replace("@t/", "")}`,
  dependencies,
});

// core <- cli, core <- api, api <- web; tools stands alone.
const GRAPH: WorkspacePackage[] = [
  pkg("@t/api", "2.0.0", { "@t/core": "^1.4.0", zod: "^4.0.0" }),
  pkg("@t/cli", "1.0.9", { "@t/core": "^1.4.0" }),
  pkg("@t/core", "1.4.2"),
  pkg("@t/tools", "0.3.0"),
  pkg("@t/web", "0.9.0", { "@t/api": "^2.0.0" }),
];

describe("the package", () => {
  test("@tally/release is a package of its own", () => {
    const release = readJson("packages", "release", "package.json");
    assert.equal(field(release, "name"), "@tally/release");
    assert.equal(field(release, "exports", "."), "./src/index.ts");
    assert.deepEqual(boundaryViolations("packages/release", []), []);
  });
});

describe("bumpVersion", () => {
  test("bumps major, minor and patch", () => {
    assert.equal(bumpVersion("1.2.3", "major"), "2.0.0");
    assert.equal(bumpVersion("1.2.3", "minor"), "1.3.0");
    assert.equal(bumpVersion("1.2.3", "patch"), "1.2.4");
    assert.equal(bumpVersion("0.9.19", "patch"), "0.9.20");
    assert.equal(bumpVersion("0.0.0", "minor"), "0.1.0");
  });

  test("refuses what is not major.minor.patch", () => {
    for (const version of ["1.2", "v1.2.3", "1.2.3-beta", "", "1.2.x", "01.2.3"]) {
      assert.throws(() => bumpVersion(version, "patch"), { message: `invalid version: ${version}` }, version);
    }
  });
});

describe("readWorkspace", () => {
  test("reads the repository itself", () => {
    const names = readWorkspace(ROOT).map((found) => `${found.name} ${found.dir}`);
    for (const expected of ["@tally/api packages/api", "@tally/cli packages/cli", "@tally/core packages/core", "@tally/release packages/release"]) {
      assert.ok(names.includes(expected), `${expected} not in ${names.join(", ")}`);
    }
  });

  test("follows folders and folder/* patterns, sorted by name", () => {
    const root = workspace(["libs/*", "tools/bump"], {
      "libs/b": { name: "b", version: "1.0.0", dependencies: { a: "^1.0.0" } },
      "libs/a": { name: "a", version: "1.0.0" },
      "tools/bump": { name: "0-bump", version: "0.1.0", private: true },
      "elsewhere/x": { name: "x", version: "9.9.9" },
    });
    mkdirSync(path.join(root, "libs", "empty"));
    assert.deepEqual(readWorkspace(root), [
      { name: "0-bump", version: "0.1.0", dir: "tools/bump", dependencies: {} },
      { name: "a", version: "1.0.0", dir: "libs/a", dependencies: {} },
      { name: "b", version: "1.0.0", dir: "libs/b", dependencies: { a: "^1.0.0" } },
    ]);
  });
});

describe("planRelease", () => {
  test("no changes, no plan", () => {
    assert.deepEqual(planRelease(GRAPH, []), []);
  });

  test("a package nobody depends on is released alone", () => {
    assert.deepEqual(planRelease(GRAPH, [{ package: "@t/tools", bump: "minor" }]), [
      { name: "@t/tools", from: "0.3.0", to: "0.4.0", reason: "changed" },
    ]);
  });

  test("dependents follow with a patch, dependencies first, ties by name", () => {
    assert.deepEqual(planRelease(GRAPH, [{ package: "@t/core", bump: "minor" }]), [
      { name: "@t/core", from: "1.4.2", to: "1.5.0", reason: "changed" },
      { name: "@t/api", from: "2.0.0", to: "2.0.1", reason: "dependency" },
      { name: "@t/cli", from: "1.0.9", to: "1.0.10", reason: "dependency" },
      { name: "@t/web", from: "0.9.0", to: "0.9.1", reason: "dependency" },
    ]);
  });

  test("the highest bump of a package wins, and a changed dependent keeps its own", () => {
    const plan = planRelease(GRAPH, [
      { package: "@t/core", bump: "patch" },
      { package: "@t/web", bump: "major" },
      { package: "@t/core", bump: "minor" },
      { package: "@t/core", bump: "patch" },
    ]);
    assert.deepEqual(plan, [
      { name: "@t/core", from: "1.4.2", to: "1.5.0", reason: "changed" },
      { name: "@t/api", from: "2.0.0", to: "2.0.1", reason: "dependency" },
      { name: "@t/cli", from: "1.0.9", to: "1.0.10", reason: "dependency" },
      { name: "@t/web", from: "0.9.0", to: "1.0.0", reason: "changed" },
    ]);
  });

  test("a change in the middle only moves what is above it", () => {
    assert.deepEqual(planRelease(GRAPH, [{ package: "@t/api", bump: "major" }]), [
      { name: "@t/api", from: "2.0.0", to: "3.0.0", reason: "changed" },
      { name: "@t/web", from: "0.9.0", to: "0.9.1", reason: "dependency" },
    ]);
  });

  test("the order follows the graph, not the names", () => {
    const chain = [pkg("@t/a", "1.0.0", { "@t/b": "^1.0.0" }), pkg("@t/b", "1.0.0", { "@t/c": "^1.0.0" }), pkg("@t/c", "1.0.0")];
    assert.deepEqual(planRelease(chain, [{ package: "@t/c", bump: "patch" }]).map((release) => release.name), ["@t/c", "@t/b", "@t/a"]);
  });

  test("an unknown package throws", () => {
    assert.throws(() => planRelease(GRAPH, [{ package: "@t/ghost", bump: "patch" }]), { message: "unknown package @t/ghost" });
  });

  test("a dependency cycle throws with every package in it", () => {
    const cyclic = [
      pkg("@t/a", "1.0.0", { "@t/b": "^1.0.0" }),
      pkg("@t/b", "1.0.0", { "@t/c": "^1.0.0" }),
      pkg("@t/c", "1.0.0", { "@t/a": "^1.0.0" }),
      pkg("@t/d", "1.0.0", { "@t/a": "^1.0.0" }),
    ];
    assert.throws(() => planRelease(cyclic, [{ package: "@t/d", bump: "patch" }]), { message: "dependency cycle: @t/a, @t/b, @t/c" });
  });
});

describe("tagsFor", () => {
  test("one tag per release, in plan order", () => {
    assert.deepEqual(tagsFor(planRelease(GRAPH, [{ package: "@t/api", bump: "minor" }])), ["@t/api@2.1.0", "@t/web@0.9.1"]);
  });
});

describe("applyRelease", () => {
  test("writes versions and ranges, keeps the rest, leaves the others alone", () => {
    const root = workspace(["packages/*"], {
      "packages/core": { name: "@t/core", version: "1.4.2", type: "module", exports: { ".": "./src/index.ts" } },
      "packages/cli": { name: "@t/cli", version: "1.0.9", dependencies: { "@t/core": "^1.4.0", chalk: "^5.0.0" }, bin: { t: "x" } },
      "packages/tools": { name: "@t/tools", version: "0.3.0" },
    });
    const toolsFile = path.join(root, "packages", "tools", "package.json");
    const before = statSync(toolsFile).mtimeMs;
    const toolsText = readFileSync(toolsFile, "utf8");
    const plan = planRelease(readWorkspace(root), [{ package: "@t/core", bump: "minor" }]);
    applyRelease(root, plan);
    const read = (dir: string) => readFileSync(path.join(root, "packages", dir, "package.json"), "utf8");
    assert.equal(read("core"), `${JSON.stringify({ name: "@t/core", version: "1.5.0", type: "module", exports: { ".": "./src/index.ts" } }, null, 2)}\n`);
    assert.equal(
      read("cli"),
      `${JSON.stringify({ name: "@t/cli", version: "1.0.10", dependencies: { "@t/core": "^1.5.0", chalk: "^5.0.0" }, bin: { t: "x" } }, null, 2)}\n`,
    );
    assert.equal(readFileSync(toolsFile, "utf8"), toolsText);
    assert.equal(statSync(toolsFile).mtimeMs, before);
  });

  test("an empty plan changes nothing", () => {
    const root = workspace(["packages/*"], { "packages/a": { name: "a", version: "1.0.0" } });
    const file = path.join(root, "packages", "a", "package.json");
    const text = readFileSync(file, "utf8");
    applyRelease(root, []);
    assert.equal(readFileSync(file, "utf8"), text);
  });
});
