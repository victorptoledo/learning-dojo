# Authentication Across the Stack: Cookies, CSRF, CORS and OIDC

The Backend Developer track explains password hashing, sessions, JWTs and OAuth2 from the
server's point of view. In a full stack app the hard part is the **browser**: where the
credential lives, which requests carry it automatically, and which pages can steal or abuse it.

## Where does the browser keep the credential?

| Option | Readable by JavaScript? | Sent automatically? | Main risk |
| --- | --- | --- | --- |
| Session id in a `Secure; HttpOnly; SameSite=Lax` cookie | no | yes, to its site | CSRF (other sites trigger requests that carry it) |
| Access token in `localStorage` | yes | no, the code adds `Authorization` | XSS: any injected script can copy it and use it elsewhere |
| Access token in memory only | yes, while the page runs | no | XSS can still use it; lost on reload |

An XSS bug with a token in `localStorage` lets the attacker **take the token away** and use it
from their own machine until it expires. With an `HttpOnly` cookie, XSS can still act inside the
page while it is open, but it cannot exfiltrate the credential. That is why current guidance
(the OAuth 2.0 for Browser-Based Apps best current practice) prefers keeping tokens out of
JavaScript.

## CSRF: the price of automatic cookies

A page on `evil.example` can submit a form to `https://shop.example/account/email`. If the
browser attaches your session cookie, the request runs as you. Defences, best in layers:

1. **`SameSite=Lax` or `Strict` cookies**: browsers do not attach them to cross-site POSTs. It
   is a strong default but does not cover subdomains of the same site or old browsers.
2. **CSRF tokens**: the server gives the page a secret value (in the HTML of a form, or in a
   cookie the SPA reads) and requires it back on every state-changing request, in a form field
   or a header like `X-XSRF-TOKEN`. Another site cannot read it, so it cannot send it. Spring
   Security, ASP.NET Core, Django and Rails do this for you.
3. **Origin checks**: refuse state-changing requests whose `Origin` (or `Sec-Fetch-Site`) is not
   yours.
4. **Never change state on `GET`.**

Login forms need CSRF protection too: *login CSRF* signs the victim in to the attacker's account.

## CORS with sessions

When the SPA and the API are on different origins, the SPA calls
`fetch(url, { credentials: "include" })` and the API answers with the **exact** origin and
`Access-Control-Allow-Credentials: true`. The cookie must then be same-*site* (same registrable
domain, such as `app.example.com` and `api.example.com`) to keep `SameSite=Lax`, or
`SameSite=None; Secure` if it is truly cross-site, which brings CSRF back to the front. Serving the
API under the app's origin (`/api` through a proxy) avoids all of this.

## Logging in with an identity provider

Most products delegate login to an **OpenID Connect** provider (Keycloak, Auth0, Entra ID,
Cognito, Google). The flow for browsers is the **authorization code flow with PKCE**:

```text
1. App → browser: redirect to the provider with client_id, redirect_uri, scope=openid,
   state (against CSRF) and code_challenge (PKCE).
2. User logs in at the provider (passwords, MFA and passkeys live there).
3. Provider → browser: redirect back with ?code=...&state=...
4. Code + code_verifier → provider's token endpoint → ID token, access token, refresh token.
```

The **backend-for-frontend (BFF)** pattern does step 4 on your server: the server keeps the
tokens, gives the browser only an `HttpOnly` session cookie, and calls APIs with the access token
on the user's behalf. The SPA never sees a token; it just calls its own backend with cookies and
a CSRF header. Spring Security (`oauth2Login`), ASP.NET Core and Next.js auth libraries support it.

## Expiry and logout

- Keep sessions and access tokens **short-lived**, with an idle timeout and an absolute one.
- **Logout** must invalidate the server-side session (and revoke the refresh token at the
  provider), not just delete a cookie in the browser.
- When the API answers **`401`** to an SPA, send the user to log in (and come back to where they
  were); never answer an API call with a redirect to an HTML login page, which `fetch` cannot use.

## Authorization: shown in the UI, enforced on the server

The UI hides the delete button from users who may not delete; that is **user experience, not
security**. The server checks every request against the user and the object (owner, role,
tenant). A pattern that works well: the API returns flags such as `canDelete` computed by the same
code that enforces the rule, so UI and server never disagree. You will build exactly that in the
second checkpoint.

## Summary

- Prefer `HttpOnly`, `Secure`, `SameSite` session cookies over tokens readable by JavaScript.
- Cookies need CSRF defences: SameSite, tokens in a header or form, origin checks, safe GETs.
- With an identity provider, use the authorization code flow with PKCE; a BFF keeps tokens on
  the server.
- Short lifetimes, real server-side logout, and a JSON 401 for API calls.
- The UI shows permissions; the server enforces them.

## Further reading

- OWASP, "Cross-Site Request Forgery Prevention Cheat Sheet": <https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html>
- IETF, "OAuth 2.0 for Browser-Based Applications": <https://datatracker.ietf.org/doc/draft-ietf-oauth-browser-based-apps/>
- RFC 7636, "Proof Key for Code Exchange (PKCE)": <https://www.rfc-editor.org/rfc/rfc7636>
- Spring Security reference, "Cross Site Request Forgery (CSRF)": <https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html>
- OWASP, "Session Management Cheat Sheet": <https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html>
