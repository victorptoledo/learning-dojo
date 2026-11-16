variable "service" {
  description = "Name of the service: lower case letters, digits and dashes, 3 to 21 characters, starting with a letter."
  type        = string

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{2,20}$", var.service))
    error_message = "The service name must match ^[a-z][a-z0-9-]{2,20}$."
  }
}

variable "environment" {
  description = "Where the service runs: dev, staging or prod."
  type        = string

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "The environment must be dev, staging or prod."
  }
}

variable "replicas" {
  description = "How many instances run in each region."
  type        = number
  default     = 2

  validation {
    condition     = var.replicas >= 1 && var.replicas <= 10 && floor(var.replicas) == var.replicas
    error_message = "Replicas must be a whole number from 1 to 10."
  }

  validation {
    condition     = var.environment != "prod" || var.replicas >= 3
    error_message = "Production needs at least 3 replicas."
  }
}

variable "regions" {
  description = "The regions the service is deployed to."
  type        = set(string)
  default     = ["eu-west-1"]

  validation {
    condition     = length(var.regions) > 0
    error_message = "Give at least one region."
  }
}

variable "extra_tags" {
  description = "Tags added to the standard ones; they cannot override service, environment or managed_by."
  type        = map(string)
  default     = {}
}
