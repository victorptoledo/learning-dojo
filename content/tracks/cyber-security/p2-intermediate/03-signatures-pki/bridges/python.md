---
from: python
status: draft
concept: "Prove origin and integrity: MACs, signatures, certificates"
from_way: "hmac.new(...).hexdigest() == header, and requests.get(url, verify=False) to silence errors."
native_way: "hmac.compare_digest over the raw body, Ed25519 from pyca/cryptography, and TLS verification left on (or verify= a CA bundle path)."
sensei_tip: "In Flask use `request.get_data()` and in Django `request.body` for webhook HMACs; `request.json` has already been parsed and cannot give you the signed bytes back."
---

| Python | Verify this way |
| --- | --- |
| sig == header | hmac.compare_digest(sig, header) |
| HMAC over json.dumps(request.json) | HMAC over the raw body |
| verify=False | verify=True or verify='/path/ca.pem' |
| rsa PKCS#1 v1.5 everywhere | Ed25519PrivateKey.sign |
| ssl._create_unverified_context() | ssl.create_default_context() |
