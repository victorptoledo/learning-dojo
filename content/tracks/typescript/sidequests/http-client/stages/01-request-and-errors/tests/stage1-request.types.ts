import { createClient, type HttpError, type Result } from "../client.ts";
import type { Equal, Expect } from "./expect.ts";

const client = createClient({ baseUrl: "http://localhost" });
const request = client.request({ method: "GET", path: "/n", decode: (json) => Number(json) });

// test: request resolves to a Result of what decode returns
export type RequestType = Expect<Equal<typeof request, Promise<Result<number, HttpError>>>>;

// test: HttpError is a union on kind with the members of the README
export type Network = Expect<Equal<Extract<HttpError, { kind: "network" }>, { kind: "network"; message: string }>>;
export type Status = Expect<Equal<Extract<HttpError, { kind: "status" }>, { kind: "status"; status: number; body: string }>>;
export type Decode = Expect<Equal<Extract<HttpError, { kind: "decode" }>, { kind: "decode"; message: string }>>;
export function statusOf(error: HttpError): number | undefined {
  return error.kind === "status" ? error.status : undefined;
}

// test: the value can only be read after checking ok
export async function valueOf(): Promise<number | undefined> {
  const result = await request;
  // @ts-expect-error: the result may be a failure
  result.value;
  return result.ok ? result.value : undefined;
}

// test: only the HTTP methods of the README compile
// @ts-expect-error: "FETCH" is not a method
client.request({ method: "FETCH", path: "/", decode: (json) => json });

// test: decode is required
// @ts-expect-error: decode is missing
client.request({ method: "GET", path: "/" });

// test: query values are text, numbers or booleans
// @ts-expect-error: a query value cannot be an object
client.request({ method: "GET", path: "/", query: { filter: { a: 1 } }, decode: (json) => json });
