// A small but real command-line program: options parsed with node:util, help, exit codes.
import { parseArgs } from "node:util";

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

function isLang(value: string): value is Lang {
  return value === "en" || value === "pt" || value === "es";
}

/** "Hello, Ada!" in the given language. */
export function greeting(name: string, lang: Lang): string {
  return `${WORDS[lang]}, ${name}!`;
}

function readArgs(argv: readonly string[]) {
  return parseArgs({
    args: [...argv],
    allowPositionals: true,
    strict: true,
    options: {
      lang: { type: "string", short: "l" },
      times: { type: "string", short: "t" },
      shout: { type: "boolean", short: "s", default: false },
      help: { type: "boolean", short: "h", default: false },
    },
  });
}

/**
 * Parses argv with util.parseArgs (strict). The language comes from --lang, then from
 * defaultLang (unless empty), then "en". --times is a whole number from 1 to 5 (default 1).
 */
export function parseOptions(argv: readonly string[], defaultLang?: string): Parsed {
  let args: ReturnType<typeof readArgs>;
  try {
    args = readArgs(argv);
  } catch (error) {
    // parseArgs throws a TypeError for an unknown option or a missing value.
    return { ok: false, error: error instanceof Error ? error.message : String(error) };
  }
  const { values, positionals } = args;

  const lang = values.lang ?? (defaultLang || "en"); // an empty variable counts as unset
  if (!isLang(lang)) {
    return { ok: false, error: `unknown language: ${lang}` };
  }
  const times = values.times === undefined ? 1 : Number(values.times);
  if (!Number.isInteger(times) || times < 1 || times > 5) {
    return { ok: false, error: `--times must be a whole number from 1 to 5, got ${values.times}` };
  }
  if (!values.help && positionals.length === 0) {
    return { ok: false, error: "missing a name" };
  }
  return {
    ok: true,
    options: { names: positionals, times, shout: values.shout, lang, help: values.help },
  };
}

/** Runs the program and returns its exit code: 0 on success, 2 on a usage error. */
export function main(argv: readonly string[], io: Io): number {
  const parsed = parseOptions(argv, io.env.HELLO_LANG);
  if (!parsed.ok) {
    io.stderr(`error: ${parsed.error}`);
    io.stderr(USAGE);
    return 2;
  }
  const { options } = parsed;
  if (options.help) {
    io.stdout(USAGE);
    return 0;
  }
  for (let round = 0; round < options.times; round += 1) {
    for (const name of options.names) {
      const line = greeting(name, options.lang);
      io.stdout(options.shout ? line.toUpperCase() : line);
    }
  }
  return 0;
}
