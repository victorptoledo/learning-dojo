// Helpers for the contract tests: reading the repository's files.
import { existsSync, readdirSync, readFileSync, statSync } from "node:fs";
import path from "node:path";

/** The repository's root folder (the one that holds tests/). */
export const ROOT = path.resolve(import.meta.dirname, "..");

export const at = (...parts: string[]) => path.join(ROOT, ...parts);

/** JSON, or JSON with comments and trailing commas as in a tsconfig.json. */
export function readJson(...parts: string[]): unknown {
  const text = readFileSync(at(...parts), "utf8");
  const withoutComments = text.replace(/("(?:[^"\\]|\\.)*")|\/\/[^\n]*|\/\*[\s\S]*?\*\//g, (match, quoted: string | undefined) => quoted ?? "");
  return JSON.parse(withoutComments.replace(/,(\s*[}\]])/g, "$1"));
}

export function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

/** A field of a JSON object, or undefined. */
export function field(value: unknown, ...keys: string[]): unknown {
  let current = value;
  for (const key of keys) {
    current = isRecord(current) ? current[key] : undefined;
  }
  return current;
}

/** Every .ts file under a folder of the repository, as paths relative to the root. */
export function sourcesUnder(...parts: string[]): string[] {
  const found: string[] = [];
  const walk = (dir: string) => {
    if (!existsSync(dir)) {
      return;
    }
    for (const entry of readdirSync(dir)) {
      const full = path.join(dir, entry);
      if (statSync(full).isDirectory()) {
        if (entry !== "node_modules" && entry !== "dist") {
          walk(full);
        }
      } else if (entry.endsWith(".ts")) {
        found.push(path.relative(ROOT, full).split(path.sep).join("/"));
      }
    }
  };
  walk(at(...parts));
  return found.sort();
}

/** The module specifiers a source file imports or re-exports. */
export function importsOf(file: string): string[] {
  const text = readFileSync(at(file), "utf8");
  const specifiers: string[] = [];
  const pattern = /(?:\bfrom\s*|\bimport\s*\(\s*|\bimport\s+)["']([^"']+)["']/g;
  for (const match of text.matchAll(pattern)) {
    if (match[1] !== undefined) {
      specifiers.push(match[1]);
    }
  }
  return specifiers;
}

/**
 * The imports of a package that break the boundaries: relative paths that leave the package,
 * and imports of other workspace packages that are not in `allowed` or reach past their exports.
 */
export function boundaryViolations(packageDir: string, allowed: readonly string[]): string[] {
  const violations: string[] = [];
  const own = at(packageDir);
  for (const file of sourcesUnder(packageDir, "src")) {
    for (const specifier of importsOf(file)) {
      if (specifier.startsWith(".")) {
        const target = path.resolve(path.dirname(at(file)), specifier);
        if (!target.startsWith(own + path.sep)) {
          violations.push(`${file}: ${specifier} leaves the package`);
        }
      } else if (specifier.startsWith("@tally/")) {
        const name = specifier.split("/").slice(0, 2).join("/");
        if (!allowed.includes(name)) {
          violations.push(`${file}: ${specifier} is not a dependency of this package`);
        }
      }
    }
  }
  return violations;
}
