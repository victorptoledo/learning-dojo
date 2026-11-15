---
from: java
status: draft
concept: "JUnit, AssertJ and Mockito in pytest"
from_way: "Test classes with @Test methods, assertEquals or AssertJ chains, @BeforeEach and @AfterEach, @ParameterizedTest with @CsvSource, and Mockito's mock, when and verify."
native_way: "Plain test functions with assert, fixtures injected by parameter name with yield teardown, @pytest.mark.parametrize, and unittest.mock or hand-written fakes."
sensei_tip: "Drop the class and the assertion library: a bare assert with pytest's rewriting prints more than AssertJ. Fixtures replace @BeforeEach and also compose, which setup methods cannot."
---

| JUnit 6 / Mockito | pytest |
| --- | --- |
| `@Test void addsVat()` | `def test_adds_vat():` |
| `assertEquals(expected, actual)` | `assert actual == expected` |
| `assertThrows(X.class, () -> ...)` | `with pytest.raises(X):` |
| `@BeforeEach` / `@AfterEach` | a fixture with `yield` |
| `@ParameterizedTest @CsvSource({...})` | `@pytest.mark.parametrize(...)` |
| `@TempDir Path dir` | `tmp_path` |
| `when(rates.get("USD")).thenReturn(x)` | `rates.get.return_value = x` |
| `verify(mail).send(any())` | `mail.send.assert_called_once()` |
