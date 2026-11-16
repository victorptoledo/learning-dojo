// Validators on the server: ETag, If-None-Match (304) and If-Match (412, 428).
import { createHash } from "node:crypto";
import { createServer, type IncomingMessage, type Server, type ServerResponse } from "node:http";

export interface Product {
  id: number;
  name: string;
  priceCents: number;
}

/** The products, in memory. */
export class Catalog {
  private readonly products = new Map<number, Product>();

  constructor(products: readonly Product[] = []) {
    for (const product of products) {
      this.products.set(product.id, product);
    }
  }

  get(id: number): Product | undefined {
    return this.products.get(id);
  }

  /** Applies the known fields of changes and returns the updated product. */
  update(id: number, changes: Partial<Pick<Product, "name" | "priceCents">>): Product | undefined {
    const current = this.products.get(id);
    if (current === undefined) {
      return undefined;
    }
    const next: Product = {
      ...current,
      ...(typeof changes.name === "string" ? { name: changes.name } : {}),
      ...(typeof changes.priceCents === "number" ? { priceCents: changes.priceCents } : {}),
    };
    this.products.set(id, next);
    return next;
  }
}

/** A strong ETag: '"' + the first 16 characters of the base64url SHA-256 of the body + '"'. */
export function etagOf(body: string): string {
  return `"${createHash("sha256").update(body).digest("base64url").slice(0, 16)}"`;
}

function opaque(tag: string): string {
  const trimmed = tag.trim();
  return trimmed.startsWith("W/") ? trimmed.slice(2) : trimmed;
}

/** Does an If-None-Match / If-Match value match this ETag? ("*", lists, weak W/ prefixes.) */
export function matches(header: string | undefined, etag: string): boolean {
  if (header === undefined) {
    return false;
  }
  if (header.trim() === "*") {
    return true;
  }
  return header.split(",").some((tag) => opaque(tag) === opaque(etag));
}

export async function readBody(request: IncomingMessage): Promise<string> {
  const chunks: Buffer[] = [];
  for await (const chunk of request) {
    chunks.push(chunk as Buffer);
  }
  return Buffer.concat(chunks).toString("utf8");
}

export function send(response: ServerResponse, status: number, body: string | undefined, headers: Record<string, string> = {}): void {
  response.writeHead(status, body === undefined ? headers : { "Content-Type": "application/json", ...headers });
  response.end(body);
}

const PRODUCT = /^\/api\/products\/(\d+)$/;

async function route(catalog: Catalog, request: IncomingMessage, response: ServerResponse): Promise<void> {
  const match = PRODUCT.exec(new URL(request.url ?? "/", "http://localhost").pathname);
  const product = match ? catalog.get(Number(match[1])) : undefined;
  if (product === undefined) {
    send(response, 404, JSON.stringify({ error: "not found" }));
    return;
  }
  const body = JSON.stringify(product);
  const etag = etagOf(body);
  if (request.method === "GET") {
    if (matches(request.headers["if-none-match"], etag)) {
      send(response, 304, undefined, { ETag: etag, "Cache-Control": "no-cache" });
    } else {
      send(response, 200, body, { ETag: etag, "Cache-Control": "no-cache" });
    }
    return;
  }
  if (request.method === "PUT") {
    const ifMatch = request.headers["if-match"];
    if (ifMatch === undefined) {
      send(response, 428, JSON.stringify({ error: "send If-Match with the ETag you edited" }));
      return;
    }
    if (!matches(ifMatch, etag)) {
      send(response, 412, body, { ETag: etag });
      return;
    }
    let changes: unknown;
    try {
      changes = JSON.parse(await readBody(request));
    } catch {
      send(response, 400, JSON.stringify({ error: "the body is not valid JSON" }));
      return;
    }
    const updated = catalog.update(product.id, (changes ?? {}) as Partial<Product>) ?? product;
    const updatedBody = JSON.stringify(updated);
    send(response, 200, updatedBody, { ETag: etagOf(updatedBody) });
    return;
  }
  send(response, 405, JSON.stringify({ error: "method not allowed" }), { Allow: "GET, PUT" });
}

/** GET and PUT /api/products/:id with validators (see README.md). */
export function createProductServer(catalog: Catalog): Server {
  return createServer((request, response) => {
    route(catalog, request, response).catch(() => send(response, 500, JSON.stringify({ error: "internal error" })));
  });
}

if (import.meta.main) {
  const catalog = new Catalog([{ id: 1, name: "Mug", priceCents: 1200 }]);
  createProductServer(catalog).listen(3000, () => console.log("http://localhost:3000/api/products/1"));
}
