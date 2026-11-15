---
from: go
status: draft
concept: "Identity security"
from_way: "Middleware with golang-jwt, sessions in a store, and x/oauth2."
native_way: "Deliberate choices about sessions, tokens and flows."
sensei_tip: "With golang-jwt, check the signing method in the key function; otherwise a token signed with a different algorithm might be accepted."
---

| Go | In practice |
| --- | --- |
| golang-jwt Parse with keyfunc | verify algorithm and claims |
| gorilla/sessions | server-side or signed sessions |
| x/oauth2 | OAuth2 client flows |
| bcrypt.GenerateFromPassword | slow password hashing |
