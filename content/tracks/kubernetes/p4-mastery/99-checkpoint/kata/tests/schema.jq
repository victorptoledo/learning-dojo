# A small validator for custom resources, so the tests can ask "would the API server accept
# this Database?" without a cluster. It covers the part of a structural OpenAPI v3 schema the
# kata uses: type, required, properties (unknown fields are rejected, as kubectl's default
# strict field validation does, unless x-kubernetes-preserve-unknown-fields is set), enum,
# minimum, maximum, items and additionalProperties. CEL rules are not evaluated.

def type_ok($t):
  if $t == null then true
  elif $t == "integer" then type == "number" and . == floor
  elif $t == "number" then type == "number"
  elif $t == "string" then type == "string"
  elif $t == "boolean" then type == "boolean"
  elif $t == "array" then type == "array"
  elif $t == "object" then type == "object"
  else true end;

# errors($schema; $path): the violations of . as strings.
def errors($s; $path):
  . as $v
  | if ($v | type_ok($s.type)) | not then ["\($path): must be of type \($s.type)"]
    else
      [ (if $s.enum != null and (any($s.enum[]; . == $v) | not)
           then "\($path): \($v | tojson) is not one of \($s.enum | tojson)" else empty end),
        (if $s.minimum != null and ($v | type) == "number" and $v < $s.minimum
           then "\($path): \($v) is below the minimum \($s.minimum)" else empty end),
        (if $s.maximum != null and ($v | type) == "number" and $v > $s.maximum
           then "\($path): \($v) is above the maximum \($s.maximum)" else empty end),
        (if ($v | type) == "object" then
           (($s.required // [])[] | . as $r | select(($v | has($r)) | not) | "\($path).\($r): required"),
           ($v | keys[] | . as $k | select((($s.properties // {}) | has($k)) | not)
             | if $s["x-kubernetes-preserve-unknown-fields"] == true then empty
               elif ($s.additionalProperties | type) == "object" then ($v[$k] | errors($s.additionalProperties; "\($path).\($k)"))[]
               else "\($path).\($k): unknown field" end),
           (($s.properties // {}) | to_entries[] | . as $p | select($v | has($p.key))
             | ($v[$p.key] | errors($p.value; "\($path).\($p.key)"))[])
         else empty end),
        (if ($v | type) == "array" and $s.items != null then
           ($v | to_entries[] | (.value | errors($s.items; "\($path)[\(.key)]"))[])
         else empty end) ]
    end;

# validate_cr($crd): the violations of the custom resource . against the CRD, for the
# version its apiVersion names.
def validate_cr($crd):
  . as $cr
  | ($cr.apiVersion | split("/")) as [$group, $version]
  | if $group != $crd.spec.group or $cr.kind != $crd.spec.names.kind then ["not a \($crd.spec.names.kind) of \($crd.spec.group)"]
    else ([$crd.spec.versions[] | select(.name == $version and .served == true)] | first) as $v
      | if $v == null then ["the version \($version) is not served"]
        else ($cr | del(.apiVersion, .kind, .metadata) | errors($v.schema.openAPIV3Schema; "")) end
    end;
