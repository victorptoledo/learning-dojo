---
from: python
status: draft
concept: "Networks, load balancers and bills are part of the design"
from_way: "A requests.get to an external API just works from your laptop."
native_way: "From a private subnet it goes through NAT (and costs per gigabyte); calls to cloud services should use private endpoints."
sensei_tip: "boto3 uses the regional endpoint; with a VPC endpoint and private DNS enabled the same code takes the private path without changes."
---

| Python world | Cloud |
| --- | --- |
| `requests.get` to the internet | egress through NAT |
| `boto3` to S3 | a gateway or interface endpoint |
| `socket.gethostbyname` | private and public DNS zones |
| a cron to delete old files | lifecycle rules |
