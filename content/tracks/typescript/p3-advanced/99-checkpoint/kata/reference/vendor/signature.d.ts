// Types for the untyped vendor module next to this file (signature.js).

/** Signs a request body: "sha256=" followed by the HMAC-SHA256 of the body, in hex. */
export declare function sign(secret: string, body: string): string;

/** True when the signature matches the body, compared in constant time. */
export declare function verify(secret: string, body: string, signature: string): boolean;
