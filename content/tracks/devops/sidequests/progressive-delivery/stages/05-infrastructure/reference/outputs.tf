output "file" {
  description = "The rollout file to give rollout.sh."
  value       = local_file.rollout.filename
}

output "steps" {
  description = "How many steps the rollout has."
  value       = length(var.steps)
}

output "first_weight" {
  description = "The share of the traffic the canary gets first."
  value       = var.steps[0].weight
}
