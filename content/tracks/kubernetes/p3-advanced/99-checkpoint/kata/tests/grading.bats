load /opt/dojo/k8s

# The rules of one Role as "apiGroup/resource:verb,verb" lines, sorted, so a test can
# compare the whole grant at once.
grants() {
    get "$1" Role "$2" '[.rules[] | (.apiGroups // [""])[] as $g | .resources[] as $r
        | "\($g)/\($r):\(.verbs | sort | join(","))"] | sort | .[]'
}

# The NetworkPolicy's ingress or egress rules, one line per rule:
# "<number of peers> <namespace>/<pod name label or k8s-app>|<ipBlock> <PROTO/port,...>",
# for the first peer of the rule.
np_rules() {
    get prod NetworkPolicy thumbnailer '(.spec[$dir] // [])[]
        | ((if $dir == "ingress" then .from else .to end) // []) as $peers
        | ($peers[0] // {}) as $p
        | "\($peers | length) "
          + (if $p.ipBlock != null then $p.ipBlock.cidr
             else "\($p.namespaceSelector.matchLabels["kubernetes.io/metadata.name"] // "-")/\($p.podSelector.matchLabels["app.kubernetes.io/name"] // $p.podSelector.matchLabels["k8s-app"] // "-")" end)
          + " " + ([(.ports // [])[] | "\(.protocol // "TCP")/\(.port)"] | sort | join(","))' --arg dir "$1"
}

@test "the defaults render 1 replica, no HPA and no NetworkPolicy" {
    run get default Deployment thumbnailer '.spec.replicas'
    assert_output "1"
    run count default HorizontalPodAutoscaler
    assert_output "0"
    run count default NetworkPolicy
    assert_output "0"
}

@test "with autoscaling on, the HPA owns the replica count" {
    run get prod Deployment thumbnailer '.spec | has("replicas")'
    assert_output "false"
    run get prod HorizontalPodAutoscaler thumbnailer '"\(.spec.scaleTargetRef.apiVersion) \(.spec.scaleTargetRef.kind)/\(.spec.scaleTargetRef.name) \(.spec.minReplicas)-\(.spec.maxReplicas)"'
    assert_output "apps/v1 Deployment/thumbnailer 2-20"
}

@test "the HPA scales on the queue depth, 20 ready messages per replica" {
    run get prod HorizontalPodAutoscaler thumbnailer '.spec.metrics | length'
    assert_output "1"
    run get prod HorizontalPodAutoscaler thumbnailer '.spec.metrics[0] | "\(.type) \(.external.metric.name) \(.external.metric.selector.matchLabels | tojson) \(.external.target.type) \(.external.target.averageValue)"'
    assert_output 'External rabbitmq_queue_messages_ready {"queue":"thumbnails"} AverageValue 20'
}

@test "the HPA adds at most 4 Pods a minute and waits 5 minutes before scaling down" {
    run get prod HorizontalPodAutoscaler thumbnailer '[.spec.behavior.scaleUp.policies[]? | "\(.type) \(.value) \(.periodSeconds)"] | join(";")'
    assert_output "Pods 4 60"
    run get prod HorizontalPodAutoscaler thumbnailer '.spec.behavior.scaleDown.stabilizationWindowSeconds'
    assert_output "300"
}

@test "the worker meets the restricted Pod Security profile as UID 10001" {
    for target in default prod; do
        policy "$target" restricted
        run get "$target" Deployment thumbnailer '.spec.template.spec as $p | $p.containers[0].securityContext.runAsUser // $p.securityContext.runAsUser'
        assert_output "10001"
    done
}

@test "the root file system is read-only and thumbnails go to a 1Gi emptyDir at /tmp" {
    policy prod read_only_root
    run get prod Deployment thumbnailer '.spec.template.spec as $p
        | ($p.containers[0].volumeMounts // [])[] | select(.mountPath == "/tmp") | .name as $v
        | $p.volumes[] | select(.name == $v) | "\(.emptyDir != null) \(.emptyDir.sizeLimit)"'
    assert_output "true 1Gi"
}

@test "the Role grants exactly the Jobs, Pods and Pod logs the worker uses" {
    policy default least_privilege
    run grants default thumbnailer
    assert_output "$(printf '%s\n' '/pods/log:get' '/pods:get,list,watch' 'batch/jobs:create,delete,get,list,watch')"
}

@test "the RoleBinding gives the Role to the worker's ServiceAccount in the release namespace" {
    run get default RoleBinding thumbnailer '"\(.roleRef.kind)/\(.roleRef.name)"'
    assert_output "Role/thumbnailer"
    run get default RoleBinding thumbnailer '[.subjects[] | "\(.kind)/\(.namespace)/\(.name)"] | join(",")'
    assert_output "ServiceAccount/media/thumbnailer"
}

@test "rbac.create=false drops the Role and the binding but keeps the ServiceAccount" {
    run count norbac Role
    assert_output "0"
    run count norbac RoleBinding
    assert_output "0"
    run get norbac Deployment thumbnailer '.spec.template.spec.serviceAccountName'
    assert_output "thumbnailer"
    run names norbac ServiceAccount
    assert_output "thumbnailer"
}

@test "the NetworkPolicy selects the worker Pods for ingress and egress" {
    pod_labels=$(get prod Deployment thumbnailer '.spec.template.metadata.labels | tojson')
    run get prod NetworkPolicy thumbnailer '(.spec.podSelector.matchLabels // {}) as $s | ($s | length) > 0 and matches($s; $labels)' --argjson labels "$pod_labels"
    assert_output "true"
    run get prod NetworkPolicy thumbnailer '.spec.policyTypes | sort | join(",")'
    assert_output "Egress,Ingress"
}

@test "only Prometheus in the monitoring namespace may scrape the metrics port" {
    run np_rules ingress
    assert_output --regexp '^1 monitoring/prometheus TCP/(metrics|9090)$'
}

@test "the worker may reach RabbitMQ in the messaging namespace on 5672" {
    run np_rules egress
    assert_line "1 messaging/rabbitmq TCP/5672"
}

@test "the worker may reach the cluster DNS on 53 over UDP and TCP" {
    run get prod NetworkPolicy thumbnailer '[.spec.egress[]? | select(any((.to // [])[]; .podSelector.matchLabels["k8s-app"] == "kube-dns"))]
        | if length == 0 then "no rule reaches kube-dns" else
          (map(.to | length) | unique | map(tostring) | join(",")) + " "
          + ([.[].to[] | .namespaceSelector.matchLabels["kubernetes.io/metadata.name"] // "-"] | unique | join(",")) + " "
          + ([.[].ports[]? | "\(.protocol // "TCP")/\(.port)"] | unique | join(",")) end'
    assert_output "1 kube-system TCP/53,UDP/53"
}

@test "the worker may reach the API server at the address and port from the values" {
    run np_rules egress
    assert_line "1 172.20.0.10/32 TCP/6443"
}

@test "egress allows nothing beyond those three flows" {
    run get prod NetworkPolicy thumbnailer '[.spec.egress[]?
        | if ((.to // []) | length) == 0 then "a rule without to allows every destination"
          elif ((.ports // []) | length) == 0 then "a rule without ports allows every port"
          else (.to[] | select(
                  ((.namespaceSelector.matchLabels["kubernetes.io/metadata.name"] == "messaging" and .podSelector.matchLabels["app.kubernetes.io/name"] == "rabbitmq")
                   or (.namespaceSelector.matchLabels["kubernetes.io/metadata.name"] == "kube-system" and .podSelector.matchLabels["k8s-app"] == "kube-dns")
                   or (.ipBlock.cidr == "172.20.0.10/32")) | not)
                | "an extra destination: \(tojson)")
          end] | join("; ")'
    assert_output ""
}

@test "prod pins the image, wires the Service and probes, and sets resources" {
    run get prod Deployment thumbnailer '.spec.template.spec.containers[0].image'
    assert_output "ghcr.io/acme/thumbnailer:3.2.0"
    policy prod selectors
    policy prod service_targets
    policy prod probe_ports
    policy prod resources
    policy prod pinned_images
}
