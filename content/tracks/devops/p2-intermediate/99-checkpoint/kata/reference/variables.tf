variable "environment" {
  description = "dev or prod."
  type        = string

  validation {
    condition     = contains(["dev", "prod"], var.environment)
    error_message = "The environment must be dev or prod."
  }
}

variable "services" {
  description = "The services of the platform, by name."
  type = map(object({
    replicas = optional(number, 2)
    public   = optional(bool, false)
  }))

  validation {
    condition     = length(var.services) > 0
    error_message = "Declare at least one service."
  }

  validation {
    condition     = alltrue([for name in keys(var.services) : can(regex("^[a-z][a-z0-9-]{2,20}$", name))])
    error_message = "Every service name must match ^[a-z][a-z0-9-]{2,20}$."
  }
}
