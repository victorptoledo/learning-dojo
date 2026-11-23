variable "service" {
  description = "The service; its Services are SERVICE-stable and SERVICE-canary, its canary Deployment SERVICE-canary."
  type        = string

  validation {
    condition     = can(regex("^[a-z0-9]([-a-z0-9]*[a-z0-9])?$", var.service)) && length(var.service) <= 56
    error_message = "service must be a Kubernetes name, short enough for the -stable and -canary suffixes."
  }
}

variable "namespace" {
  description = "The namespace of the route, Services and Deployments."
  type        = string

  validation {
    condition     = can(regex("^[a-z0-9]([-a-z0-9]*[a-z0-9])?$", var.namespace)) && length(var.namespace) <= 63
    error_message = "namespace must be a Kubernetes name."
  }
}

variable "environment" {
  description = "Where the policy applies."
  type        = string

  validation {
    condition     = contains(["staging", "production"], var.environment)
    error_message = "environment must be staging or production."
  }
}

variable "steps" {
  description = "The canary steps: the weight sent to the canary, and how long to watch it."
  type = list(object({
    weight = number
    pause  = optional(string)
  }))

  validation {
    condition     = try(var.steps[length(var.steps) - 1].weight == 100, false)
    error_message = "The last step must reach weight 100."
  }

  validation {
    condition = alltrue([
      for step in var.steps : step.weight >= 1 && step.weight <= 100 && floor(step.weight) == step.weight
    ])
    error_message = "Every weight must be an integer from 1 to 100."
  }

  validation {
    condition = try(alltrue([
      for i, step in slice(var.steps, 1, length(var.steps)) : step.weight > var.steps[i].weight
    ]), false)
    error_message = "Weights must go up from one step to the next."
  }

  validation {
    condition     = alltrue([for step in var.steps : step.pause == null || can(regex("^[0-9]+[smh]$", step.pause))])
    error_message = "A pause must be a duration like 30s, 5m or 1h."
  }

  # Production goes slowly: at least three steps, and the first one small.
  validation {
    condition     = var.environment != "production" || try(length(var.steps) >= 3 && var.steps[0].weight <= 10, false)
    error_message = "In production a rollout has at least 3 steps and starts at 10% or less."
  }
}

variable "interval" {
  description = "How often the canary is measured during a pause."
  type        = string
  default     = "1m"

  validation {
    condition     = can(regex("^[0-9]+[smh]$", var.interval)) && !startswith(var.interval, "0")
    error_message = "interval must be a duration like 30s, 5m or 1h."
  }
}

variable "max_error_ratio" {
  description = "Above this error ratio, a check fails."
  type        = number

  validation {
    condition     = var.max_error_ratio > 0 && var.max_error_ratio < 1
    error_message = "max_error_ratio must be between 0 and 1."
  }
}

variable "failure_limit" {
  description = "Failed checks in a row that roll back."
  type        = number
  default     = 2

  validation {
    condition     = var.failure_limit >= 1 && floor(var.failure_limit) == var.failure_limit
    error_message = "failure_limit must be an integer of at least 1."
  }
}

variable "query" {
  description = "The query that gives the canary's error ratio; by default its 5xx ratio."
  type        = string
  default     = null
}

variable "output_dir" {
  description = "Where the rollout files go: OUTPUT_DIR/ENVIRONMENT/SERVICE.yaml."
  type        = string
  default     = "rollouts"
}
