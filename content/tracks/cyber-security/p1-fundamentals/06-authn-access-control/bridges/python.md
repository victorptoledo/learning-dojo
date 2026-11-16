---
from: python
status: draft
concept: "Who you are, whether it is still you, and what you may do"
from_way: "Flask apps with a signed cookie holding user_id forever, and role checks spread over views."
native_way: "Framework sessions with Secure, HttpOnly and SameSite set, rotation at login, CSRF protection, and a permission layer."
sensei_tip: "Django rotates the session key in `login()` and has CSRF middleware on by default; in Flask set `SESSION_COOKIE_SECURE`, `SESSION_COOKIE_SAMESITE` and use Flask-WTF or an equivalent for CSRF."
---

| Python | Secure practice |
| --- | --- |
| session['user_id'] that never expires | PERMANENT_SESSION_LIFETIME and idle timeouts |
| @csrf_exempt on forms | CSRF tokens on every state change |
| random.random() session ids | secrets.token_urlsafe(32) |
| if user.role == 'admin' everywhere | one permission layer |
| Model(**request.json) | a schema with allowed fields |
