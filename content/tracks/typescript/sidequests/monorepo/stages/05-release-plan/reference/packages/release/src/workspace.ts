import { existsSync, readdirSync, statSync } from "node:fs";
import path from "node:path";
import { isRecord, readPackageJson } from "./json.ts";

export type WorkspacePackage = { name: string; version: string; dir: string; dependencies: Record<string, string> };

/** The folders the root package.json's "workspaces" name, relative to root. */
function workspaceDirs(root: string): string[] {
  const workspaces = readPackageJson(path.join(root, "package.json"))["workspaces"];
  const dirs: string[] = [];
  for (const pattern of Array.isArray(workspaces) ? workspaces : []) {
    if (typeof pattern !== "string") {
      continue;
    }
    if (pattern.endsWith("/*")) {
      const parent = pattern.slice(0, -2);
      const full = path.join(root, parent);
      if (existsSync(full)) {
        for (const entry of readdirSync(full)) {
          if (statSync(path.join(full, entry)).isDirectory() && existsSync(path.join(full, entry, "package.json"))) {
            dirs.push(path.posix.join(parent, entry));
          }
        }
      }
    } else if (existsSync(path.join(root, pattern, "package.json"))) {
      dirs.push(path.posix.normalize(pattern));
    }
  }
  return dirs;
}

export function readWorkspace(root: string): WorkspacePackage[] {
  return workspaceDirs(root)
    .map((dir) => {
      const json = readPackageJson(path.join(root, dir, "package.json"));
      const dependencies: Record<string, string> = {};
      const declared = json["dependencies"];
      if (isRecord(declared)) {
        for (const [name, range] of Object.entries(declared)) {
          dependencies[name] = String(range);
        }
      }
      return { name: String(json["name"]), version: String(json["version"]), dir, dependencies };
    })
    .sort((a, b) => (a.name < b.name ? -1 : a.name > b.name ? 1 : 0));
}
