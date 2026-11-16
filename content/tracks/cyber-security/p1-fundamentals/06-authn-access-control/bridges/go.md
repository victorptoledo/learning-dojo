---
from: go
status: draft
concept: "Who you are, whether it is still you, and what you may do"
from_way: "A cookie holding the user id, set without attributes, and middleware checking only that it exists."
native_way: "Random session ids from crypto/rand, http.Cookie with Secure, HttpOnly and SameSite, rotation at login, and a policy function per action."
sensei_tip: "Go 1.25 added `http.CrossOriginProtection`, which rejects cross-origin state-changing requests using `Sec-Fetch-Site` and `Origin`: an easy CSRF layer for net/http servers."
---

| Go | Secure practice |
| --- | --- |
| cookie value = user id | a random id mapped to server state |
| http.Cookie{Name, Value} | add Secure, HttpOnly, SameSite |
| math/rand for tokens | crypto/rand or rand.Text() |
| no CSRF defence | http.CrossOriginProtection or tokens |
| json.Decode into the User model | decode into a request struct |
