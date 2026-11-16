// W3C Trace Context: the traceparent header that ties a browser, an API and its dependencies.
import { randomBytes } from "node:crypto";

export interface IncomingTrace {
  traceId: string;
  parentId: string;
  flags: string;
}

export interface SpanContext {
  traceId: string;
  spanId: string;
  flags: string;
}

/** 32 lower-case hex digits. */
export function newTraceId(): string {
  return randomBytes(16).toString("hex");
}

/** 16 lower-case hex digits. */
export function newSpanId(): string {
  return randomBytes(8).toString("hex");
}

/** The trace of a traceparent header, or undefined when it is missing or invalid. */
export function parseTraceparent(header: string | undefined): IncomingTrace | undefined {
  throw new Error("todo: write parseTraceparent()");
}

/** 00-TRACEID-SPANID-FLAGS, for an outgoing call made inside this span. */
export function formatTraceparent(context: SpanContext): string {
  throw new Error("todo: write formatTraceparent()");
}
