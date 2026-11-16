# One service of the platform (already done: the kata is about the root module and the
# pipeline). A real module would create a load balancer, DNS and a deployment; this one
# writes the service's configuration file.

variable "name" {
  description = "The service name."
  type        = string
}

variable "environment" {
  description = "dev or prod."
  type        = string
}

variable "replicas" {
  description = "How many instances run."
  type        = number
}

variable "public" {
  description = "Whether the service is reachable from the internet."
  type        = bool
}

locals {
  endpoint = var.public ? "https://${var.name}.example.com" : "http://${var.name}.${var.environment}.svc.cluster.local"
}

resource "local_file" "config" {
  filename        = "out/${var.environment}/${var.name}.json"
  file_permission = "0644"
  content = jsonencode({
    name     = var.name
    replicas = var.replicas
    endpoint = local.endpoint
  })
}

output "endpoint" {
  description = "Where clients reach the service."
  value       = local.endpoint
}

output "replicas" {
  description = "How many instances run."
  value       = var.replicas
}
