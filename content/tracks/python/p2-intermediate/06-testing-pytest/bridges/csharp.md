---
from: csharp
status: draft
concept: "xUnit and Moq in pytest"
from_way: "xUnit [Fact] and [Theory] with [InlineData], Assert.Equal and Assert.Throws, constructor setup with IDisposable teardown, class fixtures, and Moq's Setup and Verify."
native_way: "test_ functions with assert, @pytest.mark.parametrize for theories, fixtures with scopes for setup and teardown, and Mock with return_value and assert_called_with."
sensei_tip: "xUnit class and collection fixtures are pytest fixtures with scope=\"module\" or \"session\". The same advice applies: share only expensive, read-only things."
---

| xUnit / Moq | pytest |
| --- | --- |
| `[Fact]` | `def test_...():` |
| `[Theory, InlineData(1, 2)]` | `@pytest.mark.parametrize("a, b", [(1, 2)])` |
| `Assert.Equal(expected, actual)` | `assert actual == expected` |
| `Assert.Throws<ArgumentException>(...)` | `with pytest.raises(ValueError):` |
| constructor + `Dispose` | fixture with `yield` |
| `IClassFixture<DbFixture>` | `@pytest.fixture(scope="module")` |
| `mock.Setup(r => r.Get("USD")).Returns(x)` | `rates.get.return_value = x` |
| `mock.Verify(m => m.Send(...), Times.Once)` | `mail.send.assert_called_once_with(...)` |
