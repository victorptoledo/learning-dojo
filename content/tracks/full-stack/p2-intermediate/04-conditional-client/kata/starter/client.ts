// Validators on the client: revalidate with If-None-Match, update with If-Match.

/** Someone else changed the resource first; `current` is the server's version now. */
export class ConflictError extends Error {
  override name = "ConflictError";

  readonly current: unknown;

  constructor(current: unknown) {
    super("the resource changed since you loaded it");
    this.current = current;
  }
}

interface Cached {
  etag: string;
  body: unknown;
}

export class CachingClient {
  readonly stats = { downloaded: 0, revalidated: 0 };
  private readonly cache = new Map<string, Cached>();

  private readonly baseUrl: string;
  private readonly fetchFn: typeof fetch;

  constructor(baseUrl: string, fetchFn: typeof fetch = fetch) {
    this.baseUrl = baseUrl;
    this.fetchFn = fetchFn;
  }

  /** GET with If-None-Match; a 304 answers from the cache. */
  async get<T>(path: string): Promise<T> {
    void this.cache;
    void this.baseUrl;
    void this.fetchFn;
    throw new Error("todo: write get()");
  }

  /** PUT with If-Match; a 412 refreshes the cache and throws a ConflictError. */
  async update<T>(path: string, changes: object): Promise<T> {
    throw new Error("todo: write update()");
  }
}
