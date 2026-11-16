terraform {
  required_version = ">= 1.9"

  # The state lives in S3; the bucket and key come from `terraform init -backend-config=...`
  # in the pipeline (a partial configuration), so nothing secret is in the code.
  backend "s3" {}

  required_providers {
    local = {
      source  = "hashicorp/local"
      version = "~> 2.9"
    }
  }
}
