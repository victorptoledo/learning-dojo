// A streaming log filter: bytes -> lines -> entries -> filtered entries -> NDJSON bytes.
import { Transform, type Readable, type Writable } from "node:stream";
import { pipeline } from "node:stream/promises";
import { StringDecoder } from "node:string_decoder";

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
  const decoder = new StringDecoder("utf8");
  let rest = "";
  const pushLines = (stream: Transform, text: string) => {
    const lines = (rest + text).split("\n");
    rest = lines.pop() ?? "";
    for (const line of lines) {
      stream.push(line.endsWith("\r") ? line.slice(0, -1) : line);
    }
  };
  return new Transform({
    readableObjectMode: true,
    transform(chunk: Buffer | string, _encoding, callback) {
      pushLines(this, typeof chunk === "string" ? chunk : decoder.write(chunk));
      callback();
    },
    flush(callback) {
      pushLines(this, decoder.end());
      if (rest !== "") {
        this.push(rest.endsWith("\r") ? rest.slice(0, -1) : rest);
      }
      callback();
    },
  });
}

function isEntry(value: unknown): value is LogEntry {
  if (typeof value !== "object" || value === null) {
    return false;
  }
  const record = value as Record<string, unknown>;
  return (
    typeof record.time === "string" &&
    typeof record.msg === "string" &&
    typeof record.level === "string" &&
    (LEVELS as readonly string[]).includes(record.level)
  );
}

/**
 * Lines in, LogEntry objects out. Blank lines are skipped. A line that is not JSON, or not an
 * object with a string time, a known level and a string msg, is reported to onInvalid with its
 * 1-based line number (blank lines count) and skipped. Only those three fields are kept.
 */
export function parseEntries(onInvalid: (lineNumber: number, line: string) => void = () => {}): Transform {
  let lineNumber = 0;
  return new Transform({
    objectMode: true,
    transform(line: string, _encoding, callback) {
      lineNumber += 1;
      if (line.trim() === "") {
        callback();
        return;
      }
      let value: unknown;
      try {
        value = JSON.parse(line);
      } catch {
        value = undefined;
      }
      if (isEntry(value)) {
        callback(null, { time: value.time, level: value.level, msg: value.msg } satisfies LogEntry);
      } else {
        onInvalid(lineNumber, line);
        callback();
      }
    },
  });
}

/** Entries in, entries of level `min` or higher out. */
export function filterLevel(min: Level): Transform {
  const lowest = LEVELS.indexOf(min);
  return new Transform({
    objectMode: true,
    transform(entry: LogEntry, _encoding, callback) {
      callback(null, LEVELS.indexOf(entry.level) >= lowest ? entry : undefined);
    },
  });
}

/** Entries in, one JSON line per entry out (strings ending in "\n"). */
export function toNdjson(): Transform {
  return new Transform({
    writableObjectMode: true,
    transform(entry: LogEntry, _encoding, callback) {
      callback(null, `${JSON.stringify(entry)}\n`);
    },
  });
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
  let kept = 0;
  let invalid = 0;
  const count = new Transform({
    objectMode: true,
    transform(entry: LogEntry, _encoding, callback) {
      kept += 1;
      callback(null, entry);
    },
  });
  await pipeline(
    input,
    splitLines(),
    parseEntries(() => {
      invalid += 1;
    }),
    filterLevel(min),
    count,
    toNdjson(),
    output,
  );
  return { kept, invalid };
}

/** Counts the entries of every level, reading the source with for await. */
export async function countByLevel(source: AsyncIterable<LogEntry>): Promise<Record<Level, number>> {
  const counts: Record<Level, number> = { debug: 0, info: 0, warn: 0, error: 0 };
  for await (const entry of source) {
    counts[entry.level] += 1;
  }
  return counts;
}
