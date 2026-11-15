---
from: python
status: draft
concept: "From a web framework to Spring Boot 4"
from_way: "Django gives you a project generator and batteries included; FastAPI a small app you assemble yourself."
native_way: "start.spring.io generates the project, starters bring dependencies with matching versions, and auto-configuration sets up what it finds."
sensei_tip: "Spring Boot sits between the two: as complete as Django, but you add pieces as starters, like FastAPI plus chosen libraries, and auto-configuration wires them."
---

| Python | Spring Boot 4 |
| --- | --- |
| django-admin startproject | start.spring.io |
| pip install fastapi[standard] | spring-boot-starter-webmvc |
| settings.py | application.properties |
| uvicorn main:app --reload | mvn spring-boot:run (with devtools) |
| INSTALLED_APPS | starters plus auto-configuration |
