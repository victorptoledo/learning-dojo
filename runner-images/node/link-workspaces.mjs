// Makes node_modules for a check in the current folder, offline. Without "workspaces" in the
// root package.json it is a link to the packages preinstalled in the image. With them, it is
// a folder holding a link to each preinstalled package plus a link to each workspace package
// under its own name, as `npm install` does in a monorepo, so `import "@scope/core"` works.
// "workspaces" entries are folders ("tools/release") or folders ending in "/*" ("packages/*").
import { existsSync, mkdirSync, readFileSync, readdirSync, symlinkSync } from "node:fs";
import path from "node:path";

const IMAGE = "/opt/dojo/node_modules";

function readJson(file) {
  try {
    return JSON.parse(readFileSync(file, "utf8"));
  } catch {
    return undefined;
  }
}

function workspaceDirs() {
  const root = readJson("package.json");
  if (!root || !Array.isArray(root.workspaces)) {
    return [];
  }
  const dirs = [];
  for (const pattern of root.workspaces) {
    if (typeof pattern !== "string") {
      continue;
    }
    if (pattern.endsWith("/*")) {
      const parent = pattern.slice(0, -2);
      if (existsSync(parent)) {
        for (const entry of readdirSync(parent, { withFileTypes: true })) {
          if (entry.isDirectory()) {
            dirs.push(path.join(parent, entry.name));
          }
        }
      }
    } else {
      dirs.push(path.normalize(pattern));
    }
  }
  return dirs.sort();
}

const dirs = workspaceDirs();
if (dirs.length === 0) {
  symlinkSync(IMAGE, "node_modules");
  process.exit(0);
}

mkdirSync("node_modules");
for (const entry of readdirSync(IMAGE)) {
  symlinkSync(path.join(IMAGE, entry), path.join("node_modules", entry));
}
for (const dir of dirs) {
  const name = readJson(path.join(dir, "package.json"))?.name;
  if (typeof name !== "string" || !/^(@[a-z0-9._-]+\/)?[a-z0-9._-]+$/.test(name)) {
    continue;
  }
  const link = path.join("node_modules", name);
  if (existsSync(link)) {
    console.log(`warning: workspace package ${name} has the name of a preinstalled package; skipped`);
    continue;
  }
  mkdirSync(path.dirname(link), { recursive: true });
  symlinkSync(path.relative(path.dirname(link), dir), link);
}
