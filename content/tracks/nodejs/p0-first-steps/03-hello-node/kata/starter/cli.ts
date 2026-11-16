// A small but real command-line program: options parsed with node:util, help, exit codes.
// Tip: `import { parseArgs } from "node:util";` does most of the parsing for you.

/** Where the program reads its environment and writes its output: process in bin.ts, fakes in tests. */
export interface Io {
  env: Readonly<Record<string, string | undefined>>;
  stdout: (line: string) => void;
  stderr: (line: string) => void;
}

export type Lang = "en" | "pt" | "es";

export interface Options {
  names: string[];
  times: number;
  shout: boolean;
  lang: Lang;
  help: boolean;
}

export type Parsed = { ok: true; options: Options } | { ok: false; error: string };

export const USAGE = "usage: hello [--lang en|pt|es] [--times N] [--shout] <name...>";

export const WORDS: Record<Lang, string> = { en: "Hello", pt: "Olá", es: "Hola" };

/** "Hello, Ada!" in the given language. */
export function greeting(name: string, lang: Lang): string {
  throw new Error("todo: write greeting()");
}

/**
 * Parses argv with util.parseArgs (strict). The language comes from --lang, then from
 * defaultLang (unless empty), then "en". --times is a whole number from 1 to 5 (default 1).
 */
export function parseOptions(argv: readonly string[], defaultLang?: string): Parsed {
  throw new Error("todo: write parseOptions()");
}

/** Runs the program and returns its exit code: 0 on success, 2 on a usage error. */
export function main(argv: readonly string[], io: Io): number {
  throw new Error("todo: write main()");
}
