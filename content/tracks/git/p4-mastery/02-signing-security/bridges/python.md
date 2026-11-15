---
from: python
status: draft
concept: "Signing and secrets"
from_way: "A settings.py or notebook with the API key pasted in 'just for now'."
native_way: "Secrets from the environment (os.environ, python-dotenv with .env ignored), detect-secrets or gitleaks in pre-commit, and push protection on the host."
sensei_tip: "Notebook outputs leak too: a printed token or connection string stays in the .ipynb JSON. Strip outputs before committing (nbstripout as a filter)."
---

| Python habit | Git practice |
| --- | --- |
| key pasted in `settings.py` | `os.environ["API_KEY"]` |
| `.env` file | in `.gitignore`, `.env.example` committed |
| a token in a notebook output | strip outputs before committing |
| PyPI trusted publishing | no long-lived token to leak at all |
