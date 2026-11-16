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
  void createHash;
  throw new Error("todo: write etagOf()");
}

/** Does an If-None-Match / If-Match value match this ETag? ("*", lists, weak W/ prefixes.) */
export function matches(header: string | undefined, etag: string): boolean {
  throw new Error("todo: write matches()");
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

/** GET and PUT /api/products/:id with validators (see README.md). */
export function createProductServer(catalog: Catalog): Server {
  return createServer((request, response) => {
    void catalog;
    send(response, 501, JSON.stringify({ error: `todo: ${request.method} ${request.url} is not written yet` }));
  });
}

if (import.meta.main) {
  const catalog = new Catalog([{ id: 1, name: "Mug", priceCents: 1200 }]);
  createProductServer(catalog).listen(3000, () => console.log("http://localhost:3000/api/products/1"));
}
