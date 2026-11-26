variables {
  environment = "prod"
  vpc_cidr    = "10.20.0.0/16"
  zones       = ["eu-west-1a", "eu-west-1b", "eu-west-1c"]
  tags        = { owner = "payments", cost-center = "cc-4120" }
  rules = [
    { from = "internet", to = "lb", port = 443 },
    { from = "lb", to = "app", port = 8080 },
    { from = "app", to = "db", port = 5432 },
    { from = "lb", to = "app", port = 9090 },
  ]
}

run "subnets_per_zone" {
  command = plan

  assert {
    condition = output.public_subnets == {
      "eu-west-1a" = "10.20.0.0/24"
      "eu-west-1b" = "10.20.1.0/24"
      "eu-west-1c" = "10.20.2.0/24"
    }
    error_message = "public_subnets should be the /24s with netnum 0, 1 and 2, one per zone"
  }
  assert {
    condition = output.private_subnets == {
      "eu-west-1a" = "10.20.10.0/24"
      "eu-west-1b" = "10.20.11.0/24"
      "eu-west-1c" = "10.20.12.0/24"
    }
    error_message = "private_subnets should be the /24s with netnum 10, 11 and 12, one per zone"
  }
}

run "nat_per_zone_in_prod" {
  command = plan

  assert {
    condition     = output.nat_gateway_zones == tolist(["eu-west-1a", "eu-west-1b", "eu-west-1c"])
    error_message = "prod should have one NAT gateway in every zone"
  }
}

run "one_shared_nat_elsewhere" {
  command = plan

  variables {
    environment = "staging"
  }

  assert {
    condition     = output.nat_gateway_zones == tolist(["eu-west-1a"])
    error_message = "dev and staging should share one NAT gateway, in the first zone"
  }
}

run "gateway_endpoints" {
  command = plan

  assert {
    condition     = jsonencode(output.gateway_endpoints) == jsonencode(["dynamodb", "s3"])
    error_message = "gateway_endpoints should be dynamodb and s3, in that order"
  }
}

run "ingress_by_tier" {
  command = plan

  assert {
    condition = jsonencode(output.ingress) == jsonencode({
      app = ["lb:8080", "lb:9090"]
      db  = ["app:5432"]
      lb  = ["internet:443"]
    })
    error_message = "ingress should map each tier to its sorted \"from:port\" flows"
  }
}

run "platform_tags_win" {
  command = plan

  variables {
    tags = { owner = "payments", cost-center = "cc-4120", environment = "laptop", managed-by = "hand" }
  }

  assert {
    condition = jsonencode(output.tags) == jsonencode({
      "cost-center" = "cc-4120"
      environment   = "prod"
      "managed-by"  = "terraform"
      owner         = "payments"
    })
    error_message = "tags should keep the caller's tags and always set environment and managed-by = terraform"
  }
}

run "unknown_environment_is_rejected" {
  command = plan

  variables {
    environment = "production"
  }

  expect_failures = [var.environment]
}

run "public_or_too_large_cidr_is_rejected" {
  command = plan

  variables {
    vpc_cidr = "172.16.0.0/12"
  }

  expect_failures = [var.vpc_cidr]
}

run "cidr_with_host_bits_is_rejected" {
  command = plan

  variables {
    vpc_cidr = "10.20.5.0/16"
  }

  expect_failures = [var.vpc_cidr]
}

run "repeated_zone_is_rejected" {
  command = plan

  variables {
    zones = ["eu-west-1a", "eu-west-1a", "eu-west-1b"]
  }

  expect_failures = [var.zones]
}

run "missing_cost_center_is_rejected" {
  command = plan

  variables {
    tags = { owner = "payments" }
  }

  expect_failures = [var.tags]
}

run "database_open_to_the_internet_is_rejected" {
  command = plan

  variables {
    rules = [{ from = "internet", to = "db", port = 5432 }]
  }

  expect_failures = [var.rules]
}

run "app_open_to_the_internet_is_rejected" {
  command = plan

  variables {
    rules = [{ from = "internet", to = "app", port = 8080 }]
  }

  expect_failures = [var.rules]
}

run "database_from_the_load_balancer_is_rejected" {
  command = plan

  variables {
    rules = [{ from = "lb", to = "db", port = 5432 }]
  }

  expect_failures = [var.rules]
}

run "database_on_another_port_is_rejected" {
  command = plan

  variables {
    rules = [{ from = "app", to = "db", port = 22 }]
  }

  expect_failures = [var.rules]
}
