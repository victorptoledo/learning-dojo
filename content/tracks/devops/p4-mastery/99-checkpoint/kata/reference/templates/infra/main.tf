# The paved-road data module of a service: its backup policy and tags, decided by its tier.
# Every service gets this file unchanged; only terraform.tfvars differs.
terraform {
  required_version = ">= 1.9"
}

variable "service" {
  type = string

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{1,28}[a-z0-9]$", var.service))
    error_message = "service must be 3 to 30 lowercase letters, digits and dashes."
  }
}

variable "team" {
  type = string

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]*$", var.team))
    error_message = "team must be lowercase letters, digits and dashes."
  }
}

variable "tier" {
  type = string

  validation {
    condition     = contains(["critical", "standard"], var.tier)
    error_message = "tier must be critical or standard."
  }
}

locals {
  # RPO and RTO in minutes. Both tiers copy backups to another region and keep them
  # immutable, so a region outage, a bad script or ransomware cannot take the backups too.
  policies = {
    critical = {
      rpo_minutes             = 15
      rto_minutes             = 120
      point_in_time_recovery  = true
      cross_region_copy       = true
      immutable               = true
      retention_days          = 35
      restore_test_every_days = 30
    }
    standard = {
      rpo_minutes             = 1440
      rto_minutes             = 480
      point_in_time_recovery  = false
      cross_region_copy       = true
      immutable               = true
      retention_days          = 14
      restore_test_every_days = 90
    }
  }
}

output "backup_policy" {
  value = local.policies[var.tier]
}

output "tags" {
  value = {
    service    = var.service
    team       = var.team
    tier       = var.tier
    managed-by = "terraform"
  }
}
