---
from: python
status: draft
concept: "Security in ordinary code"
from_way: "ModelSerializer with fields='__all__', and object lookups by id without an owner check."
native_way: "Input models with only the allowed fields, ownership checks on every object, and limits."
sensei_tip: "fields = '__all__' is mass assignment waiting to happen; list the fields, and filter querysets by request.user so another user's id returns 404."
---

| Python | In practice |
| --- | --- |
| fields = '__all__' | an explicit input model |
| Order.objects.get(id=id) | Order.objects.get(id=id, owner=request.user) |
| Pydantic model with every field | a separate request model |
| django-ratelimit | rate limits |
