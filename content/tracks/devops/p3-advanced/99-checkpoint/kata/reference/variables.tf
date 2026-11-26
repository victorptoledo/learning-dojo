variable "environment" {
  description = "dev, staging or prod."
  type        = string

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "environment must be dev, staging or prod."
  }
}

variable "vpc_cidr" {
  description = "The VPC's network: a /16 inside 10.0.0.0/8, written as its network address."
  type        = string

  validation {
    condition = (
      can(cidrsubnet(var.vpc_cidr, 0, 0))
      && endswith(var.vpc_cidr, "/16")
      && startswith(var.vpc_cidr, "10.")
      && try(cidrsubnet(var.vpc_cidr, 0, 0) == var.vpc_cidr, false)
    )
    error_message = "vpc_cidr must be a /16 network address inside 10.0.0.0/8, such as 10.20.0.0/16."
  }
}

variable "zones" {
  description = "Three different availability zones."
  type        = list(string)

  validation {
    condition     = length(var.zones) == 3 && length(distinct(var.zones)) == 3
    error_message = "zones must list three different availability zones."
  }
}

variable "tags" {
  description = "Cost allocation tags; owner and cost-center are required."
  type        = map(string)

  validation {
    condition     = alltrue([for key in ["owner", "cost-center"] : trimspace(lookup(var.tags, key, "")) != ""])
    error_message = "tags must set owner and cost-center, so every resource can be charged to a team."
  }
}

variable "rules" {
  description = "Allowed flows between tiers: internet, lb, app and db."
  type = list(object({
    from = string
    to   = string
    port = number
  }))

  validation {
    condition     = alltrue([for rule in var.rules : contains(["internet", "lb", "app", "db"], rule.from) && contains(["lb", "app", "db"], rule.to)])
    error_message = "rules go from internet, lb, app or db to lb, app or db."
  }

  validation {
    condition     = alltrue([for rule in var.rules : rule.from != "internet" || rule.to == "lb"])
    error_message = "only the load balancers may be reached from the internet."
  }

  validation {
    condition     = alltrue([for rule in var.rules : rule.to != "db" || (rule.from == "app" && rule.port == 5432)])
    error_message = "the database may only be reached from app, on port 5432."
  }
}
