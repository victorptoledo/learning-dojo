service         = "checkout"
namespace       = "shop"
environment     = "production"
interval        = "1m"
max_error_ratio = 0.01
failure_limit   = 2
steps = [
  { weight = 10, pause = "5m" },
  { weight = 25, pause = "5m" },
  { weight = 50, pause = "10m" },
  { weight = 100 },
]
