variables {
  service     = "checkout"
  environment = "dev"
}

run "dev_uses_the_defaults" {
  command = plan

  assert {
    condition     = output.name == "checkout-dev"
    error_message = "name should be checkout-dev"
  }
  assert {
    condition     = length(local_file.config) == 1 && local_file.config["eu-west-1"].filename == "out/eu-west-1/checkout-dev.json"
    error_message = "with the default regions there should be one file, out/eu-west-1/checkout-dev.json"
  }
  assert {
    condition     = jsondecode(local_file.config["eu-west-1"].content).replicas == 2
    error_message = "replicas should default to 2"
  }
  assert {
    condition     = local_file.config["eu-west-1"].file_permission == "0644"
    error_message = "the files should have mode 0644"
  }
}

run "the_file_holds_name_region_replicas_and_tags" {
  command = plan

  variables {
    replicas = 4
  }

  assert {
    condition = jsondecode(local_file.config["eu-west-1"].content) == {
      name     = "checkout-dev"
      region   = "eu-west-1"
      replicas = 4
      tags     = { service = "checkout", environment = "dev", managed_by = "terraform" }
    }
    error_message = "the file should hold exactly name, region, replicas and tags"
  }
}

run "extra_tags_cannot_override_the_standard_ones" {
  command = plan

  variables {
    extra_tags = { team = "payments", managed_by = "someone" }
  }

  assert {
    condition     = output.tags == { team = "payments", service = "checkout", environment = "dev", managed_by = "terraform" }
    error_message = "tags should be extra_tags plus the standard tags, which win"
  }
}

run "one_file_per_region" {
  command = plan

  variables {
    regions = ["us-east-1", "eu-west-1"]
  }

  assert {
    condition = output.config_files == {
      "eu-west-1" = "out/eu-west-1/checkout-dev.json"
      "us-east-1" = "out/us-east-1/checkout-dev.json"
    }
    error_message = "config_files should map each region to its file"
  }
}

run "a_bad_service_name_is_rejected" {
  command = plan

  variables {
    service = "Checkout_API"
  }

  expect_failures = [var.service]
}

run "an_unknown_environment_is_rejected" {
  command = plan

  variables {
    environment = "qa"
  }

  expect_failures = [var.environment]
}

run "more_than_ten_replicas_are_rejected" {
  command = plan

  variables {
    replicas = 11
  }

  expect_failures = [var.replicas]
}

run "prod_needs_three_replicas" {
  command = plan

  variables {
    environment = "prod"
    replicas    = 2
  }

  expect_failures = [var.replicas]
}

run "prod_with_three_replicas_is_fine" {
  command = plan

  variables {
    environment = "prod"
    replicas    = 3
  }

  assert {
    condition     = output.name == "checkout-prod"
    error_message = "name should be checkout-prod"
  }
}

run "at_least_one_region" {
  command = plan

  variables {
    regions = []
  }

  expect_failures = [var.regions]
}
