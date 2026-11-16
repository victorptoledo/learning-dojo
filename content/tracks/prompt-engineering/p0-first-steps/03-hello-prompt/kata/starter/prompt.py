"""Build prompts from templates and sections, and check them against a token budget."""

from collections.abc import Iterable, Mapping


def estimate_tokens(text: str) -> int:
    """The dojo's token estimate: ceil(len(text) / 4)."""
    raise NotImplementedError("todo: write estimate_tokens()")


def render(template: str, values: Mapping[str, str]) -> str:
    """Replace every {{name}} with values[name]; KeyError if missing, ValueError if unused."""
    raise NotImplementedError("todo: write render()")


def build_prompt(
    task: str,
    *,
    context: str = "",
    rules: Iterable[str] = (),
    output_format: str = "",
) -> str:
    """Assemble <context>, <task>, <rules> and <output_format> sections, in that order."""
    raise NotImplementedError("todo: write build_prompt()")


def fits(prompt: str, budget: int, *, reserve_for_output: int = 0) -> bool:
    """True when the prompt's estimate plus the output reserve is at most the budget."""
    raise NotImplementedError("todo: write fits()")
