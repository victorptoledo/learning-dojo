# The platform started with one hand-written service. It is already applied: the state holds
# module.web, and destroying it would take the web service down.
module "web" {
  source      = "./modules/service"
  name        = "web"
  environment = var.environment
  replicas    = 2
  public      = true
}

# todo: one module "service" per entry of var.services (for_each), and a moved block so
# Terraform renames module.web to module.service["web"] instead of destroying it
