# Every service of the platform, from var.services.
module "service" {
  source   = "./modules/service"
  for_each = var.services

  name        = each.key
  environment = var.environment
  replicas    = each.value.replicas
  public      = each.value.public
}

# The web service was applied as module.web before for_each: rename it in the state instead
# of destroying and recreating it.
moved {
  from = module.web
  to   = module.service["web"]
}
