---
from: javascript
status: draft
concept: "Instances are disposable; build them from images and give them roles"
from_way: "A Node.js app runs under pm2 on one VPS that was set up by hand."
native_way: "Instances are built from an AMI and user data, run in an Auto Scaling group across AZs and get credentials from their role."
sensei_tip: "Install the CloudWatch agent and send pm2 or systemd logs to CloudWatch Logs; the instance can disappear any time, its logs must not."
---

| Node.js world | AWS |
| --- | --- |
| pm2 on one VPS | systemd on instances in an Auto Scaling group |
| `nvm install` on the box | an AMI with Node.js baked in |
| `.env` with keys | an instance profile |
| a bigger VPS | a right-sized instance type, or Graviton |
