import pytest

from dojo_aws import getatt, outputs, ref
from webapp import CERTIFICATE, attributes, ingress, one, props, stack, tpl

ALB = "AWS::ElasticLoadBalancingV2::LoadBalancer"
LISTENER = "AWS::ElasticLoadBalancingV2::Listener"
TARGETS = "AWS::ElasticLoadBalancingV2::TargetGroup"


def parameter(name):
    found = tpl().get("Parameters", {})
    assert name in found, f"declare the parameter {name}"
    return found[name]


@pytest.mark.parametrize("name,kind", [
    ("PublicSubnetIds", "List<AWS::EC2::Subnet::Id>"),
    ("PrivateSubnetIds", "List<AWS::EC2::Subnet::Id>"),
    ("ImageId", "AWS::SSM::Parameter::Value<AWS::EC2::Image::Id>"),
])
def test_the_deployment_parameters_have_their_types(name, kind):
    assert parameter(name).get("Type") == kind, f"{name} must be of Type {kind}"


def test_the_image_is_the_latest_amazon_linux_from_ssm():
    assert parameter("ImageId").get("Default") == "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-arm64"


def test_the_instance_type_is_one_of_three_graviton_sizes():
    spec = parameter("InstanceType")
    assert spec.get("AllowedValues") == ["t4g.small", "t4g.medium", "m7g.large"], "InstanceType AllowedValues"
    assert spec.get("Default") == "t4g.small", "InstanceType defaults to t4g.small"


def test_the_certificate_must_be_an_acm_arn():
    pattern = parameter("CertificateArn").get("AllowedPattern", "")
    assert pattern.startswith("^arn:aws:acm:"), "CertificateArn needs an AllowedPattern starting with ^arn:aws:acm:"


@pytest.mark.parametrize("group,expected", [
    ("LoadBalancerSecurityGroup", [{"protocol": "tcp", "ports": (443, 443), "cidr": "0.0.0.0/0"},
                                   {"protocol": "tcp", "ports": (80, 80), "cidr": "0.0.0.0/0"}]),
    ("AppSecurityGroup", [{"protocol": "tcp", "ports": (8080, 8080), "group": "LoadBalancerSecurityGroup"}]),
])
def test_security_groups_open_only_what_each_tier_needs(group, expected):
    found = ingress(group)
    key = lambda rule: str(sorted(rule.items()))  # noqa: E731
    assert sorted(found, key=key) == sorted(expected, key=key), f"{group} ingress must be {expected}, found {found}"


def test_the_load_balancer_is_public_in_the_three_public_subnets():
    spec = props(ALB, "LoadBalancer")
    assert spec.get("Type", "application") == "application", "use an application load balancer"
    assert spec.get("Scheme") == "internet-facing", "Scheme must be internet-facing"
    assert stack().value(spec.get("Subnets")) == ["subnet-pub-a", "subnet-pub-b", "subnet-pub-c"], (
        "Subnets must be !Ref PublicSubnetIds")
    assert stack().value(spec.get("SecurityGroups")) == ["dojo-LoadBalancerSecurityGroup"]
    found = attributes(spec.get("LoadBalancerAttributes"))
    assert found.get("deletion_protection.enabled") == "true", "turn deletion protection on"
    assert found.get("routing.http.drop_invalid_header_fields.enabled") == "true", "drop invalid header fields"


def test_the_target_group_checks_the_apps_health():
    spec = props(TARGETS, "TargetGroup")
    assert spec.get("Protocol") == "HTTP" and int(spec.get("Port", 0)) == 8080, "HTTP on port 8080"
    assert spec.get("TargetType", "instance") == "instance", "TargetType instance"
    assert stack().value(spec.get("VpcId")) == "vpc-0a1b2c3d", "VpcId must be !Ref VpcId"
    assert spec.get("HealthCheckPath") == "/healthz", "HealthCheckPath must be /healthz"
    assert int(spec.get("HealthCheckIntervalSeconds", 30)) <= 10, "check health every 10 seconds at most"
    assert str((spec.get("Matcher") or {}).get("HttpCode")) == "200", "expect HttpCode 200"
    found = attributes(spec.get("TargetGroupAttributes"))
    assert found.get("deregistration_delay.timeout_seconds") == "30", "deregistration delay of 30 seconds"


def listeners():
    found = {}
    for name, res in stack().resources_of(LISTENER).items():
        spec = res["Properties"]
        assert ref(spec.get("LoadBalancerArn")) == "LoadBalancer", f"{name}: LoadBalancerArn must be !Ref LoadBalancer"
        found[int(spec.get("Port"))] = spec
    return found


def test_https_on_443_with_tls_13_forwards_to_the_app():
    spec = listeners().get(443)
    assert spec, "add an HTTPS listener on port 443"
    assert spec.get("Protocol") == "HTTPS", "port 443 must be HTTPS"
    assert spec.get("SslPolicy") == "ELBSecurityPolicy-TLS13-1-2-2021-06", "use the TLS 1.3 security policy"
    assert stack().value(spec.get("Certificates")) == [{"CertificateArn": CERTIFICATE}], (
        "Certificates must hold !Ref CertificateArn")
    actions = spec.get("DefaultActions", [])
    assert len(actions) == 1 and actions[0].get("Type") == "forward" and ref(actions[0].get("TargetGroupArn")) == \
        "TargetGroup", "forward to !Ref TargetGroup"


def test_plain_http_only_redirects():
    found = listeners()
    assert set(found) == {80, 443}, f"listen on 80 and 443 only, found {sorted(found)}"
    spec = found[80]
    actions = spec.get("DefaultActions", [])
    assert len(actions) == 1 and actions[0].get("Type") == "redirect", "port 80 only redirects"
    redirect = actions[0].get("RedirectConfig", {})
    assert redirect.get("Protocol") == "HTTPS" and str(redirect.get("Port")) == "443", "redirect to HTTPS on 443"
    assert redirect.get("StatusCode") == "HTTP_301", "a permanent redirect: StatusCode HTTP_301"


@pytest.fixture(scope="module")
def launch():
    return props("AWS::EC2::LaunchTemplate", "LaunchTemplate").get("LaunchTemplateData", {})


def test_instances_take_the_image_and_type_from_the_parameters(launch):
    assert ref(launch.get("ImageId")) == "ImageId", "ImageId must be !Ref ImageId"
    assert ref(launch.get("InstanceType")) == "InstanceType", "InstanceType must be !Ref InstanceType"
    assert stack().value(launch.get("SecurityGroupIds")) == ["dojo-AppSecurityGroup"], "use AppSecurityGroup"


def test_instances_only_answer_imdsv2(launch):
    metadata = launch.get("MetadataOptions", {})
    assert metadata.get("HttpTokens") == "required", "require IMDSv2: HttpTokens required"
    assert int(metadata.get("HttpPutResponseHopLimit", 1)) == 1, "HttpPutResponseHopLimit 1"
    assert metadata.get("HttpEndpoint", "enabled") == "enabled"


def test_the_root_volume_is_encrypted_gp3(launch):
    volumes = {m.get("DeviceName"): m.get("Ebs", {}) for m in launch.get("BlockDeviceMappings", [])}
    root = volumes.get("/dev/xvda")
    assert root, "declare the root volume /dev/xvda"
    assert root.get("Encrypted") is True, "encrypt the root volume"
    assert root.get("VolumeType") == "gp3" and int(root.get("VolumeSize", 0)) == 20, "a 20 GiB gp3 root volume"


@pytest.fixture(scope="module")
def group():
    return one("AWS::AutoScaling::AutoScalingGroup", "AutoScalingGroup")


def test_the_group_spans_the_three_private_subnets(group):
    spec = group["Properties"]
    assert stack().value(spec.get("VPCZoneIdentifier")) == ["subnet-app-a", "subnet-app-b", "subnet-app-c"], (
        "VPCZoneIdentifier must be !Ref PrivateSubnetIds")
    assert int(spec.get("MinSize")) == 3 and int(spec.get("MaxSize")) == 9, "MinSize 3 and MaxSize 9"
    template_ref = spec.get("LaunchTemplate", {})
    assert ref(template_ref.get("LaunchTemplateId")) == "LaunchTemplate", "LaunchTemplateId: !Ref LaunchTemplate"
    assert getatt(template_ref.get("Version")) == ("LaunchTemplate", "LatestVersionNumber"), (
        "Version: !GetAtt LaunchTemplate.LatestVersionNumber (a fixed version never rolls out changes)")


def test_unhealthy_apps_are_replaced(group):
    spec = group["Properties"]
    assert [ref(t) for t in spec.get("TargetGroupARNs", [])] == ["TargetGroup"], "TargetGroupARNs: [!Ref TargetGroup]"
    assert spec.get("HealthCheckType") == "ELB", "HealthCheckType ELB: replace instances the load balancer marks bad"
    assert int(spec.get("HealthCheckGracePeriod", 0)) > 0, "give new instances a HealthCheckGracePeriod"


def test_updates_replace_one_instance_at_a_time(group):
    rolling = (group.get("UpdatePolicy") or {}).get("AutoScalingRollingUpdate") or {}
    assert rolling, "add UpdatePolicy: AutoScalingRollingUpdate"
    assert int(rolling.get("MaxBatchSize", 1)) == 1, "MaxBatchSize 1"
    assert int(rolling.get("MinInstancesInService", 0)) >= 2, "keep at least 2 instances in service"


def test_the_group_scales_on_cpu():
    spec = props("AWS::AutoScaling::ScalingPolicy", "CpuScaling")
    assert ref(spec.get("AutoScalingGroupName")) == "AutoScalingGroup"
    assert spec.get("PolicyType") == "TargetTrackingScaling", "PolicyType TargetTrackingScaling"
    config = spec.get("TargetTrackingConfiguration", {})
    metric = config.get("PredefinedMetricSpecification", {}).get("PredefinedMetricType")
    assert metric == "ASGAverageCPUUtilization" and float(config.get("TargetValue", 0)) == 50, "track 50% average CPU"


def test_the_site_url_is_https():
    found = outputs(tpl()).get("SiteUrl")
    assert found, 'add the output SiteUrl: !Sub "https://${LoadBalancer.DNSName}"'
    assert stack().value(found["Value"]) == "https://dojo-LoadBalancer-1234567890.eu-west-1.elb.amazonaws.com"
