// A typed HTTP client on top of fetch. The README of each stage lists what to export.

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

export function createClient(options: ClientOptions) {
  return {
    async request<T>(request: RequestOptions<T>): Promise<Result<T>> {
      throw new Error("todo: send the request");
    },
  };
}
