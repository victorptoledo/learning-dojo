// A streaming log report: chunks become lines, lines become entries, entries become time
// windows. Every stage is an async generator, so nothing is read before it is needed.

export const LEVELS = ["debug", "info", "warn", "error"];
// TODO: derive the union "debug" | "info" | "warn" | "error" from LEVELS.
export type Level = string;

export type LogEntry = { at: Date; level: Level; message: string };
export type Window = { start: Date; counts: Record<Level, number> };
export type LineError = { line: number; text: string };
export type Report = { windows: Window[]; errors: LineError[] };

type Source<T> = AsyncIterable<T> | Iterable<T>;

/** Splits chunks of text or UTF-8 bytes into lines, without the "\n" or "\r\n". */
export async function* lines(chunks: Source<string | Uint8Array>): AsyncGenerator<string, void, undefined> {
  // TODO: lines can be split between chunks, and so can UTF-8 characters.
  for await (const chunk of chunks) {
    yield* String(chunk).split("\n");
  }
}

/** Parses "<ISO time> <level> <message>" lines; blank lines are skipped, bad ones reported. */
export async function* parseEntries(
  source: Source<string>,
  onError: (error: LineError) => void = () => {},
): AsyncGenerator<LogEntry, void, undefined> {
  void source;
  void onError;
}

/** Counts entries per level in tumbling windows of sizeMs, aligned on multiples of sizeMs. */
export async function* windows(
  entries: Source<LogEntry>,
  sizeMs: number,
): AsyncGenerator<Window, void, undefined> {
  void entries;
  void sizeMs;
}

/** Reads the whole source and returns its windows and the lines that could not be parsed. */
export async function report(chunks: Source<string | Uint8Array>, windowMs: number): Promise<Report> {
  void chunks;
  void windowMs;
  return { windows: [], errors: [] };
}

/** The first error entry; stops reading the source as soon as it is found. */
export async function firstError(chunks: Source<string | Uint8Array>): Promise<LogEntry | undefined> {
  void chunks;
  return undefined;
}
