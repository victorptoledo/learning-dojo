# Secure Node.js: Input, Paths, Dependencies and the Permission Model

Node.js applications are attacked like any web application (see the OWASP Top 10), but a few
weaknesses are especially common or especially dangerous in JavaScript: objects that can be
polluted, one event loop that one request can freeze, and a dependency tree of hundreds of
packages. This lesson covers those, and the tools Node.js gives you against them.

## Validate at the boundary

Everything from outside is `unknown`: request bodies, query strings, headers, environment
variables, files, messages, even responses from other services. TypeScript types are erased,
so an annotation proves nothing at runtime. Validate **once, where data enters**, with a schema
(zod, valibot, JSON Schema with ajv, or Fastify's route schemas), and work with typed, trusted
values inside.

- Prefer **allow lists** (known fields, known values) to deny lists.
- Limit **sizes**: body length, string length, array length, nesting depth.
- Reject unknown fields instead of carrying them along.

## Prototype pollution

In JavaScript, `obj.__proto__` is the prototype of `obj`. A naive deep merge of parsed JSON
can write to it, and then **every object** in the process gets the property:

```ts
const payload = JSON.parse('{"__proto__": {"isAdmin": true}}');
deepMerge(settings, payload);      // a careless merge copies into settings.__proto__
({} as { isAdmin?: boolean }).isAdmin;   // true: Object.prototype was modified
```

Defences: validate input with a schema that allows only known keys; never merge untrusted
objects recursively without skipping `__proto__`, `constructor` and `prototype`; use `Map`
or `Object.create(null)` for dictionaries keyed by user input; check own keys with
`Object.hasOwn`. `node --disable-proto=delete` removes the `__proto__` accessor altogether.

## Path traversal

```ts
const file = path.join("/srv/uploads", req.query.name);   // name = "../../etc/passwd"
```

`path.join` happily resolves `..`. Resolve the final path and check that it is still inside
the folder:

```ts
function insideFolder(root: string, name: string): string {
  const full = path.resolve(root, name);
  if (!full.startsWith(path.resolve(root) + path.sep)) {
    throw new HttpError(400, "invalid file name");
  }
  return full;
}
```

Better still, do not use client-supplied names as paths at all: store files under generated
ids and keep the original name in a database.

## Do not let one request block everyone

A single thread serves every request, so CPU-heavy input is a **denial of service**:

- **ReDoS**: a regular expression with nested quantifiers, such as `/^(\w+\s?)*$/`, takes
  exponential time on inputs like `"aaaaaaaaaaaaaaaaaaaaaaaaa!"`. Keep patterns simple, limit
  input length first, and test patterns with a ReDoS checker.
- **Huge JSON** or deeply nested payloads: limit the body size before parsing.
- **Expensive work per request**: rate-limit it (password hashing, report generation).

## Injection

The rule is the same everywhere: **keep data out of code**.

| Sink | Unsafe | Safe |
| --- | --- | --- |
| Shell | `exec(\`convert ${name}\`)` | `execFile("convert", [name])` |
| SQL | `` db.query(`... WHERE id = '${id}'`) `` | `db.query("... WHERE id = $1", [id])` |
| HTML | `` `<p>${comment}</p>` `` | A template engine that escapes, or a sanitiser for HTML input |
| Code | `eval`, `new Function`, `vm` with user input | Never; `node:vm` is not a security sandbox |

## Secrets and crypto

- Read secrets from the environment or a secret manager; never commit them, never log them
  (redact fields such as `authorization` and `password` in your logger).
- Compare secrets and signatures with **`crypto.timingSafeEqual`**, not `===`, which leaks
  how many characters matched through timing.
- Hash passwords with a slow, salted algorithm: `crypto.scrypt` (built in), argon2 or bcrypt
  (packages). Never a plain SHA-256.
- Random tokens come from `crypto.randomBytes` or `crypto.randomUUID()`, never `Math.random()`.

## The supply chain

A typical service has a few dozen direct dependencies and hundreds of transitive ones, each of
which runs with your process's full permissions. Attacks seen in practice: typosquatting
(`expresss`), hijacked maintainer accounts publishing a malicious patch release, and
`postinstall` scripts that steal tokens at install time.

- **Commit the lockfile** and install with `npm ci`, so a new malicious version is not picked up
  silently.
- Run **`npm audit`** (or GitHub's Dependabot, Socket, Snyk) in CI and upgrade deliberately.
- Disable install scripts where you can: `npm ci --ignore-scripts`, or `ignore-scripts=true`
  in `.npmrc`, allowing them only for packages that need them.
- Prefer **fewer, well-maintained dependencies**; Node.js now covers much of what packages
  used to (`fetch`, `parseArgs`, `--env-file`, `node:test`, `fs.glob`).
- Check provenance where available (`npm audit signatures`).

## Hardening the process

- Run as a **non-root user** (in Docker: `USER node`), with a read-only file system where
  possible.
- Keep Node.js on a supported release line and patch it: security releases ship for all LTS
  lines.
- Set HTTP limits (body size, timeouts, header size) and security headers (helmet for
  Express or @fastify/helmet), and do not reveal stack traces or versions in errors.
- Rate-limit authentication and expensive endpoints.

## The permission model

Node.js can restrict what a process may access, since Node.js 23.5 as a stable feature:

```bash
node --permission --allow-fs-read=/srv/app --allow-fs-write=/srv/app/tmp app.js
```

With `--permission`, file system access outside the allowed paths, child processes, worker
threads, native addons and WASI are **denied** unless allowed (`--allow-child-process`,
`--allow-worker`, ...). Code can check with `process.permission.has("fs.write", path)`. It is
a seat belt against a compromised dependency reading `~/.ssh` or spawning a shell, not a
sandbox for running untrusted code.

## Summary

- Validate all external data at the boundary with schemas and limits.
- Watch for JavaScript-specific bugs: prototype pollution, path traversal through `..`, ReDoS.
- Keep data out of shells, SQL, HTML and `eval`.
- `timingSafeEqual` for secrets, `scrypt`/argon2 for passwords, `crypto.randomBytes` for tokens.
- Lockfile + `npm ci` + `npm audit` + fewer dependencies + `--ignore-scripts`.
- Non-root, patched Node.js, HTTP limits, and `--permission` as a second line of defence.

## Further reading

- Node.js, "Security Best Practices": <https://nodejs.org/en/learn/getting-started/security-best-practices>
- Node.js API, "Permissions": <https://nodejs.org/api/permissions.html>
- OWASP, "Node.js Security Cheat Sheet": <https://cheatsheetseries.owasp.org/cheatsheets/Nodejs_Security_Cheat_Sheet.html>
- OWASP, "Prototype Pollution Prevention Cheat Sheet": <https://cheatsheetseries.owasp.org/cheatsheets/Prototype_Pollution_Prevention_Cheat_Sheet.html>
- OWASP Top 10: <https://owasp.org/www-project-top-ten/>
- OWASP, "Regular expression Denial of Service (ReDoS)": <https://owasp.org/www-community/attacks/Regular_expression_Denial_of_Service_-_ReDoS>
- npm Docs, "npm audit": <https://docs.npmjs.com/cli/v11/commands/npm-audit>
- Node.js API, `crypto.timingSafeEqual()`: <https://nodejs.org/api/crypto.html#cryptotimingsafeequala-b>
