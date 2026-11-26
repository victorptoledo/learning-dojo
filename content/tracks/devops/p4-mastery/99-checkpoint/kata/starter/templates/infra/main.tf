# The paved-road data module of a service: its backup policy and tags, decided by its tier.
# Every service gets this file unchanged; only terraform.tfvars differs.
terraform {
  required_version = ">= 1.9"
}

variable "service" {
  type = string
}

variable "team" {
  type = string
}

variable "tier" {
  type = string
}

# TODO: one policy per tier (see README.md). Today every service gets what the first one
# had: daily snapshots, kept a week, in the same region and account.
output "backup_policy" {
  value = {
    rpo_minutes             = 1440
    rto_minutes             = 1440
    point_in_time_recovery  = false
    cross_region_copy       = false
    immutable               = false
    retention_days          = 7
    restore_test_every_days = 0
  }
}

output "tags" {
  value = {
    service = var.service
  }
}
