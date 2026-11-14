# Spring Boot: What It Does for You

A real Java service needs dozens of objects wired together: a web server, controllers, a
connection pool, repositories, a JSON mapper, security filters, configuration. Creating and
connecting them by hand is a lot of code that has nothing to do with your business.
**Spring** does that wiring; **Spring Boot** decides sensible defaults so you rarely have to.

This track teaches **Spring Boot 4** (Spring Framework 7) on **Java 25**. It assumes the
Modern Java track: records, lambdas, `Optional`, streams and JUnit.

## Three ideas behind everything

### 1. The container (inversion of control)

You do not call `new OrderService(new OrderRepository(dataSource), mailer)`. You **declare**
the pieces, and Spring's **application context** creates them, in the right order, and gives
each one what it needs. Objects managed by the container are called **beans**.

```java
@Service
class OrderService {
    private final OrderRepository orders;
    private final Mailer mailer;

    OrderService(OrderRepository orders, Mailer mailer) {   // Spring passes the beans in
        this.orders = orders;
        this.mailer = mailer;
    }
}
```

### 2. Starters

A **starter** is one dependency that brings a coherent set of libraries with compatible
versions:

| Starter | Brings |
| --- | --- |
| `spring-boot-starter-webmvc` | Spring MVC, an embedded Tomcat, Jackson for JSON |
| `spring-boot-starter-validation` | Jakarta Bean Validation (Hibernate Validator) |
| `spring-boot-starter-data-jpa` | Spring Data JPA, Hibernate, a connection pool |
| `spring-boot-starter-security` | Spring Security |
| `spring-boot-starter-actuator` | health, metrics and management endpoints |
| `spring-boot-starter-*-test` | the test support for each of them (MockMvc, test slices...) |

Spring Boot 4 split the old all-in-one modules into smaller ones, so each technology now has
its own starter **and** its own test starter. The versions come from the
`spring-boot-starter-parent` (or the `spring-boot-dependencies` BOM): you never write a
version for a Spring library yourself.

### 3. Auto-configuration

When the application starts, Boot looks at the **classpath** and the **configuration** and
creates the beans you would almost always want. Jackson on the classpath: a JSON mapper.
H2 and Data JPA on the classpath and no database URL: an in-memory database. Spring Security
on the classpath: every endpoint requires a login. Each auto-configuration backs off as soon as
you declare your own bean of that type.

> [!TIP]
> Start the app with `--debug` (or `debug=true` in the properties) to print the
> **condition evaluation report**: which auto-configurations applied and why the others did
> not.

## Anatomy of a Boot project

Generate one at [start.spring.io](https://start.spring.io) (Maven, Java 25, Spring Boot 4.x,
dependency "Spring Web"), or from VS Code with the Spring Boot Extension Pack.

```text
hello/
├── pom.xml                                  parent spring-boot-starter-parent, the starters
└── src/
    ├── main/java/dojo/
    │   ├── DojoApplication.java             main class
    │   └── GreetingController.java          your code, in the same package or below
    ├── main/resources/
    │   └── application.properties           configuration
    └── test/java/dojo/
        └── GreetingControllerTest.java
```

```java
@SpringBootApplication
public class DojoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DojoApplication.class, args);
    }
}
```

`@SpringBootApplication` turns on three things: configuration in this class, component
**scanning** of this package and every package below it, and auto-configuration. Put it in
the root package; classes outside that package tree are not found.

## A first endpoint

```java
@RestController
class GreetingController {
    @GetMapping("/greet")
    String greet(@RequestParam(defaultValue = "stranger") String name) {
        return "Welcome to the dojo, " + name + "!";
    }
}
```

Run it with `mvn spring-boot:run` (or the Run button in the IDE) and open
`http://localhost:8080/greet?name=Ada`. `@RestController` says the return value **is** the
response body; `@GetMapping` maps `GET /greet` to the method; `@RequestParam` reads `?name=`.

## How the dojo tests a Spring kata

Spring katas are tested without starting a real server, with **MockMvc**: it sends fake HTTP
requests straight into Spring MVC and lets the test check status, headers and body.

```java
@SpringBootTest
@AutoConfigureMockMvc
class GreetingControllerTest {
    @Autowired MockMvc mvc;

    @Test
    void greets_by_name() throws Exception {
        mvc.perform(get("/greet").param("name", "Ada"))
                .andExpect(status().isOk())
                .andExpect(content().string("Welcome to the dojo, Ada!"));
    }
}
```

`@SpringBootTest` starts the whole application context (in a few seconds), and
`@AutoConfigureMockMvc` adds a `MockMvc` bean. The dojo runs these tests in Docker, offline;
the libraries it loads follow the starters in your kata's `pom.xml`.

## Summary

- Spring's container creates and wires **beans**; you declare them and ask for them in
  constructors.
- **Starters** bring compatible libraries; the parent POM manages every version.
- **Auto-configuration** creates sensible beans from what is on the classpath, and backs off
  when you define your own.
- `@SpringBootApplication` in the root package; controllers in the same package or below.
- MockMvc tests the web layer without a real server.
