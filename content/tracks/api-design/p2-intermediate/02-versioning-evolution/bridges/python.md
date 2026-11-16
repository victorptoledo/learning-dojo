---
from: python
status: draft
concept: "Evolving an API without breaking clients"
from_way: "Pydantic models with `extra=\"forbid\"` on the client side, so any new field breaks the integration."
native_way: "`extra=\"ignore\"` (the default) for clients, additive fields on the server, a router per major version, `Deprecation`/`Sunset` headers via middleware."
sensei_tip: "A Pydantic `Literal[\"ACTIVE\", \"INACTIVE\"]` on the client is a closed enum: a new value fails validation. Accept `str` and map unknown values."
---

| Python | Evolution concept |
| --- | --- |
| `model_config = ConfigDict(extra="ignore")` | tolerant reader |
| `APIRouter(prefix="/v2")` | path version |
| `Field(deprecated=True)` | deprecated field in the schema |
| middleware adding `Sunset` | retirement notice |
| `pact-python` | consumer contract tests |
