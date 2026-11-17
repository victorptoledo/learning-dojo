export type Bump = "major" | "minor" | "patch";

const VERSION = /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$/;

export function bumpVersion(version: string, bump: Bump): string {
  const match = VERSION.exec(version);
  if (match === null) {
    throw new Error(`invalid version: ${version}`);
  }
  const [major, minor, patch] = match.slice(1).map(Number) as [number, number, number];
  switch (bump) {
    case "major":
      return `${major + 1}.0.0`;
    case "minor":
      return `${major}.${minor + 1}.0`;
    case "patch":
      return `${major}.${minor}.${patch + 1}`;
  }
}
