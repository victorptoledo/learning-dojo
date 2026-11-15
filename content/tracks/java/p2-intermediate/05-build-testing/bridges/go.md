---
from: go
status: draft
concept: "Builds and unit tests"
from_way: "go test with table-driven tests and t.Run subtests."
native_way: "JUnit 6 with parameterized tests and descriptive names."
sensei_tip: "A Go table-driven test is a @ParameterizedTest with @MethodSource. Java's assertions make the expected value the first argument, the opposite of many Go helpers."
---

| Go | Java 25 |
| --- | --- |
| go test ./... | mvn test |
| func TestX(t *testing.T) | @Test void x() |
| table of cases + t.Run | @ParameterizedTest @MethodSource |
| t.Errorf("got %v want %v") | assertEquals(want, got) |
| testdata/ | src/test/resources |
