import pytest

from dojo_aws import getatt, iam, outputs, resources, template
from dojo_aws.stack import ACCOUNT, Stack

BUCKET = "arn:aws:s3:::dojo-sitebucket"
DISTRIBUTION = f"arn:aws:cloudfront::{ACCOUNT}:distribution/dojo-SiteDistribution"
OTHER_DISTRIBUTION = f"arn:aws:cloudfront::{ACCOUNT}:distribution/E2OTHERSITE"
CLOUDFRONT = "cloudfront.amazonaws.com"
ADMIN = f"arn:aws:iam::{ACCOUNT}:role/admin"
OUTSIDER = "arn:aws:iam::444455556666:user/eve"
ADMIN_POLICY = {"Version": "2012-10-17",
                "Statement": [{"Effect": "Allow", "Action": "s3:*", "Resource": "*"}]}
CACHING_OPTIMIZED = "658327ea-f89d-4fab-a63d-7e88639e58f6"


@pytest.fixture(scope="module")
def stack():
    return Stack(template("site.yaml"))


def one(stack, type_, name):
    found = stack.resources_of(type_)
    assert name in found, f"declare {name} ({type_})"
    return found[name].get("Properties", {}) or {}


@pytest.fixture(scope="module")
def config(stack):
    return one(stack, "AWS::CloudFront::Distribution", "SiteDistribution").get("DistributionConfig", {})


@pytest.fixture(scope="module")
def policy(stack):
    props = one(stack, "AWS::S3::BucketPolicy", "SiteBucketPolicy")
    assert stack.value(props.get("Bucket")) == "dojo-sitebucket", "SiteBucketPolicy: Bucket must be !Ref SiteBucket"
    return stack.value(props.get("PolicyDocument"))


def test_origin_access_control_signs_every_request(stack):
    oac = one(stack, "AWS::CloudFront::OriginAccessControl", "SiteOriginAccessControl")
    config = oac.get("OriginAccessControlConfig", {})
    assert config.get("OriginAccessControlOriginType") == "s3", "OriginAccessControlOriginType must be s3"
    assert config.get("SigningBehavior") == "always", "SigningBehavior must be always"
    assert config.get("SigningProtocol") == "sigv4", "SigningProtocol must be sigv4"


def test_the_distribution_reads_the_bucket_through_the_oac(stack, config):
    assert config.get("Enabled") is True, "the distribution must be Enabled: true"
    assert config.get("DefaultRootObject") == "index.html", "DefaultRootObject must be index.html"
    origins = config.get("Origins", [])
    assert len(origins) == 1, f"use one origin, found {len(origins)}"
    origin = origins[0]
    assert stack.value(origin.get("DomainName")) == "dojo-sitebucket.s3.eu-west-1.amazonaws.com", (
        "the origin's DomainName must be !GetAtt SiteBucket.RegionalDomainName")
    assert getatt(origin.get("OriginAccessControlId")) == ("SiteOriginAccessControl", "Id"), (
        "OriginAccessControlId must be !GetAtt SiteOriginAccessControl.Id")
    assert origin.get("S3OriginConfig") == {"OriginAccessIdentity": ""}, (
        'S3OriginConfig must be { OriginAccessIdentity: "" }: no legacy origin access identity')


def test_the_default_behaviour_is_https_read_only_and_cached(config):
    behaviour = config.get("DefaultCacheBehavior", {})
    origin_id = (config.get("Origins") or [{}])[0].get("Id")
    assert behaviour.get("TargetOriginId") == origin_id, "TargetOriginId must be the origin's Id"
    assert behaviour.get("ViewerProtocolPolicy") == "redirect-to-https", "ViewerProtocolPolicy: redirect-to-https"
    methods = set(behaviour.get("AllowedMethods", ["GET", "HEAD"]))
    assert methods <= {"GET", "HEAD", "OPTIONS"}, f"a static site only needs GET and HEAD, found {sorted(methods)}"
    assert behaviour.get("Compress") is True, "set Compress: true"
    assert behaviour.get("CachePolicyId") == CACHING_OPTIMIZED, "use the managed CachingOptimized CachePolicyId"
    assert "ForwardedValues" not in behaviour, "ForwardedValues is the legacy cache setting: use the cache policy"


def test_missing_pages_and_denied_keys_both_show_the_404_page(config):
    answers = {r.get("ErrorCode"): r for r in config.get("CustomErrorResponses", [])}
    for code in (403, 404):
        assert code in answers, f"add a CustomErrorResponse for ErrorCode {code}"
        assert answers[code].get("ResponsePagePath") == "/404.html", f"{code} must show /404.html"
        assert answers[code].get("ResponseCode") == 404, f"{code} must answer ResponseCode 404"


def test_modern_protocols_and_the_cheapest_edge(config):
    assert config.get("HttpVersion") == "http2and3", "HttpVersion: http2and3"
    assert config.get("PriceClass") == "PriceClass_100", "PriceClass: PriceClass_100"


def test_the_bucket_policy_is_well_formed(policy):
    assert iam.validate(policy, "resource") == [], "; ".join(iam.validate(policy, "resource"))


def request(principal, action, key="index.html", source=DISTRIBUTION, tls=True):
    context = {"aws:SecureTransport": "true" if tls else "false"}
    if source:
        context["aws:SourceArn"] = source
    resource = BUCKET if key is None else f"{BUCKET}/{key}"
    return iam.Request(principal=principal, action=action, resource=resource, context=context)


@pytest.mark.parametrize("case,req,identity,expected", [
    ("cloudfront reads a page", request(CLOUDFRONT, "s3:GetObject"), None, iam.ALLOW),
    ("cloudfront reads an asset", request(CLOUDFRONT, "s3:GetObject", "assets/app.3f9c.css"), None, iam.ALLOW),
    ("another distribution of the account", request(CLOUDFRONT, "s3:GetObject", source=OTHER_DISTRIBUTION),
     None, iam.IMPLICIT_DENY),
    ("cloudfront with no source ARN", request(CLOUDFRONT, "s3:GetObject", source=None), None, iam.IMPLICIT_DENY),
    ("cloudfront lists the bucket", request(CLOUDFRONT, "s3:ListBucket", key=None), None, iam.IMPLICIT_DENY),
    ("cloudfront uploads", request(CLOUDFRONT, "s3:PutObject"), None, iam.IMPLICIT_DENY),
    ("cloudfront deletes", request(CLOUDFRONT, "s3:DeleteObject"), None, iam.IMPLICIT_DENY),
    ("cloudfront over plain HTTP", request(CLOUDFRONT, "s3:GetObject", tls=False), None, iam.EXPLICIT_DENY),
    ("another account", request(OUTSIDER, "s3:GetObject", source=None), None, iam.IMPLICIT_DENY),
    ("the admin over TLS", request(ADMIN, "s3:PutObject", source=None), [ADMIN_POLICY], iam.ALLOW),
    ("the admin over plain HTTP", request(ADMIN, "s3:GetObject", source=None, tls=False), [ADMIN_POLICY],
     iam.EXPLICIT_DENY),
    ("the admin lists over plain HTTP", request(ADMIN, "s3:ListBucket", key=None, source=None, tls=False),
     [ADMIN_POLICY], iam.EXPLICIT_DENY),
])
def test_only_this_distribution_reads_and_only_over_tls(policy, case, req, identity, expected):
    found = iam.evaluate(req, identity=identity, resource_policy=policy)
    assert found == expected, f"{case}: expected {expected}, the policy gives {found}"


def test_outputs_give_the_distribution_and_its_url(stack):
    found = outputs(template("site.yaml"))
    assert "DistributionId" in found, "add the output DistributionId: !Ref SiteDistribution"
    assert stack.value(found["DistributionId"]["Value"]) == "dojo-SiteDistribution", (
        "DistributionId must be !Ref SiteDistribution")
    assert "SiteUrl" in found, 'add the output SiteUrl: !Sub "https://${SiteDistribution.DomainName}"'
    assert stack.value(found["SiteUrl"]["Value"]) == "https://d111111abcdef8.cloudfront.net", (
        'SiteUrl must be !Sub "https://${SiteDistribution.DomainName}"')


def test_nothing_else_reads_the_bucket(stack):
    assert len(resources(template("site.yaml"), "AWS::S3::BucketPolicy")) == 1, "use one bucket policy"
    assert not stack.resources_of("AWS::CloudFront::CloudFrontOriginAccessIdentity"), (
        "do not add a legacy origin access identity: the OAC replaces it")
