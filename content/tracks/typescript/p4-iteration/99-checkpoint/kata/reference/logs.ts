// A streaming log report: chunks become lines, lines become entries, entries become time
// windows. Every stage is an async generator, so nothing is read before it is needed.

export const LEVELS = ["debug", "info", "warn", "error"] as const;
export type Level = (typeof LEVELS)[number];

export type LogEntry = { at: Date; level: Level; message: string };
export type Window = { start: Date; counts: Record<Level, number> };
export type LineError = { line: number; text: string };
export type Report = { windows: Window[]; errors: LineError[] };

type Source<T> = AsyncIterable<T> | Iterable<T>;

/** Splits chunks of text or UTF-8 bytes into lines, without the "\n" or "\r\n". */
export async function* lines(chunks: Source<string | Uint8Array>): AsyncGenerator<string, void, undefined> {
  const decoder = new TextDecoder();
  let buffer = "";
  for await (const chunk of chunks) {
    buffer += typeof chunk === "string" ? chunk : decoder.decode(chunk, { stream: true });
    let end = buffer.indexOf("\n");
    while (end !== -1) {
      yield withoutCr(buffer.slice(0, end));
      buffer = buffer.slice(end + 1);
      end = buffer.indexOf("\n");
    }
  }
  buffer += decoder.decode();
  if (buffer !== "") {
    yield withoutCr(buffer);
  }
}

function withoutCr(line: string): string {
  return line.endsWith("\r") ? line.slice(0, -1) : line;
}

const ENTRY = /^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,3})?Z) (\S+) (.+)$/;

function isLevel(value: string): value is Level {
  return (LEVELS as readonly string[]).includes(value);
}

/** Parses "<ISO time> <level> <message>" lines; blank lines are skipped, bad ones reported. */
export async function* parseEntries(
  source: Source<string>,
  onError: (error: LineError) => void = () => {},
): AsyncGenerator<LogEntry, void, undefined> {
  let line = 0;
  for await (const text of source) {
    line += 1;
    if (text.trim() === "") {
      continue;
    }
    const match = ENTRY.exec(text);
    const at = match ? new Date(match[1] ?? "") : undefined;
    const level = match?.[2] ?? "";
    if (!match || !at || Number.isNaN(at.getTime()) || !isLevel(level)) {
      onError({ line, text });
      continue;
    }
    yield { at, level, message: match[3] ?? "" };
  }
}

function emptyCounts(): Record<Level, number> {
  return { debug: 0, info: 0, warn: 0, error: 0 };
}

/** Counts entries per level in tumbling windows of sizeMs, aligned on multiples of sizeMs. */
export async function* windows(
  entries: Source<LogEntry>,
  sizeMs: number,
): AsyncGenerator<Window, void, undefined> {
  if (!Number.isInteger(sizeMs) || sizeMs < 1) {
    throw new RangeError("sizeMs must be a positive integer");
  }
  let current: Window | undefined;
  for await (const entry of entries) {
    const start = Math.floor(entry.at.getTime() / sizeMs) * sizeMs;
    if (current && start < current.start.getTime()) {
      throw new RangeError(`entry out of order: ${entry.at.toISOString()}`);
    }
    if (current && start !== current.start.getTime()) {
      yield current;
      current = undefined;
    }
    current ??= { start: new Date(start), counts: emptyCounts() };
    current.counts[entry.level] += 1;
  }
  if (current) {
    yield current;
  }
}

/** Reads the whole source and returns its windows and the lines that could not be parsed. */
export async function report(chunks: Source<string | Uint8Array>, windowMs: number): Promise<Report> {
  const errors: LineError[] = [];
  const result = await Array.fromAsync(
    windows(
      parseEntries(lines(chunks), (error) => errors.push(error)),
      windowMs,
    ),
  );
  return { windows: result, errors };
}

/** The first error entry; stops reading the source as soon as it is found. */
export async function firstError(chunks: Source<string | Uint8Array>): Promise<LogEntry | undefined> {
  for await (const entry of parseEntries(lines(chunks))) {
    if (entry.level === "error") {
      return entry;
    }
  }
  return undefined;
}
