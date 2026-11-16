---
from: java
status: draft
concept: "Declare the state, let a tool converge to it"
from_way: "A deploy guide tells someone to install a JDK, edit server.xml and restart Tomcat on each server."
native_way: "A playbook declares the JDK package, the templated server.xml and the running service; a handler restarts Tomcat only when the file changed."
sensei_tip: "If the JVM's settings live in a template, review JVM flag changes in pull requests like any other code."
---

| Java world | Configuration management |
| --- | --- |
| a deploy guide | a playbook |
| `server.xml` edited by hand | a Jinja2 template |
| restart Tomcat after changes | a handler notified on change |
| a Maven profile per environment | group_vars per environment |
