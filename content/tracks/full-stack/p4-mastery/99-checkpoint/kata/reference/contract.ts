// What the old web and mobile clients read from GET /api/users/:id: the consumer contract
// that every release must keep until the last old client is gone.

/** The broken expectations, one message per field, each starting with the field name; empty when the body is fine. */
export function checkUserForOldClients(body: unknown): string[] {
  if (typeof body !== "object" || body === null || Array.isArray(body)) return ["body: must be a JSON object"];
  const user = body as Record<string, unknown>;
  const problems: string[] = [];
  if (typeof user["id"] !== "number" || !Number.isInteger(user["id"])) problems.push("id: must be a whole number");
  if (typeof user["fullName"] !== "string" || user["fullName"].trim() === "") {
    problems.push("fullName: must be a non-empty string");
  }
  if (typeof user["email"] !== "string" || !user["email"].includes("@")) problems.push("email: must be an email address");
  // Extra fields are fine: old clients ignore what they do not know (tolerant readers).
  return problems;
}
