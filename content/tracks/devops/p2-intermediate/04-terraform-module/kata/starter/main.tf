locals {
  # todo: name is "<service>-<environment>"
  # todo: tags are extra_tags plus service, environment and managed_by = "terraform",
  # and the standard tags win over extra_tags
}

# todo: one local_file "config" per region, at out/<region>/<name>.json, mode 0644, holding
# the JSON object { name, region, replicas, tags }
