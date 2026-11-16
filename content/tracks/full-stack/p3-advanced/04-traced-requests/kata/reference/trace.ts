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

const TRACEPARENT = /^([0-9a-f]{2})-([0-9a-f]{32})-([0-9a-f]{16})-([0-9a-f]{2})$/;

/** The trace of a traceparent header, or undefined when it is missing or invalid. */
export function parseTraceparent(header: string | undefined): IncomingTrace | undefined {
  const match = header === undefined ? null : TRACEPARENT.exec(header.trim());
  if (!match) {
    return undefined;
  }
  const [, version, traceId, parentId, flags] = match;
  if (version === "ff" || /^0+$/.test(traceId ?? "") || /^0+$/.test(parentId ?? "")) {
    return undefined;
  }
  return { traceId: traceId ?? "", parentId: parentId ?? "", flags: flags ?? "" };
}

/** 00-TRACEID-SPANID-FLAGS, for an outgoing call made inside this span. */
export function formatTraceparent(context: SpanContext): string {
  return `00-${context.traceId}-${context.spanId}-${context.flags}`;
}
