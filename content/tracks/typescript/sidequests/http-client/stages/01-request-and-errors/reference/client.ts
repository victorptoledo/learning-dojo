// A typed HTTP client on top of fetch: one request whose failures are values.

export type Method = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

export type HttpError =
  | { kind: "network"; message: string }
  | { kind: "status"; status: number; body: string }
  | { kind: "decode"; message: string };

export type Result<T, E = HttpError> = { ok: true; value: T } | { ok: false; error: E };

export type Query = Record<string, string | number | boolean | undefined>;

export type RequestOptions<T> = {
  method: Method;
  path: string;
  query?: Query;
  body?: unknown;
  decode: (json: unknown) => T;
};

export type ClientOptions = { baseUrl: string };

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

export function createClient(options: ClientOptions) {
  return {
    async request<T>(request: RequestOptions<T>): Promise<Result<T>> {
      const headers: Record<string, string> = { accept: "application/json" };
      const init: RequestInit = { method: request.method, headers };
      if (request.body !== undefined) {
        headers["content-type"] = "application/json";
        init.body = JSON.stringify(request.body);
      }
      let text: string;
      let status: number;
      try {
        const response = await fetch(urlOf(options.baseUrl, request.path, request.query), init);
        status = response.status;
        text = await response.text();
      } catch (error) {
        return { ok: false, error: { kind: "network", message: messageOf(error) } };
      }
      if (status < 200 || status > 299) {
        return { ok: false, error: { kind: "status", status, body: text } };
      }
      return decodeBody(text, request.decode);
    },
  };
}
