output "public_subnets" {
  description = "Zone => /24 for the load balancers and NAT gateways."
  value       = { for index, zone in var.zones : zone => cidrsubnet(var.vpc_cidr, 8, index) }
}

output "private_subnets" {
  description = "Zone => /24 for the workloads and the database, never overlapping a public one."
  value       = { for index, zone in var.zones : zone => cidrsubnet(var.vpc_cidr, 8, index + 10) }
}

output "nat_gateway_zones" {
  description = "One NAT gateway per zone in prod (a zone failure cannot cut the others off); one shared NAT elsewhere, to save cost."
  value       = var.environment == "prod" ? var.zones : [var.zones[0]]
}

output "gateway_endpoints" {
  description = "Free gateway endpoints, so S3 and DynamoDB traffic never pays for the NAT."
  value       = ["dynamodb", "s3"]
}

output "ingress" {
  description = "Tier => the sorted \"from:port\" flows it accepts."
  value = {
    for to in distinct([for rule in var.rules : rule.to]) :
    to => sort([for rule in var.rules : "${rule.from}:${rule.port}" if rule.to == to])
  }
}

output "tags" {
  description = "The tags every resource gets: the caller's, plus environment and managed-by, which the caller cannot override."
  value       = merge(var.tags, { environment = var.environment, managed-by = "terraform" })
}
