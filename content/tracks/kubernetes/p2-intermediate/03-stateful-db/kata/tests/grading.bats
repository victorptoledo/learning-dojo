load /opt/dojo/k8s

container='.spec.template.spec.containers[] | select(.name == "postgres")'

@test "runs 3 replicas behind the postgres Service" {
    run get manifests StatefulSet postgres '"\(.spec.replicas) \(.spec.serviceName)"'
    assert_output "3 postgres"
    policy manifests selectors
}

@test "the Service selects the postgres Pods on the port named postgres" {
    policy manifests service_targets
    run get manifests Service postgres '.spec.ports[0] | "\(.name) \(.port)"'
    assert_output "postgres 5432"
}

@test "reads POSTGRES_PASSWORD from the postgres-auth Secret" {
    run get manifests StatefulSet postgres "$container"' | .env[] | select(.name == "POSTGRES_PASSWORD") | "\(.value // "-") \(.valueFrom.secretKeyRef.name) \(.valueFrom.secretKeyRef.key)"'
    assert_output "- postgres-auth password"
}

@test "sets PGDATA below the volume mount" {
    run get manifests StatefulSet postgres "$container"' | .env[] | select(.name == "PGDATA") | .value'
    assert_output "/var/lib/postgresql/data/pgdata"
}

@test "claims a 20Gi ReadWriteOnce fast-ssd volume per replica" {
    run get manifests StatefulSet postgres '.spec.volumeClaimTemplates[] | select(.metadata.name == "data") | .spec | "\(.accessModes | join(",")) \(.storageClassName) \(.resources.requests.storage)"'
    assert_output "ReadWriteOnce fast-ssd 20Gi"
}

@test "mounts the data volume at /var/lib/postgresql/data" {
    run get manifests StatefulSet postgres "$container"' | .volumeMounts[]? | select(.name == "data") | .mountPath'
    assert_output "/var/lib/postgresql/data"
}

@test "has readiness and liveness probes" {
    policy manifests probes
}

@test "requests 500m CPU and 1Gi memory with a 1Gi memory limit" {
    policy manifests resources
    run get manifests StatefulSet postgres "$container"' | .resources | "\(.requests.cpu) \(.requests.memory) \(.limits.memory)"'
    assert_output "500m 1Gi 1Gi"
}

@test "spreads the members over nodes" {
    run get manifests StatefulSet postgres '.spec.template.spec.topologySpreadConstraints[]? | select(.topologyKey == "kubernetes.io/hostname") | "\(.maxSkew) \(.labelSelector.matchLabels["app.kubernetes.io/name"])"'
    assert_output "1 postgres"
}

@test "a PodDisruptionBudget allows at most one unavailable member" {
    run get manifests PodDisruptionBudget postgres '"\(.spec.maxUnavailable) \(.spec.selector.matchLabels["app.kubernetes.io/name"])"'
    assert_output "1 postgres"
}
