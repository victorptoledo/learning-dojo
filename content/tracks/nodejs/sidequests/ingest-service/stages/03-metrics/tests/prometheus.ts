// A small reader for the Prometheus text format, enough for the contract tests.
import assert from "node:assert/strict";

export interface Sample {
  name: string;
  labels: Record<string, string>;
  value: number;
}

export interface Scrape {
  samples: Sample[];
  help: Map<string, string>;
  types: Map<string, string>;
  /** The one sample with this name whose labels include these. */
  value(name: string, labels?: Record<string, string>): number;
  /** Like value(), but 0 when there is no such sample. */
  valueOr0(name: string, labels?: Record<string, string>): number;
}

const SAMPLE = /^([a-zA-Z_:][a-zA-Z0-9_:]*)(?:\{(.*)\})?\s+(\S+)$/;
const LABEL = /([a-zA-Z_][a-zA-Z0-9_]*)="((?:[^"\\]|\\.)*)"/g;

export function parse(text: string): Scrape {
  const samples: Sample[] = [];
  const help = new Map<string, string>();
  const types = new Map<string, string>();
  for (const line of text.split("\n")) {
    if (line.trim() === "") continue;
    const comment = /^# (HELP|TYPE) (\S+) (.*)$/.exec(line);
    if (comment) {
      (comment[1] === "HELP" ? help : types).set(comment[2] ?? "", comment[3] ?? "");
      continue;
    }
    if (line.startsWith("#")) continue;
    const match = SAMPLE.exec(line);
    assert.ok(match, `not a sample line: ${JSON.stringify(line)}`);
    const labels: Record<string, string> = {};
    for (const [, key, value] of (match[2] ?? "").matchAll(LABEL)) labels[key ?? ""] = value ?? "";
    const raw = match[3] ?? "";
    const value = raw === "+Inf" ? Infinity : raw === "-Inf" ? -Infinity : Number(raw);
    assert.ok(!Number.isNaN(value) || raw === "NaN", `not a number: ${line}`);
    samples.push({ name: match[1] ?? "", labels, value });
  }
  const matching = (name: string, labels: Record<string, string>) =>
    samples.filter((sample) => sample.name === name && Object.entries(labels).every(([key, value]) => sample.labels[key] === value));
  return {
    samples,
    help,
    types,
    value(name, labels = {}) {
      const found = matching(name, labels);
      assert.equal(found.length, 1, `expected one sample ${name}${JSON.stringify(labels)}, found ${found.length}`);
      return found[0]?.value ?? NaN;
    },
    valueOr0(name, labels = {}) {
      const found = matching(name, labels);
      return found.length === 0 ? 0 : (found[0]?.value ?? 0);
    },
  };
}

export async function scrape(base: string): Promise<Scrape> {
  const response = await fetch(`${base}/metrics`);
  assert.equal(response.status, 200);
  assert.match(response.headers.get("content-type") ?? "", /^text\/plain; version=0\.0\.4/);
  return parse(await response.text());
}
