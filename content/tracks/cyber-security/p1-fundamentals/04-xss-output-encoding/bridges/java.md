---
from: java
status: draft
concept: "Encode for the context the data lands in"
from_way: "JSP scriptlets printing request parameters, or th:utext for convenience."
native_way: "Thymeleaf's th:text (escaped by default), OWASP Java Encoder for other contexts, and OWASP Java HTML Sanitizer for rich text."
sensei_tip: "`Encode.forHtmlAttribute`, `Encode.forJavaScript` and `Encode.forUriComponent` from the OWASP Java Encoder name the context in the method, which makes reviews easy."
---

| Java | XSS-safe way |
| --- | --- |
| <%= request.getParameter("q") %> | th:text or Encode.forHtml |
| th:utext="${bio}" | sanitise with a PolicyFactory first |
| a href from user input | check the scheme with java.net.URI |
| Pattern.matches with ^...$ | matches() is already a full match |
| no CSP | a strict CSP header via Spring Security |
