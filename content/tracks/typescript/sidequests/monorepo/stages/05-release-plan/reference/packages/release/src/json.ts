import { readFileSync } from "node:fs";

export type Json = Record<string, unknown>;

export function isRecord(value: unknown): value is Json {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

export function readPackageJson(file: string): Json {
  const json: unknown = JSON.parse(readFileSync(file, "utf8"));
  if (!isRecord(json)) {
    throw new Error(`${file} is not a JSON object`);
  }
  return json;
}
