variable "environment" {
  description = "dev or prod."
  type        = string

  validation {
    condition     = contains(["dev", "prod"], var.environment)
    error_message = "The environment must be dev or prod."
  }
}

# todo: services, a map from service name to an object with an optional replicas (default 2)
# and an optional public (default false); at least one service, and every name must match
# ^[a-z][a-z0-9-]{2,20}$
