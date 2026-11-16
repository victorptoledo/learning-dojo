---
from: java
status: draft
concept: "The browser as a client runtime"
from_way: "Server-side rendering with Spring MVC or JSF: the server builds HTML and the browser only displays it."
native_way: "The browser downloads HTML, CSS and JavaScript and runs the UI itself; your code runs on the user's device, with their CPU, network and settings."
sensei_tip: "Think of the browser as a JVM you do not control: you cannot pick its version, its memory or its network, so test on slow devices and keep what you ship small."
---

| Java | In the browser |
| --- | --- |
| Servlet request/response | The HTTP request and response seen in the Network panel |
| Thymeleaf template | The HTML document |
| JAR on the classpath | A script loaded with `<script type="module" src>` |
| `System.out.println` | `console.log` in the Console panel |
