terraform {
  # No provider: this module only computes the network plan (subnets, NAT gateways,
  # endpoints, flows and tags) that the platform's AWS modules consume, so the rules
  # can be tested without a cloud account.
  required_version = ">= 1.9"
}
