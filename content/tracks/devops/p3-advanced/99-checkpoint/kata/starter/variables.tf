variable "environment" {
  description = "dev, staging or prod."
  type        = string
}

variable "vpc_cidr" {
  description = "The VPC's network: a /16 inside 10.0.0.0/8, written as its network address."
  type        = string
}

variable "zones" {
  description = "Three different availability zones."
  type        = list(string)
}

variable "tags" {
  description = "Cost allocation tags; owner and cost-center are required."
  type        = map(string)
}

variable "rules" {
  description = "Allowed flows between tiers: internet, lb, app and db."
  type = list(object({
    from = string
    to   = string
    port = number
  }))
}
