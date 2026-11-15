---
from: python
status: draft
concept: "From Flask views to typed endpoints"
from_way: "Flask routes reading request.get_json() and validating by hand (or with marshmallow), jsonify for output, global app config dicts, apispec for documentation, and sync code everywhere."
native_way: "FastAPI endpoints whose typed parameters and Pydantic models do validation and serialisation, Depends for shared logic, pydantic-settings for configuration, automatic OpenAPI, and async where the libraries are async."
sensei_tip: "The biggest change is not async but the types: once request and response models exist, validation, documentation and client generation come for free. You can get most of it in Flask too with Pydantic, but FastAPI makes it the default."
---

| Older habit | Modern typed service |
| --- | --- |
| `data = request.get_json(); if "sku" not in data: abort(400)` | `def create(order: OrderIn)` |
| `return jsonify(order.to_dict())` | `-> OrderOut` (response model) |
| marshmallow schemas + apispec | Pydantic models, OpenAPI generated |
| `app.config["DB_URL"]` | `Settings(BaseSettings)` |
| `before_request` hooks for auth | `Depends(current_user)` |
| `app.test_client()` with monkeypatching | `TestClient` + `dependency_overrides` |
| Gunicorn sync workers | Uvicorn (or Gunicorn with Uvicorn workers) |
