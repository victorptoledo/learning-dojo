"""A tiny, deterministic stand-in for an embedding model. Given: do not edit.

Each dimension is a concept. A text's vector counts its words that belong to each concept, so
texts about the same concepts point in similar directions, as with a real embedding model.
"""

import re

CONCEPTS: dict[str, frozenset[str]] = {
    "refund": frozenset({"refund", "refunds", "refunded", "reimburse", "money", "return", "returns", "returned"}),
    "password": frozenset({"password", "passwords", "login", "log", "sign", "credentials", "reset"}),
    "shipping": frozenset({"ship", "shipped", "shipping", "delivery", "deliver", "delivered", "parcel", "parcels", "package", "courier", "track", "tracking", "arrive"}),
    "cancel": frozenset({"cancel", "cancelled", "cancellation", "terminate", "unsubscribe"}),
    "invoice": frozenset({"invoice", "invoices", "receipt", "receipts", "bill", "billing", "vat"}),
    "account": frozenset({"account", "accounts", "profile", "email", "username"}),
    "payment": frozenset({"pay", "paid", "payment", "payments", "card", "cards", "paypal", "charge", "charged"}),
    "time": frozenset({"days", "day", "week", "weeks", "long", "when", "time", "hours"}),
    "abroad": frozenset({"abroad", "international", "country", "countries", "customs", "overseas"}),
    "security": frozenset({"secure", "security", "hacked", "fraud", "2fa", "suspicious"}),
}
DIMENSIONS = tuple(CONCEPTS)


def embed(text: str) -> tuple[float, ...]:
    words = re.findall(r"[a-z0-9]+", text.lower())
    return tuple(float(sum(word in CONCEPTS[name] for word in words)) for name in DIMENSIONS)
