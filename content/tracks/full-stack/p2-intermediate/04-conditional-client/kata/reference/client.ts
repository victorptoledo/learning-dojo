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

  private remember(path: string, response: Response, body: unknown): void {
    const etag = response.headers.get("etag");
    if (etag !== null) {
      this.cache.set(path, { etag, body });
    }
  }

  /** GET with If-None-Match; a 304 answers from the cache. */
  async get<T>(path: string): Promise<T> {
    const cached = this.cache.get(path);
    const response = await this.fetchFn(`${this.baseUrl}${path}`, {
      headers: cached ? { "If-None-Match": cached.etag } : {},
    });
    if (response.status === 304 && cached) {
      this.stats.revalidated += 1;
      return cached.body as T;
    }
    if (!response.ok) {
      throw new Error(`GET ${path} failed with ${response.status}`);
    }
    const body: unknown = await response.json();
    this.stats.downloaded += 1;
    this.remember(path, response, body);
    return body as T;
  }

  /** PUT with If-Match; a 412 refreshes the cache and throws a ConflictError. */
  async update<T>(path: string, changes: object): Promise<T> {
    const cached = this.cache.get(path);
    if (!cached) {
      throw new Error(`load ${path} with get() before updating it`);
    }
    const response = await this.fetchFn(`${this.baseUrl}${path}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json", "If-Match": cached.etag },
      body: JSON.stringify(changes),
    });
    if (response.status === 412) {
      const current: unknown = await response.json();
      this.remember(path, response, current);
      throw new ConflictError(current);
    }
    if (!response.ok) {
      throw new Error(`PUT ${path} failed with ${response.status}`);
    }
    const body: unknown = await response.json();
    this.remember(path, response, body);
    return body as T;
  }
}
