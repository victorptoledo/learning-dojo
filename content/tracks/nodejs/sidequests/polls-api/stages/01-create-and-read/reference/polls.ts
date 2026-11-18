// A polls API on node:http alone: validation and problem details.
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

export function createApp(options: AppOptions = {}): Server {
  const now = options.now ?? (() => new Date());
  const maxBody = options.maxBodyBytes ?? 16384;
  const polls = new Map<string, Poll>();

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

  const findPoll = (id: string): Poll => {
    const poll = polls.get(id);
    if (poll === undefined) throw new Problem(404, `no poll ${id}`);
    return poll;
  };

  async function readJson(req: IncomingMessage): Promise<{ body: unknown }> {
    if (mediaType(req) !== "application/json") throw new Problem(415, "expected a JSON body (application/json)");
    const raw = await readBody(req, maxBody);
    return { body: parseJson(raw) };
  }

  async function createPoll(req: IncomingMessage): Promise<Answer> {
    const { body } = await readJson(req);
    const fields = validateNewPoll(body, now());
    const poll: Poll = {
      id: randomUUID(),
      question: fields.question,
      options: fields.options.map((text, index) => ({ id: index + 1, text })),
      closesAt: fields.closesAt,
      createdAt: now(),
    };
    polls.set(poll.id, poll);
    return jsonAnswer(201, view(poll), { location: `/polls/${poll.id}` });
  }

  function notAllowed(allow: string): never {
    throw new Problem(405, "method not allowed", undefined, { allow });
  }

  async function route(req: IncomingMessage): Promise<Answer> {
    const url = new URL(req.url ?? "/", "http://localhost");
    const parts = url.pathname.split("/").filter((part) => part !== "");
    const method = req.method ?? "GET";
    if (parts[0] !== "polls" || parts.length > 2) throw new Problem(404, `no route for ${url.pathname}`);
    if (parts.length === 1) {
      if (method === "POST") return createPoll(req);
      return notAllowed("POST");
    }
    const poll = findPoll(decodeURIComponent(parts[1] ?? ""));
    if (method === "GET") return jsonAnswer(200, view(poll));
    return notAllowed("GET");
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
