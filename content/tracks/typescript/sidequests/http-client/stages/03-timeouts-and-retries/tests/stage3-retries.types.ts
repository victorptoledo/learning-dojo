import { createClient, defineApi, type ClientOptions, type HttpError } from "../client.ts";
import type { Equal, Expect } from "./expect.ts";

// test: HttpError has a timeout member
export type Timeout = Expect<Equal<Extract<HttpError, { kind: "timeout" }>, { kind: "timeout"; ms: number }>>;

// test: the new options are optional numbers
export const plain: ClientOptions = { baseUrl: "http://localhost" };
export const full: ClientOptions = { baseUrl: "http://localhost", timeoutMs: 1000, retries: 2, retryDelayMs: 50 };
// @ts-expect-error: retries is a number
export const textRetries: ClientOptions = { baseUrl: "http://localhost", retries: "2" };

// test: requests and endpoints can say whether they are idempotent
createClient(plain).request({ method: "POST", path: "/", idempotent: true, decode: (json) => json });
defineApi({ pay: { method: "POST", path: "/pay", idempotent: true, decode: (json: unknown) => json } });
// @ts-expect-error: idempotent is a boolean
createClient(plain).request({ method: "POST", path: "/", idempotent: "yes", decode: (json) => json });
