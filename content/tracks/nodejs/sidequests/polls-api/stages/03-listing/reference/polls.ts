// A polls API on node:http alone: validation, problem details, votes and pagination.
import { randomUUID } from "node:crypto";
import { createServer, STATUS_CODES, type IncomingMessage, type Server, type ServerResponse } from "node:http";

export interface AppOptions {
  /** The clock; tests move it. */
  now?: () => Date;
  /** The largest request body accepted, in bytes (default 16384). */
  maxBodyBytes?: number;
}

export interface FieldError {
  path: string;
  message: string;
}

interface Option {
  id: number;
  text: string;
}

interface Poll {
  id: string;
  question: string;
  options: Option[];
  closesAt: Date | null;
  createdAt: Date;
  votes: Map<string, number>;
}

interface Stored {
  fingerprint: string;
  status: number;
  headers: Record<string, string>;
  body: string;
}

class Problem extends Error {
  readonly status: number;
  readonly detail: string;
  readonly errors: FieldError[] | undefined;
  readonly headers: Record<string, string>;

  constructor(status: number, detail: string, errors?: FieldError[], headers: Record<string, string> = {}) {
    super(detail);
    this.status = status;
    this.detail = detail;
    this.errors = errors;
    this.headers = headers;
  }
}

const MAX_QUESTION = 200;
const MAX_OPTION = 100;
const VOTER = /^[A-Za-z0-9_-]{1,64}$/;
const IDEMPOTENCY_KEY = /^[\x21-\x7e]{1,128}$/;

interface Answer {
  status: number;
  headers: Record<string, string>;
  body: string;
}

function jsonAnswer(status: number, value: unknown, headers: Record<string, string> = {}): Answer {
  return { status, headers: { "content-type": "application/json; charset=utf-8", ...headers }, body: JSON.stringify(value) };
}

function problemAnswer(problem: Problem): Answer {
  const body: Record<string, unknown> = {
    type: "about:blank",
    title: STATUS_CODES[problem.status] ?? "Error",
    status: problem.status,
    detail: problem.detail,
  };
  if (problem.errors !== undefined) body.errors = problem.errors;
  return {
    status: problem.status,
    headers: { "content-type": "application/problem+json", ...problem.headers },
    body: JSON.stringify(body),
  };
}

function mediaType(req: IncomingMessage): string {
  return (req.headers["content-type"] ?? "").split(";")[0]?.trim().toLowerCase() ?? "";
}

/** The whole body as text; past the limit the rest is read and thrown away, then 413. */
async function readBody(req: IncomingMessage, limit: number): Promise<string> {
  const chunks: Buffer[] = [];
  let size = 0;
  for await (const chunk of req as AsyncIterable<Buffer>) {
    size += chunk.length;
    if (size <= limit) chunks.push(chunk);
  }
  if (size > limit) throw new Problem(413, `the body is larger than ${limit} bytes`);
  return Buffer.concat(chunks).toString("utf8");
}

function parseJson(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    throw new Problem(400, "the body is not valid JSON");
  }
}

const isObject = (value: unknown): value is Record<string, unknown> =>
  typeof value === "object" && value !== null && !Array.isArray(value);

function checkText(value: unknown, path: string, max: number, errors: FieldError[]): string | undefined {
  if (value === undefined) {
    errors.push({ path, message: "is required" });
  } else if (typeof value !== "string") {
    errors.push({ path, message: "must be a string" });
  } else if (value.trim().length < 1 || value.trim().length > max) {
    errors.push({ path, message: `must be 1 to ${max} characters` });
  } else {
    return value.trim();
  }
  return undefined;
}

function checkClosesAt(value: unknown, now: Date, errors: FieldError[]): Date | null | undefined {
  if (value === null) return null;
  if (typeof value !== "string" || Number.isNaN(Date.parse(value))) {
    errors.push({ path: "closesAt", message: "must be a date and time" });
    return undefined;
  }
  const date = new Date(value);
  if (date.getTime() <= now.getTime()) {
    errors.push({ path: "closesAt", message: "must be in the future" });
    return undefined;
  }
  return date;
}

function checkUnknown(body: Record<string, unknown>, allowed: readonly string[], errors: FieldError[]): void {
  for (const key of Object.keys(body)) {
    if (!allowed.includes(key)) errors.push({ path: key, message: "is not allowed" });
  }
}

function validateNewPoll(body: unknown, now: Date): { question: string; options: string[]; closesAt: Date | null } {
  if (!isObject(body)) throw new Problem(422, "the body is not valid", [{ path: "", message: "must be an object" }]);
  const errors: FieldError[] = [];
  const question = checkText(body.question, "question", MAX_QUESTION, errors);
  const options: string[] = [];
  if (body.options === undefined) {
    errors.push({ path: "options", message: "is required" });
  } else if (!Array.isArray(body.options)) {
    errors.push({ path: "options", message: "must be an array" });
  } else {
    if (body.options.length < 2 || body.options.length > 10) {
      errors.push({ path: "options", message: "must have 2 to 10 items" });
    }
    const seen = new Set<string>();
    body.options.forEach((item: unknown, index) => {
      const text = checkText(item, `options[${index}]`, MAX_OPTION, errors);
      if (text === undefined) return;
      if (seen.has(text.toLowerCase())) {
        errors.push({ path: `options[${index}]`, message: "is a duplicate" });
      }
      seen.add(text.toLowerCase());
      options.push(text);
    });
  }
  const closesAt = body.closesAt === undefined ? null : checkClosesAt(body.closesAt, now, errors);
  checkUnknown(body, ["question", "options", "closesAt"], errors);
  if (errors.length > 0 || question === undefined || closesAt === undefined) {
    throw new Problem(422, "the body is not valid", errors);
  }
  return { question, options, closesAt };
}

function encodeCursor(poll: Poll): string {
  return Buffer.from(JSON.stringify([poll.createdAt.getTime(), poll.id])).toString("base64url");
}

function decodeCursor(cursor: string): [number, string] | undefined {
  try {
    const value: unknown = JSON.parse(Buffer.from(cursor, "base64url").toString("utf8"));
    if (Array.isArray(value) && value.length === 2 && typeof value[0] === "number" && typeof value[1] === "string") {
      return [value[0], value[1]];
    }
  } catch {
    // not ours
  }
  return undefined;
}

/** Newest first; polls created in the same millisecond by id, descending. */
function newestFirst(a: Poll, b: Poll): number {
  return b.createdAt.getTime() - a.createdAt.getTime() || (a.id < b.id ? 1 : a.id > b.id ? -1 : 0);
}

export function createApp(options: AppOptions = {}): Server {
  const now = options.now ?? (() => new Date());
  const maxBody = options.maxBodyBytes ?? 16384;
  const polls = new Map<string, Poll>();
  const idempotent = new Map<string, Stored>();

  const statusOf = (poll: Poll) =>
    poll.closesAt !== null && now().getTime() >= poll.closesAt.getTime() ? "closed" : "open";

  const view = (poll: Poll) => ({
    id: poll.id,
    question: poll.question,
    options: poll.options,
    status: statusOf(poll),
    closesAt: poll.closesAt?.toISOString() ?? null,
    createdAt: poll.createdAt.toISOString(),
  });

  const results = (poll: Poll) => {
    const counts = poll.options.map((option) => ({
      ...option,
      votes: [...poll.votes.values()].filter((id) => id === option.id).length,
    }));
    const max = Math.max(0, ...counts.map((option) => option.votes));
    return {
      pollId: poll.id,
      status: statusOf(poll),
      total: poll.votes.size,
      options: counts,
      winners: max === 0 ? [] : counts.filter((option) => option.votes === max).map((option) => option.id),
    };
  };

  const findPoll = (id: string): Poll => {
    const poll = polls.get(id);
    if (poll === undefined) throw new Problem(404, `no poll ${id}`);
    return poll;
  };

  async function readJson(req: IncomingMessage): Promise<{ raw: string; body: unknown }> {
    if (mediaType(req) !== "application/json") throw new Problem(415, "expected a JSON body (application/json)");
    const raw = await readBody(req, maxBody);
    return { raw, body: parseJson(raw) };
  }

  /** Runs a POST once per Idempotency-Key: a retry with the same request gets the same answer. */
  async function once(req: IncomingMessage, path: string, raw: string, run: () => Answer): Promise<Answer> {
    const key = req.headers["idempotency-key"];
    if (key === undefined) return run();
    if (typeof key !== "string" || !IDEMPOTENCY_KEY.test(key)) throw new Problem(400, "invalid Idempotency-Key header");
    const fingerprint = JSON.stringify([req.method, path, req.headers["voter-id"] ?? "", raw]);
    const stored = idempotent.get(key);
    if (stored !== undefined) {
      if (stored.fingerprint !== fingerprint) {
        throw new Problem(422, "this Idempotency-Key was used for a different request");
      }
      return { status: stored.status, headers: { ...stored.headers, "idempotent-replayed": "true" }, body: stored.body };
    }
    let answer: Answer;
    try {
      answer = run();
    } catch (error) {
      if (!(error instanceof Problem)) throw error;
      answer = problemAnswer(error);
    }
    idempotent.set(key, { fingerprint, ...answer });
    return answer;
  }

  async function createPoll(req: IncomingMessage): Promise<Answer> {
    const { raw, body } = await readJson(req);
    return once(req, "/polls", raw, () => {
      const fields = validateNewPoll(body, now());
      const poll: Poll = {
        id: randomUUID(),
        question: fields.question,
        options: fields.options.map((text, index) => ({ id: index + 1, text })),
        closesAt: fields.closesAt,
        createdAt: now(),
        votes: new Map(),
      };
      polls.set(poll.id, poll);
      return jsonAnswer(201, view(poll), { location: `/polls/${poll.id}` });
    });
  }

  function listPolls(url: URL): Answer {
    const errors: FieldError[] = [];
    const limitText = url.searchParams.get("limit");
    let limit = 20;
    if (limitText !== null) {
      limit = Number(limitText);
      if (!/^\d+$/.test(limitText) || limit < 1 || limit > 100) {
        errors.push({ path: "limit", message: "must be a whole number from 1 to 100" });
      }
    }
    const status = url.searchParams.get("status");
    if (status !== null && status !== "open" && status !== "closed") {
      errors.push({ path: "status", message: "must be open or closed" });
    }
    const cursorText = url.searchParams.get("cursor");
    const cursor = cursorText === null ? undefined : decodeCursor(cursorText);
    if (cursorText !== null && cursor === undefined) {
      errors.push({ path: "cursor", message: "is not a valid cursor" });
    }
    if (errors.length > 0) throw new Problem(400, "the query is not valid", errors);
    let all = [...polls.values()].sort(newestFirst);
    if (status !== null) all = all.filter((poll) => statusOf(poll) === status);
    if (cursor !== undefined) {
      const [time, id] = cursor;
      all = all.filter((poll) => poll.createdAt.getTime() < time || (poll.createdAt.getTime() === time && poll.id < id));
    }
    const page = all.slice(0, limit);
    const last = page.at(-1);
    const nextCursor = all.length > limit && last !== undefined ? encodeCursor(last) : null;
    const headers: Record<string, string> = {};
    if (nextCursor !== null) {
      const next = new URLSearchParams({ limit: String(limit) });
      if (status !== null) next.set("status", status);
      next.set("cursor", nextCursor);
      headers.link = `</polls?${next.toString()}>; rel="next"`;
    }
    return jsonAnswer(200, { items: page.map(view), nextCursor }, headers);
  }

  async function vote(req: IncomingMessage, poll: Poll): Promise<Answer> {
    const voter = req.headers["voter-id"];
    if (typeof voter !== "string" || !VOTER.test(voter)) {
      throw new Problem(400, "send a voter-id header (1 to 64 letters, digits, _ or -)");
    }
    const { raw, body } = await readJson(req);
    return once(req, `/polls/${poll.id}/votes`, raw, () => {
      const optionId = isObject(body) ? body.optionId : undefined;
      if (!isObject(body) || typeof optionId !== "number" || !poll.options.some((option) => option.id === optionId)) {
        throw new Problem(422, "the body is not valid", [{ path: "optionId", message: "must be the id of an option" }]);
      }
      if (statusOf(poll) === "closed") throw new Problem(409, "the poll is closed");
      if (poll.votes.has(voter)) throw new Problem(409, "this voter already voted");
      poll.votes.set(voter, optionId);
      return jsonAnswer(201, { pollId: poll.id, optionId });
    });
  }

  function notAllowed(allow: string): never {
    throw new Problem(405, "method not allowed", undefined, { allow });
  }

  async function route(req: IncomingMessage): Promise<Answer> {
    const url = new URL(req.url ?? "/", "http://localhost");
    const parts = url.pathname.split("/").filter((part) => part !== "");
    const method = req.method ?? "GET";
    if (parts[0] !== "polls" || parts.length > 3) throw new Problem(404, `no route for ${url.pathname}`);
    if (parts.length === 1) {
      if (method === "POST") return createPoll(req);
      if (method === "GET") return listPolls(url);
      return notAllowed("GET, POST");
    }
    const poll = findPoll(decodeURIComponent(parts[1] ?? ""));
    if (parts.length === 2) {
      if (method === "GET") return jsonAnswer(200, view(poll));
      return notAllowed("GET");
    }
    if (parts[2] === "votes") {
      if (method === "POST") return vote(req, poll);
      return notAllowed("POST");
    }
    if (parts[2] === "results") {
      if (method === "GET") return jsonAnswer(200, results(poll));
      return notAllowed("GET");
    }
    throw new Problem(404, `no route for ${url.pathname}`);
  }

  async function handle(req: IncomingMessage, res: ServerResponse): Promise<void> {
    let answer: Answer;
    try {
      answer = await route(req);
    } catch (error) {
      answer = problemAnswer(error instanceof Problem ? error : new Problem(500, "internal error"));
    }
    if (!req.complete) req.resume(); // an answer before the body was read: drain it
    res.writeHead(answer.status, answer.headers);
    res.end(answer.body);
  }

  return createServer((req, res) => {
    void handle(req, res);
  });
}
