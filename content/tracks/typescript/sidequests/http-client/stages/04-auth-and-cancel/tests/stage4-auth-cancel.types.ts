import { createApiClient, createClient, defineApi, type ClientOptions, type HttpError } from "../client.ts";
import type { Equal, Expect } from "./expect.ts";

// test: HttpError has auth and aborted members
export type Auth = Expect<Equal<Extract<HttpError, { kind: "auth" }>, { kind: "auth"; message: string }>>;
export type Aborted = Expect<Equal<Extract<HttpError, { kind: "aborted" }>, { kind: "aborted" }>>;

// test: headers and auth are options
export const options: ClientOptions[] = [
  { baseUrl: "http://localhost", headers: { "x-a": "1" } },
  { baseUrl: "http://localhost", auth: () => "token" },
  { baseUrl: "http://localhost", auth: async () => "token" },
];
// @ts-expect-error: auth returns the token as text
export const numberToken: ClientOptions = { baseUrl: "http://localhost", auth: () => 42 };

// test: requests and api calls take a signal
const signal = new AbortController().signal;
createClient({ baseUrl: "http://localhost" }).request({ method: "GET", path: "/", decode: (json) => json, signal });
const api = defineApi({ list: { method: "GET", path: "/items", decode: (json: unknown) => json } });
createApiClient(api, { baseUrl: "http://localhost" }).list({ signal });
// @ts-expect-error: signal is an AbortSignal
createApiClient(api, { baseUrl: "http://localhost" }).list({ signal: true });

// test: a switch over HttpError can be exhaustive
export function describeError(error: HttpError): string {
  switch (error.kind) {
    case "network":
      return error.message;
    case "status":
      return String(error.status);
    case "decode":
      return error.message;
    case "timeout":
      return `${error.ms} ms`;
    case "auth":
      return error.message;
    case "aborted":
      return "aborted";
  }
}
