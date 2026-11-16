variables {
  environment = "prod"
  services = {
    web    = { replicas = 3, public = true }
    orders = { replicas = 3 }
    worker = {}
  }
}

run "one_module_per_service" {
  command = plan

  assert {
    condition     = length(module.service) == 3 && contains(keys(module.service), "web")
    error_message = "there should be one module.service instance per entry of var.services"
  }
}

run "optional_attributes_have_defaults" {
  command = plan

  assert {
    condition     = module.service["worker"].replicas == 2 && module.service["orders"].replicas == 3
    error_message = "replicas should default to 2 and keep an explicit value"
  }
  assert {
    condition     = module.service["worker"].endpoint == "http://worker.prod.svc.cluster.local"
    error_message = "public should default to false"
  }
}

run "endpoints_by_service" {
  command = plan

  assert {
    condition = output.endpoints == {
      web    = "https://web.example.com"
      orders = "http://orders.prod.svc.cluster.local"
      worker = "http://worker.prod.svc.cluster.local"
    }
    error_message = "endpoints should map every service to its module's endpoint"
  }
}

run "public_services_sorted" {
  command = plan

  variables {
    services = {
      zeta  = { public = true }
      alpha = { public = true }
      inner = {}
    }
  }

  assert {
    condition     = output.public_services == tolist(["alpha", "zeta"])
    error_message = "public_services should list only the public services, sorted"
  }
}

run "bad_service_names_are_rejected" {
  command = plan

  variables {
    services = { Web_App = {} }
  }

  expect_failures = [var.services]
}

run "no_services_is_rejected" {
  command = plan

  variables {
    services = {}
  }

  expect_failures = [var.services]
}

run "only_web_is_public" {
  command = plan

  assert {
    condition     = output.public_services == tolist(["web"])
    error_message = "with the services of prod.tfvars only web is public"
  }
}
