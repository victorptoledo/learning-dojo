output "name" {
  description = "The service name with its environment."
  value       = local.name
}

output "tags" {
  description = "The tags every resource of the service carries."
  value       = local.tags
}

output "config_files" {
  description = "The configuration file of each region."
  value       = { for region, file in local_file.config : region => file.filename }
}
