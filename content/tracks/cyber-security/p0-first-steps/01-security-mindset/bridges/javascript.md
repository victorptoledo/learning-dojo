---
from: javascript
status: draft
concept: "Thinking in threats, not only in features"
from_way: "Checking permissions in the React component and assuming the API is safe because the button is hidden."
native_way: "Every control the client shows is enforced again on the server; the browser is attacker-controlled."
sensei_tip: "Anything in the bundle is public: API keys in front-end code, feature flags and hidden admin routes are all readable with the browser's developer tools."
---

| JavaScript / TypeScript | Security mindset |
| --- | --- |
| hiding the delete button | authorise the DELETE on the server |
| secrets bundled into the front end | only public values in the client |
| npm install without review | every dependency is attack surface |
| catch {} around a token check | fail closed |
| CORS * to make it work | allow only the origins you serve |
