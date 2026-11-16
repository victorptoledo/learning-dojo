---
from: python
status: reviewed
concept: "Pydantic models as the contract"
from_way: "Pydantic or DRF serializers on the server, and the front end guessing the shapes from example responses."
native_way: "FastAPI publishes OpenAPI from the Pydantic models; the front end generates types from it, and both sides validate at their boundary."
sensei_tip: "zod is to TypeScript what Pydantic is to Python: a runtime schema that also produces the static type."
---

| Python | Contract |
| --- | --- |
| `class NewTodo(BaseModel)` | `const NewTodo = z.object(...)` |
| `model_validate(data)` | `NewTodo.safeParse(data)` |
| `/openapi.json` in FastAPI | the source for generated clients |
| `HTTPException(422, detail=...)` | problem details with field errors |
