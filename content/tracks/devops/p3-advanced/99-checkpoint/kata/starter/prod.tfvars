environment = "prod"
vpc_cidr    = "10.20.0.0/16"
zones       = ["eu-west-1a", "eu-west-1b", "eu-west-1c"]
tags        = { owner = "payments", cost-center = "cc-4120" }
rules = [
  { from = "internet", to = "lb", port = 443 },
  { from = "lb", to = "app", port = 8080 },
  { from = "app", to = "db", port = 5432 },
]
