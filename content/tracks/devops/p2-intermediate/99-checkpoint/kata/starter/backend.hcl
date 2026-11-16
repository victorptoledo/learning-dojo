# Partial backend configuration, passed with terraform init -backend-config=backend.hcl.
bucket       = "acme-terraform-state"
key          = "platform/prod.tfstate"
region       = "eu-west-1"
use_lockfile = true
