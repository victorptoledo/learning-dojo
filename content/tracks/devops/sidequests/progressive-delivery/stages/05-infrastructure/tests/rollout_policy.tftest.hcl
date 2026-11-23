# Plans of the rollout policy module with chosen variables: what the file says, and what
# the validations refuse. No apply, no cluster.

variables {
  service         = "checkout"
  namespace       = "shop"
  environment     = "staging"
  max_error_ratio = 0.02
  steps = [
    { weight = 50, pause = "2m" },
    { weight = 100 },
  ]
}

run "the_file_is_named_after_the_environment_and_service" {
  command = plan

  assert {
    condition     = local_file.rollout.filename == "rollouts/staging/checkout.yaml"
    error_message = "the file should be rollouts/staging/checkout.yaml, not ${local_file.rollout.filename}"
  }

  assert {
    condition     = output.file == "rollouts/staging/checkout.yaml"
    error_message = "output file should be the file's path"
  }

  assert {
    condition     = output.steps == 2 && output.first_weight == 50
    error_message = "outputs steps and first_weight should be 2 and 50"
  }
}

run "the_names_come_from_the_service" {
  command = plan

  assert {
    condition = (
      yamldecode(local_file.rollout.content).service == "checkout" &&
      yamldecode(local_file.rollout.content).namespace == "shop" &&
      yamldecode(local_file.rollout.content).route == "checkout" &&
      yamldecode(local_file.rollout.content).stable == "checkout-stable" &&
      yamldecode(local_file.rollout.content).canary == "checkout-canary" &&
      yamldecode(local_file.rollout.content).deployment == "checkout-canary"
    )
    error_message = "service, namespace, route, stable, canary and deployment are wrong"
  }
}

run "the_steps_and_analysis_are_written" {
  command = plan

  assert {
    condition = (
      yamldecode(local_file.rollout.content).steps[0].weight == 50 &&
      yamldecode(local_file.rollout.content).steps[0].pause == "2m" &&
      yamldecode(local_file.rollout.content).steps[1].weight == 100 &&
      length(yamldecode(local_file.rollout.content).steps) == 2
    )
    error_message = "the steps are not the ones given"
  }

  assert {
    condition = (
      yamldecode(local_file.rollout.content).analysis.interval == "1m" &&
      yamldecode(local_file.rollout.content).analysis.max == 0.02 &&
      yamldecode(local_file.rollout.content).analysis.failure_limit == 2 &&
      yamldecode(local_file.rollout.content).analysis.inconclusive_limit == 3
    )
    error_message = "the analysis defaults are interval 1m, failure_limit 2, inconclusive_limit 3"
  }

  assert {
    condition     = strcontains(yamldecode(local_file.rollout.content).analysis.query, "service=\"checkout-canary\"")
    error_message = "the default query measures the checkout-canary service"
  }
}

run "a_query_can_be_given" {
  command = plan

  variables {
    query = "canary:error_ratio:rate1m"
  }

  assert {
    condition     = yamldecode(local_file.rollout.content).analysis.query == "canary:error_ratio:rate1m"
    error_message = "the query variable is not used"
  }
}

run "production_with_three_small_steps" {
  command = plan

  variables {
    environment = "production"
    steps = [
      { weight = 10, pause = "5m" },
      { weight = 50, pause = "5m" },
      { weight = 100 },
    ]
  }

  assert {
    condition     = local_file.rollout.filename == "rollouts/production/checkout.yaml"
    error_message = "the production file should be rollouts/production/checkout.yaml"
  }
}

run "production_refuses_two_steps" {
  command = plan

  variables {
    environment = "production"
    steps       = [{ weight = 10, pause = "5m" }, { weight = 100 }]
  }

  expect_failures = [var.steps]
}

run "production_refuses_a_big_first_step" {
  command = plan

  variables {
    environment = "production"
    steps       = [{ weight = 30, pause = "5m" }, { weight = 60, pause = "5m" }, { weight = 100 }]
  }

  expect_failures = [var.steps]
}

run "weights_must_go_up" {
  command = plan

  variables {
    steps = [{ weight = 50 }, { weight = 30 }, { weight = 100 }]
  }

  expect_failures = [var.steps]
}

run "the_last_step_reaches_100" {
  command = plan

  variables {
    steps = [{ weight = 10 }, { weight = 50 }]
  }

  expect_failures = [var.steps]
}

run "weights_are_integers_up_to_100" {
  command = plan

  variables {
    steps = [{ weight = 12.5 }, { weight = 100 }]
  }

  expect_failures = [var.steps]
}

run "pauses_are_durations" {
  command = plan

  variables {
    steps = [{ weight = 10, pause = "5 minutes" }, { weight = 100 }]
  }

  expect_failures = [var.steps]
}

run "the_environment_is_staging_or_production" {
  command = plan

  variables {
    environment = "qa"
  }

  expect_failures = [var.environment]
}

run "the_error_ratio_is_between_0_and_1" {
  command = plan

  variables {
    max_error_ratio = 1.5
  }

  expect_failures = [var.max_error_ratio]
}

run "the_service_is_a_kubernetes_name" {
  command = plan

  variables {
    service = "Checkout"
  }

  expect_failures = [var.service]
}
