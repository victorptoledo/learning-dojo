// A Prometheus latency histogram, written by hand to see what a metrics library does.

export const METRIC = "http_server_request_duration_seconds";

/** Upper bounds in seconds; +Inf is added when rendering. */
export const BUCKETS = [0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2.5, 5] as const;

export interface RequestLabels {
  method: string;
  /** The route template, such as /api/orders/:id: never the raw path, or every id is a new series. */
  route: string;
  status: string;
}

export class LatencyHistogram {
  /** Records one request that took `seconds`. */
  observe(labels: RequestLabels, seconds: number): void {
    throw new Error("todo: write observe()");
  }

  /** The Prometheus text format (see README.md). */
  render(): string {
    throw new Error("todo: write render()");
  }
}
