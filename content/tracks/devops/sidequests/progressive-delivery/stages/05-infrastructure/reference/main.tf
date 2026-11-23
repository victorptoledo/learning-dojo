locals {
  canary = "${var.service}-canary"
  default_query = join(" / ", [
    "sum(rate(http_requests_total{service=\"${local.canary}\",code=~\"5..\"}[${var.interval}]))",
    "sum(rate(http_requests_total{service=\"${local.canary}\"}[${var.interval}]))",
  ])

  # The rollout file rollout.sh reads; a step without a pause has pause: null.
  rollout = {
    service    = var.service
    namespace  = var.namespace
    route      = var.service
    stable     = "${var.service}-stable"
    canary     = local.canary
    deployment = local.canary
    steps      = [for step in var.steps : { weight = step.weight, pause = step.pause }]
    analysis = {
      interval           = var.interval
      query              = coalesce(var.query, local.default_query)
      max                = var.max_error_ratio
      failure_limit      = var.failure_limit
      inconclusive_limit = 3
    }
  }
}

resource "local_file" "rollout" {
  filename        = "${var.output_dir}/${var.environment}/${var.service}.yaml"
  content         = yamlencode(local.rollout)
  file_permission = "0644"
}
