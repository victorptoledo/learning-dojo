import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { existsSync, readdirSync, statSync } from "node:fs";
import path from "node:path";
import { describe, test } from "node:test";
import { at, field, isRecord, readJson } from "./repo.ts";

/** Every folder of packages/, by name. */
const packages = readdirSync(at("packages")).filter((name) => statSync(at("packages", name)).isDirectory()).sort();

const referencesOf = (config: unknown): string[] => {
  const references = field(config, "references");
  if (!Array.isArray(references)) {
    return [];
  }
  return references.map((reference) => String(field(reference, "path"))).map((reference) => path.posix.normalize(reference.replace(/\/$/, "")));
};

const workspaceDependencies = (name: string): string[] => {
  const dependencies = field(readJson("packages", name, "package.json"), "dependencies");
  return isRecord(dependencies) ? Object.keys(dependencies).filter((dependency) => dependency.startsWith("@tally/")) : [];
};

describe("the package projects", () => {
  test("there are packages", () => {
    assert.ok(packages.includes("core") && packages.includes("cli"), packages.join(", "));
  });

  for (const name of packages) {
    test(`packages/${name} is a composite project of its src/`, () => {
      const config = readJson("packages", name, "tsconfig.json");
      assert.equal(field(config, "extends"), "../../tsconfig.base.json");
      assert.equal(field(config, "compilerOptions", "composite"), true);
      assert.equal(field(config, "compilerOptions", "rootDir"), "src");
      assert.equal(field(config, "compilerOptions", "outDir"), "dist");
      assert.equal(field(config, "compilerOptions", "emitDeclarationOnly"), true);
      assert.deepEqual(field(config, "include"), ["src"]);
    });

    test(`packages/${name} references exactly its workspace dependencies`, () => {
      const expected = workspaceDependencies(name).map((dependency) => `../${dependency.slice("@tally/".length)}`).sort();
      assert.deepEqual(referencesOf(readJson("packages", name, "tsconfig.json")).sort(), expected);
    });
  }
});

describe("the solution", () => {
  test("the root tsconfig.json has no files and references every package", () => {
    const config = readJson("tsconfig.json");
    assert.deepEqual(field(config, "files"), []);
    assert.deepEqual(referencesOf(config).sort(), packages.map((name) => `packages/${name}`).sort());
  });

  test("npm test builds with tsc -b", () => {
    const script = field(readJson("package.json"), "scripts", "test");
    assert.ok(typeof script === "string" && /\btsc (-b|--build)\b/.test(script), String(script));
  });

  test("tsc -b builds every package and writes its declarations", () => {
    const build = spawnSync(at("node_modules", ".bin", "tsc"), ["-b", "--pretty", "false"], { cwd: at(), encoding: "utf8" });
    assert.equal(build.status, 0, `${build.stdout}${build.stderr}`.split("\n").slice(0, 12).join("\n"));
    for (const name of packages) {
      assert.ok(existsSync(at("packages", name, "dist", "index.d.ts")), `packages/${name}/dist/index.d.ts was not written`);
    }
  });
});
