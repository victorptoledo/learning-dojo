---
from: java
status: draft
concept: "Managing cloud resources"
from_way: "Clicking through the cloud console (ClickOps) to create databases and queues by hand."
native_way: "Describe the whole environment in versioned code (`main.tf`). `terraform plan` previews changes and `terraform apply` makes them idempotently."
sensei_tip: "Keep `terraform.tfstate` in a remote backend with state locking to avoid concurrent changes by teammates."
---
