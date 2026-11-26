// A small vendor module written in plain JavaScript, with no types. Do not change this file:
// describe it in signature.d.ts instead.
import { createHmac, timingSafeEqual } from "node:crypto";

/** Signs a request body: "sha256=" followed by the HMAC-SHA256 of the body, in hex. */
export function sign(secret, body) {
  return "sha256=" + createHmac("sha256", secret).update(body).digest("hex");
}

/** True when the signature matches the body, compared in constant time. */
export function verify(secret, body, signature) {
  const expected = Buffer.from(sign(secret, body));
  const given = Buffer.from(String(signature));
  return expected.length === given.length && timingSafeEqual(expected, given);
}
