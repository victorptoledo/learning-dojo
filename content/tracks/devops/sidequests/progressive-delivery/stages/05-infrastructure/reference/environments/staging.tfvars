service         = "checkout"
namespace       = "shop"
environment     = "staging"
interval        = "30s"
max_error_ratio = 0.02
failure_limit   = 1
steps = [
  { weight = 50, pause = "2m" },
  { weight = 100 },
]
