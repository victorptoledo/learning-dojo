import { applyRelease, bumpVersion, planRelease, readWorkspace, tagsFor, type Bump, type Change, type PlannedRelease, type WorkspacePackage } from "@tally/release";
import type { Equal, Expect } from "./expect.ts";

// test: the types of the README
export type Bumps = Expect<Equal<Bump, "major" | "minor" | "patch">>;
export type ChangeShape = Expect<Equal<Change, { package: string; bump: Bump }>>;
export type ReleaseShape = Expect<Equal<PlannedRelease, { name: string; from: string; to: string; reason: "changed" | "dependency" }>>;
export type PackageShape = Expect<Equal<WorkspacePackage, { name: string; version: string; dir: string; dependencies: Record<string, string> }>>;

// test: the functions take and return those types
export type Read = Expect<Equal<ReturnType<typeof readWorkspace>, WorkspacePackage[]>>;
export type Plan = Expect<Equal<ReturnType<typeof planRelease>, PlannedRelease[]>>;
export type Tags = Expect<Equal<ReturnType<typeof tagsFor>, string[]>>;
export type Apply = Expect<Equal<ReturnType<typeof applyRelease>, void>>;
export type BumpResult = Expect<Equal<ReturnType<typeof bumpVersion>, string>>;

// test: only the three bumps
// @ts-expect-error: "huge" is not a Bump
bumpVersion("1.0.0", "huge");
