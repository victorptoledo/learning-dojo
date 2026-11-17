import { bumpVersion, type Bump } from "./version.ts";
import type { WorkspacePackage } from "./workspace.ts";

export type Change = { package: string; bump: Bump };

export type PlannedRelease = { name: string; from: string; to: string; reason: "changed" | "dependency" };

const RANK: Record<Bump, number> = { patch: 0, minor: 1, major: 2 };

const byName = (a: string, b: string) => (a < b ? -1 : a > b ? 1 : 0);

/** Every package that is part of a dependency cycle (Tarjan's strongly connected components). */
function packagesInCycles(edges: ReadonlyMap<string, readonly string[]>): string[] {
  const index = new Map<string, number>();
  const low = new Map<string, number>();
  const stack: string[] = [];
  const onStack = new Set<string>();
  const inCycles: string[] = [];
  let counter = 0;
  const visit = (node: string) => {
    index.set(node, counter);
    low.set(node, counter);
    counter += 1;
    stack.push(node);
    onStack.add(node);
    for (const next of edges.get(node) ?? []) {
      if (!index.has(next)) {
        visit(next);
        low.set(node, Math.min(low.get(node) ?? 0, low.get(next) ?? 0));
      } else if (onStack.has(next)) {
        low.set(node, Math.min(low.get(node) ?? 0, index.get(next) ?? 0));
      }
    }
    if (low.get(node) === index.get(node)) {
      const component: string[] = [];
      let member: string | undefined;
      do {
        member = stack.pop();
        if (member !== undefined) {
          onStack.delete(member);
          component.push(member);
        }
      } while (member !== undefined && member !== node);
      if (component.length > 1 || (edges.get(node) ?? []).includes(node)) {
        inCycles.push(...component);
      }
    }
  };
  for (const node of edges.keys()) {
    if (!index.has(node)) {
      visit(node);
    }
  }
  return inCycles.sort(byName);
}

export function planRelease(packages: readonly WorkspacePackage[], changes: readonly Change[]): PlannedRelease[] {
  const known = new Map(packages.map((pkg) => [pkg.name, pkg]));
  const bumps = new Map<string, Bump>();
  for (const change of changes) {
    if (!known.has(change.package)) {
      throw new Error(`unknown package ${change.package}`);
    }
    const current = bumps.get(change.package);
    if (current === undefined || RANK[change.bump] > RANK[current]) {
      bumps.set(change.package, change.bump);
    }
  }
  /** name -> the workspace packages it depends on */
  const dependsOn = new Map(packages.map((pkg) => [pkg.name, Object.keys(pkg.dependencies).filter((name) => known.has(name)).sort(byName)]));
  const cycle = packagesInCycles(dependsOn);
  if (cycle.length > 0) {
    throw new Error(`dependency cycle: ${cycle.join(", ")}`);
  }
  const dependents = new Map<string, string[]>(packages.map((pkg) => [pkg.name, []]));
  for (const [name, dependencies] of dependsOn) {
    for (const dependency of dependencies) {
      dependents.get(dependency)?.push(name);
    }
  }
  const released = new Set(bumps.keys());
  const pending = [...released];
  while (pending.length > 0) {
    const name = pending.pop() ?? "";
    for (const dependent of dependents.get(name) ?? []) {
      if (!released.has(dependent)) {
        released.add(dependent);
        pending.push(dependent);
      }
    }
  }
  // Kahn's algorithm over the released packages, the smallest name first.
  const waiting = new Map([...released].map((name) => [name, (dependsOn.get(name) ?? []).filter((dependency) => released.has(dependency)).length]));
  const plan: PlannedRelease[] = [];
  while (waiting.size > 0) {
    const ready = [...waiting].filter(([, count]) => count === 0).map(([name]) => name).sort(byName);
    const name = ready[0] ?? "";
    waiting.delete(name);
    for (const dependent of dependents.get(name) ?? []) {
      const count = waiting.get(dependent);
      if (count !== undefined) {
        waiting.set(dependent, count - 1);
      }
    }
    const pkg = known.get(name);
    if (pkg === undefined) {
      break;
    }
    const bump = bumps.get(name);
    plan.push({
      name,
      from: pkg.version,
      to: bumpVersion(pkg.version, bump ?? "patch"),
      reason: bump === undefined ? "dependency" : "changed",
    });
  }
  return plan;
}

export function tagsFor(plan: readonly PlannedRelease[]): string[] {
  return plan.map((release) => `${release.name}@${release.to}`);
}
