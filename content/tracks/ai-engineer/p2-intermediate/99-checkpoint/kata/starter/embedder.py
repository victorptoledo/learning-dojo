"""A tiny, deterministic stand-in for an embedding model, with HR concepts. Given: do not edit.

Each dimension is a concept. A text's vector counts its words that belong to each concept, so
texts about the same concepts point in similar directions, as with a real embedding model.
"""

import re

CONCEPTS: dict[str, frozenset[str]] = {
    "meals": frozenset({"meal", "meals", "dinner", "lunch", "food", "restaurant"}),
    "travel": frozenset({"travel", "trip", "trips", "abroad", "flight", "hotel", "departure", "business"}),
    "money": frozenset({"reimbursed", "reimburse", "expense", "expenses", "eur", "cost", "refund"}),
    "approval": frozenset({"approval", "approve", "approved", "manager", "director"}),
    "holiday": frozenset({"holiday", "holidays", "vacation", "leave", "pto"}),
    "remote": frozenset({"remote", "home", "wfh"}),
    "equipment": frozenset({"equipment", "laptop", "monitor", "chair", "desk"}),
    "salary": frozenset({"salary", "salaries", "raise", "raises", "band", "bands"}),
    "rollover": frozenset({"carried", "carry", "unused", "rollover"}),
    "time": frozenset({"week", "weeks", "day", "days", "year", "years", "april", "march"}),
}
DIMENSIONS = tuple(CONCEPTS)


def embed(text: str) -> tuple[float, ...]:
    words = re.findall(r"[a-z0-9]+", text.lower())
    return tuple(float(sum(word in CONCEPTS[name] for word in words)) for name in DIMENSIONS)
