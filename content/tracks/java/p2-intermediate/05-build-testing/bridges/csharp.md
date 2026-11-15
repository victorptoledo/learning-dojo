---
from: csharp
status: draft
concept: "Builds and unit tests"
from_way: "xUnit or NUnit with [Fact], [Theory] and [InlineData], and dotnet test."
native_way: "JUnit 6 with @Test, @ParameterizedTest and @CsvSource, run by Maven."
sensei_tip: "[Theory] with [InlineData] is @ParameterizedTest with @CsvSource; Assert.Throws is assertThrows. Maven's Surefire plugin is what dotnet test does."
---

| C# | Java 25 |
| --- | --- |
| dotnet test | mvn test |
| [Fact] | @Test |
| [Theory] [InlineData] | @ParameterizedTest @CsvSource |
| Assert.Equal(expected, actual) | assertEquals(expected, actual) |
| Assert.Throws<T> | assertThrows(T.class, ...) |
