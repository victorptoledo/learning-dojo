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

interface Series {
  labels: string;
  /** Not cumulative: counts[i] is the number of observations in bucket i (the last is +Inf). */
  counts: number[];
  sum: number;
  count: number;
}

function labelText(labels: RequestLabels): string {
  const escape = (value: string) => value.replace(/\\/g, "\\\\").replace(/"/g, '\\"').replace(/\n/g, "\\n");
  return `method="${escape(labels.method)}",route="${escape(labels.route)}",status="${escape(labels.status)}"`;
}

export class LatencyHistogram {
  private readonly series = new Map<string, Series>();

  /** Records one request that took `seconds`. */
  observe(labels: RequestLabels, seconds: number): void {
    const key = labelText(labels);
    let series = this.series.get(key);
    if (series === undefined) {
      series = { labels: key, counts: new Array<number>(BUCKETS.length + 1).fill(0), sum: 0, count: 0 };
      this.series.set(key, series);
    }
    const index = BUCKETS.findIndex((bound) => seconds <= bound);
    const slot = index === -1 ? BUCKETS.length : index;
    series.counts[slot] = (series.counts[slot] ?? 0) + 1;
    series.sum += seconds;
    series.count += 1;
  }

  /** The Prometheus text format (see README.md). */
  render(): string {
    const lines = [`# HELP ${METRIC} Duration of HTTP server requests.`, `# TYPE ${METRIC} histogram`];
    for (const series of this.series.values()) {
      let cumulative = 0;
      BUCKETS.forEach((bound, i) => {
        cumulative += series.counts[i] ?? 0;
        lines.push(`${METRIC}_bucket{${series.labels},le="${String(bound)}"} ${cumulative}`);
      });
      lines.push(`${METRIC}_bucket{${series.labels},le="+Inf"} ${series.count}`);
      lines.push(`${METRIC}_sum{${series.labels}} ${String(series.sum)}`);
      lines.push(`${METRIC}_count{${series.labels}} ${series.count}`);
    }
    return `${lines.join("\n")}\n`;
  }
}
