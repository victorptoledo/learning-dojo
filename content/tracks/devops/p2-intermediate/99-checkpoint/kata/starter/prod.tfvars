environment = "prod"

services = {
  web    = { replicas = 3, public = true }
  orders = { replicas = 3 }
  worker = {}
}
