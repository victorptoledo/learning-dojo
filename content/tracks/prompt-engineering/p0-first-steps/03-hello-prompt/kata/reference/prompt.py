"""Build prompts from templates and sections, and check them against a token budget."""

import re
from collections.abc import Iterable, Mapping

PLACEHOLDER = re.compile(r"\{\{\s*([A-Za-z_][A-Za-z0-9_]*)\s*\}\}")


def estimate_tokens(text: str) -> int:
    """The dojo's token estimate: ceil(len(text) / 4)."""
    return (len(text) + 3) // 4


def render(template: str, values: Mapping[str, str]) -> str:
    """Replace every {{name}} with values[name]; KeyError if missing, ValueError if unused."""
    used = PLACEHOLDER.findall(template)
    missing = [name for name in dict.fromkeys(used) if name not in values]
    if missing:
        raise KeyError(missing[0])
    unused = sorted(set(values) - set(used))
    if unused:
        raise ValueError(f"unused values: {', '.join(unused)}")
    return PLACEHOLDER.sub(lambda match: values[match.group(1)], template)


def _section(tag: str, content: str) -> str:
    return f"<{tag}>\n{content}\n</{tag}>"


def build_prompt(
    task: str,
    *,
    context: str = "",
    rules: Iterable[str] = (),
    output_format: str = "",
) -> str:
    """Assemble <context>, <task>, <rules> and <output_format> sections, in that order."""
    if not task.strip():
        raise ValueError("task must not be blank")
    sections = []
    if context.strip():
        sections.append(_section("context", context.strip()))
    sections.append(_section("task", task.strip()))
    lines = [f"- {rule.strip()}" for rule in rules if rule.strip()]
    if lines:
        sections.append(_section("rules", "\n".join(lines)))
    if output_format.strip():
        sections.append(_section("output_format", output_format.strip()))
    return "\n\n".join(sections)


def fits(prompt: str, budget: int, *, reserve_for_output: int = 0) -> bool:
    """True when the prompt's estimate plus the output reserve is at most the budget."""
    return estimate_tokens(prompt) + reserve_for_output <= budget
