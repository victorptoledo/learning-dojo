# TODO: compute every output from the variables (see the table in README.md).

output "public_subnets" {
  value = { for zone in var.zones : zone => var.vpc_cidr }
}

output "private_subnets" {
  value = { for zone in var.zones : zone => var.vpc_cidr }
}

output "nat_gateway_zones" {
  value = var.zones
}

output "gateway_endpoints" {
  value = []
}

output "ingress" {
  value = {}
}

output "tags" {
  value = var.tags
}
