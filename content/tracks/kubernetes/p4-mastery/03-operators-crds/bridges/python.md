---
from: python
status: draft
concept: "Your own declarative APIs"
from_way: "A Django admin and a Celery task that create infrastructure when a row is inserted."
native_way: "A CRD is the model, the API server is the admin and storage, and a kopf operator is the task that keeps reality matching each object."
sensei_tip: "kopf handlers (@kopf.on.create, @kopf.on.update, @kopf.on.delete) plus kopf's own finalizer handling make a small Python operator a weekend project."
---

| Python world | Kubernetes |
| --- | --- |
| a Django model | a CRD |
| a model instance | a custom resource |
| a post_save signal + Celery task | a watch + reconcile |
| a soft-delete cleanup task | a finalizer |
