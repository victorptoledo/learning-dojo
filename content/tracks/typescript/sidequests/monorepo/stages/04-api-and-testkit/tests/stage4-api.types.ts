import { createHandler, type ApiRequest, type ApiResponse } from "@tally/api";
import type { Money } from "@tally/core";
import { moneyOf } from "@tally/core/testing";
import type { Equal, Expect } from "./expect.ts";

// test: moneyOf returns a Money
export type MoneyOf = Expect<Equal<ReturnType<typeof moneyOf>, Money>>;

// test: the handler maps requests to responses
export type Handler = Expect<Equal<ReturnType<typeof createHandler>, (request: ApiRequest) => ApiResponse>>;
export type RequestShape = Expect<Equal<ApiRequest, { method: string; path: string; body?: unknown }>>;
export type ResponseShape = Expect<Equal<ApiResponse, { status: number; body: unknown }>>;

// test: the test kit is not part of the main entry
// @ts-expect-error: moneyOf is only in @tally/core/testing
import("@tally/core").then((core) => core.moneyOf);

// test: the CLI is not reachable through the api
// @ts-expect-error: @tally/api exports only its own API
import("@tally/api").then((api) => api.run);
