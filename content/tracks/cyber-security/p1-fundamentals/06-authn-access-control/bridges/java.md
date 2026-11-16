---
from: java
status: draft
concept: "Who you are, whether it is still you, and what you may do"
from_way: "Hand-rolled login controllers and roles checked with if statements in services."
native_way: "Spring Security's form or OAuth login (session fixation protection and CSRF on by default), method security, and a central authorisation layer."
sensei_tip: "Spring Security already rotates the session at login (`sessionFixation().changeSessionId()`) and enables CSRF tokens for browser flows; most bugs come from turning those off."
---

| Java | Secure practice |
| --- | --- |
| csrf().disable() for a browser app | keep CSRF on, or SameSite plus tokens |
| same session before and after login | changeSessionId (the default) |
| JSESSIONID without Secure | server.servlet.session.cookie.secure=true |
| if (user.isAdmin()) in services | @PreAuthorize policies |
| @RequestBody User user | a DTO with editable fields only |
