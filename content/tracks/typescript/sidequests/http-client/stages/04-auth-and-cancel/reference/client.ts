// A typed HTTP client on top of fetch: requests whose failures are values, endpoints the
// compiler checks, timeouts and retries of what is safe to retry, auth and cancelling.

export type Method = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

export type HttpError =
  | { kind: "network"; message: string }
  | { kind: "status"; status: number; body: string }
  | { kind: "decode"; message: string }
  | { kind: "timeout"; ms: number }
  | { kind: "auth"; message: string }
  | { kind: "aborted" };

export type Result<T, E = HttpError> = { ok: true; value: T } | { ok: false; error: E };

export type Query = Record<string, string | number | boolean | undefined>;

export type RequestOptions<T> = {
  method: Method;
  path: string;
  query?: Query;
  body?: unknown;
  decode: (json: unknown) => T;
  idempotent?: boolean;
  signal?: AbortSignal;
};

export type ClientOptions = {
  baseUrl: string;
  timeoutMs?: number;
  retries?: number;
  retryDelayMs?: number;
  headers?: Record<string, string>;
  auth?: () => string | Promise<string>;
};

const RETRY_STATUSES = new Set([429, 502, 503, 504]);
const IDEMPOTENT: Record<Method, boolean> = { GET: true, PUT: true, DELETE: true, POST: false, PATCH: false };

/** Waits `ms`, or less when `signal` aborts. */
function sleep(ms: number, signal?: AbortSignal): Promise<void> {
  return new Promise<void>((resolve) => {
    const done = () => {
      clearTimeout(timer);
      signal?.removeEventListener("abort", done);
      resolve();
    };
    const timer = setTimeout(done, ms);
    signal?.addEventListener("abort", done, { once: true });
  });
}

function retryable(error: HttpError): boolean {
  switch (error.kind) {
    case "network":
    case "timeout":
      return true;
    case "status":
      return RETRY_STATUSES.has(error.status);
    case "decode":
    case "auth":
    case "aborted":
      return false;
  }
}

const messageOf = (error: unknown): string => (error instanceof Error ? error.message : String(error));

function urlOf(baseUrl: string, path: string, query: Query = {}): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined) {
      search.append(key, String(value));
    }
  }
  const text = search.toString();
  return `${baseUrl.replace(/\/$/, "")}${path}${text === "" ? "" : `?${text}`}`;
}

function decodeBody<T>(text: string, decode: (json: unknown) => T): Result<T> {
  let json: unknown;
  try {
    json = text === "" ? undefined : JSON.parse(text);
  } catch (error) {
    return { ok: false, error: { kind: "decode", message: `invalid JSON: ${messageOf(error)}` } };
  }
  try {
    return { ok: true, value: decode(json) };
  } catch (error) {
    return { ok: false, error: { kind: "decode", message: messageOf(error) } };
  }
}

type Attempt<T> = { result: Result<T>; retryAfterMs?: number };

const ABORTED: Result<never> = { ok: false, error: { kind: "aborted" } };

/** The `retry-after` header in milliseconds, when it is a number of seconds. */
function retryAfterOf(headers: Headers): number | undefined {
  const value = headers.get("retry-after")?.trim() ?? "";
  return /^\d+$/.test(value) ? Number(value) * 1000 : undefined;
}

/** One try: fetch, read the body and decode it, within `timeoutMs` and until `signal` aborts. */
async function attempt<T>(
  url: string,
  init: RequestInit,
  decode: (json: unknown) => T,
  timeoutMs?: number,
  signal?: AbortSignal,
): Promise<Attempt<T>> {
  const controller = new AbortController();
  let timedOut = false;
  const timer =
    timeoutMs === undefined
      ? undefined
      : setTimeout(() => {
          timedOut = true;
          controller.abort();
        }, timeoutMs);
  const cancel = () => controller.abort();
  signal?.addEventListener("abort", cancel, { once: true });
  let text: string;
  let response: Response;
  try {
    response = await fetch(url, { ...init, signal: controller.signal });
    text = await response.text();
  } catch (error) {
    if (signal?.aborted) {
      return { result: ABORTED };
    }
    if (timedOut && timeoutMs !== undefined) {
      return { result: { ok: false, error: { kind: "timeout", ms: timeoutMs } } };
    }
    return { result: { ok: false, error: { kind: "network", message: messageOf(error) } } };
  } finally {
    clearTimeout(timer);
    signal?.removeEventListener("abort", cancel);
  }
  const { status } = response;
  if (status < 200 || status > 299) {
    const retryAfterMs = retryAfterOf(response.headers);
    const result: Result<T> = { ok: false, error: { kind: "status", status, body: text } };
    return retryAfterMs === undefined ? { result } : { result, retryAfterMs };
  }
  return { result: decodeBody(text, decode) };
}

export function createClient(options: ClientOptions) {
  const retries = options.retries ?? 0;
  const retryDelayMs = options.retryDelayMs ?? 100;
  return {
    async request<T>(request: RequestOptions<T>): Promise<Result<T>> {
      const { signal } = request;
      const url = urlOf(options.baseUrl, request.path, request.query);
      const safe = request.idempotent ?? IDEMPOTENT[request.method];
      for (let retry = 1; ; retry += 1) {
        if (signal?.aborted) {
          return ABORTED;
        }
        const headers: Record<string, string> = { accept: "application/json" };
        const init: RequestInit = { method: request.method, headers };
        if (request.body !== undefined) {
          headers["content-type"] = "application/json";
          init.body = JSON.stringify(request.body);
        }
        Object.assign(headers, options.headers);
        if (options.auth !== undefined) {
          try {
            headers["authorization"] = `Bearer ${await options.auth()}`;
          } catch (error) {
            return { ok: false, error: { kind: "auth", message: messageOf(error) } };
          }
        }
        const { result, retryAfterMs } = await attempt(url, init, request.decode, options.timeoutMs, signal);
        if (result.ok || !safe || retry > retries || !retryable(result.error)) {
          return result;
        }
        await sleep(retryAfterMs ?? retryDelayMs * 2 ** (retry - 1), signal);
      }
    },
  };
}

/** `{ id: string; postId: string }` for `"/users/:id/posts/:postId"`, `{}` without parameters. */
export type PathParams<P extends string> = P extends `${string}:${infer Name}/${infer Rest}`
  ? { [K in Name]: string } & PathParams<`/${Rest}`>
  : P extends `${string}:${infer Name}`
    ? { [K in Name]: string }
    : {};

export type Endpoint = {
  method: Method;
  path: string;
  encode?: (input: never) => unknown;
  decode: (json: unknown) => unknown;
  idempotent?: boolean;
};

export type Api = Record<string, Endpoint>;

type ParamsArg<P extends string> = [keyof PathParams<P>] extends [never]
  ? { params?: undefined }
  : { params: PathParams<P> };

type BodyArg<E extends Endpoint> = E extends { encode: (input: infer B) => unknown } ? { body: B } : { body?: undefined };

/** The argument of an endpoint's method. */
export type CallArgs<E extends Endpoint> = ParamsArg<E["path"]> & BodyArg<E> & { query?: Query; signal?: AbortSignal };

type Call<E extends Endpoint> = {} extends CallArgs<E>
  ? (args?: CallArgs<E>) => Promise<Result<ReturnType<E["decode"]>>>
  : (args: CallArgs<E>) => Promise<Result<ReturnType<E["decode"]>>>;

export type ApiClient<A extends Api> = { [K in keyof A]: Call<A[K]> };

export function defineApi<const A extends Api>(api: A): A {
  return api;
}

function fillPath(path: string, params: Record<string, string> = {}): string {
  return path.replace(/:(\w+)/g, (_, name: string) => encodeURIComponent(params[name] ?? ""));
}

type LooseArgs = { params?: Record<string, string>; body?: unknown; query?: Query; signal?: AbortSignal };

export function createApiClient<const A extends Api>(api: A, options: ClientOptions): ApiClient<A> {
  const client = createClient(options);
  const methods: Record<string, (args?: LooseArgs) => Promise<Result<unknown>>> = {};
  for (const [name, endpoint] of Object.entries(api)) {
    const encode = endpoint.encode as ((input: unknown) => unknown) | undefined;
    methods[name] = (args = {}) => {
      const request: RequestOptions<unknown> = {
        method: endpoint.method,
        path: fillPath(endpoint.path, args.params),
        decode: endpoint.decode,
      };
      if (args.query !== undefined) {
        request.query = args.query;
      }
      if (args.signal !== undefined) {
        request.signal = args.signal;
      }
      if (endpoint.idempotent !== undefined) {
        request.idempotent = endpoint.idempotent;
      }
      if (encode !== undefined) {
        request.body = encode(args.body);
      }
      return client.request(request);
    };
  }
  return methods as ApiClient<A>;
}
