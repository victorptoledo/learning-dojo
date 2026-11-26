# Runs in a generated service's infra/ folder, whose terraform.tfvars sets the service.

run "critical_tier" {
  command = plan

  variables {
    tier = "critical"
  }

  assert {
    condition = jsonencode(output.backup_policy) == jsonencode({
      cross_region_copy       = true
      immutable               = true
      point_in_time_recovery  = true
      restore_test_every_days = 30
      retention_days          = 35
      rpo_minutes             = 15
      rto_minutes             = 120
    })
    error_message = "a critical service needs point-in-time recovery (RPO 15 minutes), RTO 120, immutable cross-region copies kept 35 days and a restore test every 30 days"
  }
}

run "standard_tier" {
  command = plan

  variables {
    tier = "standard"
  }

  assert {
    condition = jsonencode(output.backup_policy) == jsonencode({
      cross_region_copy       = true
      immutable               = true
      point_in_time_recovery  = false
      restore_test_every_days = 90
      retention_days          = 14
      rpo_minutes             = 1440
      rto_minutes             = 480
    })
    error_message = "a standard service has daily backups (RPO 1440), RTO 480, immutable cross-region copies kept 14 days and a restore test every 90 days"
  }
}

run "tags" {
  command = plan

  variables {
    service = "orders"
    team    = "checkout"
    tier    = "critical"
  }

  assert {
    condition     = jsonencode(output.tags) == jsonencode({ managed-by = "terraform", service = "orders", team = "checkout", tier = "critical" })
    error_message = "tags should be service, team, tier and managed-by = terraform"
  }
}

run "unknown_tier_is_rejected" {
  command = plan

  variables {
    tier = "gold"
  }

  expect_failures = [var.tier]
}

run "bad_service_name_is_rejected" {
  command = plan

  variables {
    service = "Orders_API"
  }

  expect_failures = [var.service]
}
