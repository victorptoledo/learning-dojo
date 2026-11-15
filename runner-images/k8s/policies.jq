# Policies over rendered Kubernetes objects. Every policy takes the array of objects of
# one render target and returns an array of violations (strings); empty means it holds.
# Used by `policy` in k8s.bash: jq -L /opt/dojo 'include "policies"; <name>'.

def ref: "\(.kind)/\(.metadata.name)";

def workload_kinds: ["Pod", "Deployment", "StatefulSet", "DaemonSet", "ReplicaSet", "Job", "CronJob"];
def is_workload: .kind as $k | workload_kinds | index($k) != null;
def is_controller: .kind as $k | ["Deployment", "StatefulSet", "DaemonSet", "ReplicaSet"] | index($k) != null;
def is_long_running: .kind as $k | ["Pod", "Deployment", "StatefulSet", "DaemonSet", "ReplicaSet"] | index($k) != null;

# The Pod template of a workload (the object itself for a bare Pod).
def pod_template:
  if .kind == "Pod" then .
  elif .kind == "CronJob" then .spec.jobTemplate.spec.template
  else .spec.template end;
def pod_spec: pod_template.spec // {};
def pod_labels: pod_template.metadata.labels // {};
def app_containers: pod_spec.containers // [] | .[];
def all_containers: pod_spec | ((.initContainers // []) + (.containers // [])) | .[];

# A selector map matches when every key/value is among the labels.
def matches($selector; $labels):
  ($selector // {}) | to_entries | all(.value == ($labels[.key] // null));

# Controllers select their own Pods: matchLabels non-empty and contained in the template labels.
def selectors:
  [ .[] | select(is_controller) | . as $w
    | ($w.spec.selector.matchLabels // {}) as $sel
    | if ($sel | length) == 0 then "\($w | ref): spec.selector.matchLabels is empty"
      elif ($sel | to_entries | all(.value == (($w | pod_labels)[.key] // null))) | not then
        "\($w | ref): selector \($sel | tojson) does not match the Pod template labels \($w | pod_labels | tojson)"
      else empty end ];

# Every Service with a selector reaches a workload, and every targetPort (a number or a
# port name) is declared by a container of one of the workloads it selects.
def service_targets:
  . as $all
  | [ .[] | select(.kind == "Service" and .spec.selector != null) | . as $svc
      | [ $all[] | select(is_long_running) | select(matches($svc.spec.selector; pod_labels)) ] as $backends
      | if ($backends | length) == 0 then
          "Service/\($svc.metadata.name): selector \($svc.spec.selector | tojson) matches no Pod template"
        else
          ($svc.spec.ports // [])[] | . as $p | ($p.targetPort // $p.port) as $t
          | if any($backends[] | app_containers | (.ports // [])[];
                   if ($t | type) == "string" then .name == $t else .containerPort == $t end)
            then empty
            else "Service/\($svc.metadata.name): port \($p.port) targets \($t | tostring), which no container of \($backends | map(ref) | join(", ")) declares"
            end
        end ];

# Long-running containers say when they are ready and when they are stuck.
def probes:
  [ .[] | select(is_controller) | ref as $r | app_containers
    | (if .readinessProbe == null then "\($r): container \(.name) has no readinessProbe" else empty end),
      (if .livenessProbe == null then "\($r): container \(.name) has no livenessProbe" else empty end) ];

# Every container, init containers included, requests CPU and memory and caps memory.
def resources:
  [ .[] | select(is_workload) | ref as $r | all_containers
    | (if .resources.requests.cpu == null then "\($r): container \(.name) has no CPU request" else empty end),
      (if .resources.requests.memory == null then "\($r): container \(.name) has no memory request" else empty end),
      (if .resources.limits.memory == null then "\($r): container \(.name) has no memory limit" else empty end) ];

# Images name a version: a tag other than latest, or a digest.
def pinned_images:
  [ .[] | select(is_workload) | ref as $r | all_containers | .image as $i
    | if ($i | test("@sha256:[0-9a-f]{64}$")) then empty
      elif ($i | test(":[^/:]+$") | not) then "\($r): image \($i) has no tag"
      elif ($i | endswith(":latest")) then "\($r): image \($i) uses the latest tag"
      else empty end ];

# The Pod Security Standards "restricted" profile, for the fields a manifest controls.
def restricted:
  [ .[] | select(is_workload) | ref as $r | pod_spec as $pod
    | ($pod.securityContext // {}) as $psc
    | (if $pod.hostNetwork == true or $pod.hostPID == true or $pod.hostIPC == true
         then "\($r): shares a host namespace (hostNetwork, hostPID or hostIPC)" else empty end),
      (($pod.volumes // [])[] | select(.hostPath != null) | "\($r): volume \(.name) is a hostPath"),
      (all_containers | . as $c | ($c.securityContext // {}) as $sc
        | (if $sc.privileged == true then "\($r): container \($c.name) is privileged" else empty end),
          (if (if $sc.runAsNonRoot != null then $sc.runAsNonRoot else $psc.runAsNonRoot end) != true
             then "\($r): container \($c.name) does not set runAsNonRoot: true" else empty end),
          (if ($sc.runAsUser // $psc.runAsUser) == 0
             then "\($r): container \($c.name) runs as UID 0" else empty end),
          (if $sc.allowPrivilegeEscalation != false
             then "\($r): container \($c.name) does not set allowPrivilegeEscalation: false" else empty end),
          (if (($sc.capabilities.drop // []) | index("ALL")) == null
             then "\($r): container \($c.name) does not drop ALL capabilities" else empty end),
          (($sc.capabilities.add // [])[] | select(. != "NET_BIND_SERVICE")
             | "\($r): container \($c.name) adds the capability \(.)"),
          (($sc.seccompProfile.type // $psc.seccompProfile.type // "") as $t
             | if $t == "RuntimeDefault" or $t == "Localhost" then empty
               else "\($r): container \($c.name) has no RuntimeDefault seccomp profile" end)) ];

# Containers cannot write to their image's file system (writes go to volumes).
def read_only_root:
  [ .[] | select(is_workload) | ref as $r | all_containers
    | select(.securityContext.readOnlyRootFilesystem != true)
    | "\($r): container \(.name) does not set readOnlyRootFilesystem: true" ];

# RBAC grants only what is named: no wildcards, no escalation verbs, no admin roles.
def least_privilege:
  [ .[] | select(.kind == "Role" or .kind == "ClusterRole") | ref as $r | (.rules // [])[]
    | (if ((.verbs // []) | index("*")) != null then "\($r): a rule allows every verb (*)" else empty end),
      (if ((.resources // []) | index("*")) != null then "\($r): a rule allows every resource (*)" else empty end),
      (if ((.apiGroups // []) | index("*")) != null then "\($r): a rule allows every API group (*)" else empty end),
      ((.verbs // [])[] | select(. == "escalate" or . == "bind" or . == "impersonate")
        | "\($r): a rule allows the \(.) verb") ]
  + [ .[] | select(.kind == "RoleBinding" or .kind == "ClusterRoleBinding")
      | select(.roleRef.kind == "ClusterRole" and (.roleRef.name == "cluster-admin" or .roleRef.name == "admin" or .roleRef.name == "edit"))
      | "\(ref): binds the built-in \(.roleRef.name) role" ];

# Every probe aims at a port its container declares (by number or name).
def probe_ports:
  [ .[] | select(is_workload) | ref as $r | app_containers | . as $c
    | [($c.ports // [])[] | .name, .containerPort] as $declared
    | ($c.startupProbe, $c.readinessProbe, $c.livenessProbe) | select(. != null)
    | (.httpGet.port // .tcpSocket.port // .grpc.port) as $p | select($p != null)
    | select(($declared | index($p)) == null)
    | "\($r): container \($c.name) probes port \($p | tostring), which it does not declare" ];

# Every ConfigMap (and key) a container reads is in the manifests, unless marked optional.
def config_refs:
  . as $all
  | [ .[] | select(is_workload) | ref as $r | all_containers | . as $c
      | ((($c.env // [])[] | .valueFrom.configMapKeyRef // empty | select(.optional != true)) as $k
          | ([$all[] | select(.kind == "ConfigMap" and .metadata.name == $k.name)] | first) as $cm
          | if $cm == null then "\($r): container \($c.name) reads the ConfigMap \($k.name), which is not in the manifests"
            elif (($cm.data // {}) | has($k.key)) | not then "\($r): container \($c.name) reads the key \($k.key) of the ConfigMap \($k.name), which has no such key"
            else empty end),
        ((($c.envFrom // [])[] | .configMapRef // empty | select(.optional != true)) as $k
          | if any($all[]; .kind == "ConfigMap" and .metadata.name == $k.name) then empty
            else "\($r): container \($c.name) loads the ConfigMap \($k.name), which is not in the manifests" end) ];
