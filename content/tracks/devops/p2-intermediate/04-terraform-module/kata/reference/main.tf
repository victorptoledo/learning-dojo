locals {
  name = "${var.service}-${var.environment}"

  # merge() keeps the last value of a key, so the standard tags cannot be overridden.
  tags = merge(var.extra_tags, {
    service     = var.service
    environment = var.environment
    managed_by  = "terraform"
  })
}

resource "local_file" "config" {
  for_each = var.regions

  filename        = "out/${each.key}/${local.name}.json"
  file_permission = "0644"
  content = jsonencode({
    name     = local.name
    region   = each.key
    replicas = var.replicas
    tags     = local.tags
  })
}
