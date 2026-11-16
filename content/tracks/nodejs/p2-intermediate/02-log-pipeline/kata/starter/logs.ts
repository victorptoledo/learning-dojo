// A streaming log filter: bytes -> lines -> entries -> filtered entries -> NDJSON bytes.
// Useful imports: Transform from "node:stream", pipeline from "node:stream/promises",
// StringDecoder from "node:string_decoder".
import type { Readable, Transform, Writable } from "node:stream";

export const LEVELS = ["debug", "info", "warn", "error"] as const;
export type Level = (typeof LEVELS)[number];

export interface LogEntry {
  time: string;
  level: Level;
  msg: string;
}

/**
 * Bytes (or strings) in, one string per line out (object mode), without the "\n" or "\r\n".
 * Lines and UTF-8 characters may be split across chunks. A last line without "\n" is kept.
 */
export function splitLines(): Transform {
  throw new Error("todo: write splitLines()");
}

/**
 * Lines in, LogEntry objects out. Blank lines are skipped. A line that is not JSON, or not an
 * object with a string time, a known level and a string msg, is reported to onInvalid with its
 * 1-based line number (blank lines count) and skipped. Only those three fields are kept.
 */
export function parseEntries(onInvalid: (lineNumber: number, line: string) => void = () => {}): Transform {
  throw new Error("todo: write parseEntries()");
}

/** Entries in, entries of level `min` or higher out. */
export function filterLevel(min: Level): Transform {
  throw new Error("todo: write filterLevel()");
}

/** Entries in, one JSON line per entry out (strings ending in "\n"). */
export function toNdjson(): Transform {
  throw new Error("todo: write toNdjson()");
}

/**
 * Streams input through the four steps into output and resolves with how many entries were
 * written and how many lines were invalid. Rejects with the first error of any stream (and
 * pipeline destroys the others).
 */
export async function filterLog(
  input: Readable,
  output: Writable,
  min: Level,
): Promise<{ kept: number; invalid: number }> {
  throw new Error("todo: write filterLog()");
}

/** Counts the entries of every level, reading the source with for await. */
export async function countByLevel(source: AsyncIterable<LogEntry>): Promise<Record<Level, number>> {
  throw new Error("todo: write countByLevel()");
}
