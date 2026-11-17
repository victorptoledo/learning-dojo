import { writeFileSync } from "node:fs";
import path from "node:path";
import { isRecord, readPackageJson } from "./json.ts";
import type { PlannedRelease } from "./plan.ts";
import { readWorkspace } from "./workspace.ts";

export function applyRelease(root: string, plan: readonly PlannedRelease[]): void {
  const versions = new Map(plan.map((release) => [release.name, release.to]));
  for (const pkg of readWorkspace(root)) {
    const file = path.join(root, pkg.dir, "package.json");
    const json = readPackageJson(file);
    let changed = false;
    const version = versions.get(pkg.name);
    if (version !== undefined && json["version"] !== version) {
      json["version"] = version;
      changed = true;
    }
    const dependencies = json["dependencies"];
    if (isRecord(dependencies)) {
      for (const name of Object.keys(dependencies)) {
        const next = versions.get(name);
        if (next !== undefined && dependencies[name] !== `^${next}`) {
          dependencies[name] = `^${next}`;
          changed = true;
        }
      }
    }
    if (changed) {
      writeFileSync(file, `${JSON.stringify(json, null, 2)}\n`);
    }
  }
}
