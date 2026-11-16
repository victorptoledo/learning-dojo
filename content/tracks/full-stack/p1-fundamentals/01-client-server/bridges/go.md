---
from: go
status: reviewed
concept: "Setting CORS and cookie headers by hand"
from_way: "w.Header().Set(\"Access-Control-Allow-Origin\", \"*\") in a middleware copied from a blog post."
native_way: "Echo only allowed origins, add Vary: Origin, answer OPTIONS preflights, and build http.Cookie with Secure, HttpOnly and SameSite."
sensei_tip: "http.Cookie has SameSite, Secure and HttpOnly fields; set them explicitly, because the zero values mean no protection."
---

| Go | Browser effect |
| --- | --- |
| `http.SetCookie(w, &http.Cookie{HttpOnly: true, Secure: true, SameSite: http.SameSiteLaxMode})` | a safe session cookie |
| `w.Header().Add("Vary", "Origin")` | caches keep per-origin answers apart |
| `if r.Method == http.MethodOptions` | the preflight answer |
| `http.Redirect(w, r, "/", http.StatusSeeOther)` | Post/Redirect/Get |
