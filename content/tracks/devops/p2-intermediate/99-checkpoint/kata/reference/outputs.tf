output "endpoints" {
  description = "Where clients reach each service."
  value       = { for name, service in module.service : name => service.endpoint }
}

output "public_services" {
  description = "The services reachable from the internet, sorted."
  value       = sort([for name, service in var.services : name if service.public])
}
