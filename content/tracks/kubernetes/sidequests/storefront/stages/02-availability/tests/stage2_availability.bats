load /opt/dojo/k8s
source "$BATS_TEST_DIRNAME/helpers.sh"

# The lowest replica count of a workload: its replicas, or its HPA's minimum (stage 3).
min_scale() {
    k8s manifests '. as $all
      | ([.[] | select(.metadata.name == $name and (.kind == "Deployment" or .kind == "StatefulSet"))] | first) as $w
      | ([$all[] | select(.kind == "HorizontalPodAutoscaler" and .spec.scaleTargetRef.name == $name) | .spec.minReplicas // 1] | first) as $hpa
      | $w.spec.replicas // $hpa // 1' --arg name "$1"
}

# The disruptions a budget allows with WORKLOAD at REPLICAS healthy Pods.
allowed_disruptions() {
    get manifests PodDisruptionBudget "$1" '
      def count($v): if ($v | type) == "string" then (($v | rtrimstr("%") | tonumber) * $n / 100) else $v end;
      if .spec.maxUnavailable != null then (count(.spec.maxUnavailable) | floor)
      else $n - (count(.spec.minAvailable // 0) | ceil) end' --argjson n "$2"
}

@test "web and api have their disruption budgets" {
    run get manifests PodDisruptionBudget web '"\(.spec.minAvailable) \(.spec.maxUnavailable // "none") \(.spec.unhealthyPodEvictionPolicy)"'
    assert_output "2 none AlwaysAllow"
    run get manifests PodDisruptionBudget api '"\(.spec.maxUnavailable) \(.spec.minAvailable // "none") \(.spec.unhealthyPodEvictionPolicy)"'
    assert_output "1 none AlwaysAllow"
}

@test "each budget selects the Pods of one workload" {
    for pdb in $(names manifests PodDisruptionBudget); do
        selector=$(get manifests PodDisruptionBudget "$pdb" '.spec.selector.matchLabels // {} | tojson')
        run k8s manifests '[.[] | select(.kind == "Deployment" or .kind == "StatefulSet") | select(($s | length) > 0 and matches($s; pod_labels)) | .metadata.name] | join(" ")' --argjson s "$selector"
        [ "$(wc -w <<< "$output")" -eq 1 ] || fail "PodDisruptionBudget $pdb selects '$output', not exactly one workload"
    done
}

@test "no budget blocks a node drain" {
    for pdb in $(names manifests PodDisruptionBudget); do
        selector=$(get manifests PodDisruptionBudget "$pdb" '.spec.selector.matchLabels // {} | tojson')
        workload=$(k8s manifests '[.[] | select(.kind == "Deployment" or .kind == "StatefulSet") | select(($s | length) > 0 and matches($s; pod_labels)) | .metadata.name] | first // ""' --argjson s "$selector")
        [ -n "$workload" ] || continue
        scale=$(min_scale "$workload")
        run allowed_disruptions "$pdb" "$scale"
        [ "$output" -ge 1 ] || fail "PodDisruptionBudget $pdb allows $output disruptions with $workload at $scale replicas: every drain would hang"
    done
}

@test "web and api spread across zones and nodes" {
    for name in web api; do
        labels=$(get manifests Deployment "$name" '.spec.template.metadata.labels | tojson')
        run get manifests Deployment "$name" '[.spec.template.spec.topologySpreadConstraints // [] | .[]
          | select(matches(.labelSelector.matchLabels // {}; $labels) and ((.labelSelector.matchLabels // {}) | length) > 0)
          | select((.matchLabelKeys // []) | index("pod-template-hash") != null)
          | "\(.topologyKey) \(.maxSkew) \(.whenUnsatisfiable)"] | sort | join(", ")' --argjson labels "$labels"
        assert_output "kubernetes.io/hostname 1 ScheduleAnyway, topology.kubernetes.io/zone 1 DoNotSchedule"
    done
}

@test "a critical priority exists and is not the default" {
    run get manifests PriorityClass shop-critical '"\(.value) \(.globalDefault) \(.preemptionPolicy) \((.description // "") | length > 0)"'
    assert_output "100000 false PreemptLowerPriority true"
}

@test "api and db are critical, web is not" {
    run get manifests Deployment api '.spec.template.spec.priorityClassName // "none"'
    assert_output "shop-critical"
    run get manifests StatefulSet db '.spec.template.spec.priorityClassName // "none"'
    assert_output "shop-critical"
    run get manifests Deployment web '.spec.template.spec.priorityClassName // "none"'
    assert_output "none"
}
