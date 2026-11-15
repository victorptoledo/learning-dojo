---
from: python
status: draft
concept: "Builds and unit tests"
from_way: "pytest with plain asserts, fixtures and parametrize, and pip or uv for builds."
native_way: "Maven with the standard layout and JUnit 6 with assertions, @ParameterizedTest and assertThrows."
sensei_tip: "@ParameterizedTest with @CsvSource is pytest.mark.parametrize; assertThrows is pytest.raises. Maven expects tests in src/test/java."
---

| Python | Java 25 |
| --- | --- |
| pytest | mvn test |
| assert a == b | assertEquals(b, a) |
| pytest.raises(ValueError) | assertThrows(IllegalArgumentException.class, ...) |
| @pytest.mark.parametrize | @ParameterizedTest @CsvSource |
| fixtures | @BeforeEach |
