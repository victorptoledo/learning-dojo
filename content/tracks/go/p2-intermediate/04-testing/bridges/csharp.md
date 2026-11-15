---
from: csharp
status: draft
concept: "xUnit tests and benchmarks"
from_way: "xUnit or NUnit with [Fact] and [Theory] plus [InlineData], Assert.Equal, Moq for mocks, FluentAssertions, BenchmarkDotNet and coverlet."
native_way: "The testing package, table-driven subtests, plain comparisons, fakes through small interfaces, and benchmarks, fuzzing and coverage in go test."
sensei_tip: "[Theory] with [InlineData] is a table-driven test; in Go the table is a slice of structs and each row gets a name you can run alone."
---

| xUnit / .NET | Go |
| --- | --- |
| `[Fact] public void Discount()` | `func TestDiscount(t *testing.T)` |
| `Assert.Equal(900, d)` | `if d != 900 { t.Errorf(...) }` |
| `[Theory]` + `[InlineData(...)]` | a table and `t.Run` |
| `IDisposable` test class | `t.Cleanup` |
| Moq `new Mock<IRepo>()` | a hand-written fake |
| BenchmarkDotNet `[Benchmark]` | `BenchmarkX` with `b.Loop()` and `-benchmem` |
| SharpFuzz | `FuzzX` and `go test -fuzz` |
| coverlet | `go test -coverprofile` |
