// What the old web and mobile clients read from GET /api/users/:id: the consumer contract
// that every release must keep until the last old client is gone.

/** The broken expectations, one message per field, each starting with the field name; empty when the body is fine. */
export function checkUserForOldClients(body: unknown): string[] {
  throw new Error(`todo: check ${JSON.stringify(body)} against what old clients read`);
}
