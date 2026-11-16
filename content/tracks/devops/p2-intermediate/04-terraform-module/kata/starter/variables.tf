variable "service" {
  description = "Name of the service: lower case letters, digits and dashes, 3 to 21 characters, starting with a letter."
  type        = string
  # todo: reject names that do not match ^[a-z][a-z0-9-]{2,20}$
}

variable "environment" {
  description = "Where the service runs: dev, staging or prod."
  type        = string
  # todo: reject anything else
}

# todo: replicas (a whole number from 1 to 10, default 2, at least 3 in prod),
# regions (a non-empty set of strings, default ["eu-west-1"]) and
# extra_tags (a map of strings, default {})
