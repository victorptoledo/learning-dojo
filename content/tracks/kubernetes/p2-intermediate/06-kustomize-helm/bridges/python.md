---
from: python
status: draft
concept: "One definition, many environments"
from_way: "Jinja2-templated configuration files rendered by Ansible for each inventory group."
native_way: "Helm is the Jinja-like path (templates plus values); Kustomize is the template-free path (patches on plain YAML)."
sensei_tip: "If you know Jinja2, Helm's Go templates will feel familiar, but whitespace control is different: learn {{- and -}} and nindent early."
---

| Python world | Kubernetes |
| --- | --- |
| Jinja2 templates | Helm templates |
| `group_vars/prod.yml` | `values-prod.yaml` |
| pip packages on PyPI | charts in a registry |
| a settings override module | a Kustomize patch |
