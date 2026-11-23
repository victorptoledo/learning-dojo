import pytest

from dojo_aws import getatt, outputs, ref, resources, template
from dojo_aws.stack import Stack


@pytest.fixture(scope="module")
def tpl():
    return template("site.yaml")


@pytest.fixture(scope="module")
def bucket(tpl):
    found = resources(tpl, "AWS::S3::Bucket")
    assert "SiteBucket" in found, "declare the bucket SiteBucket (AWS::S3::Bucket)"
    return found["SiteBucket"]


def props(res):
    return res.get("Properties", {}) or {}


def test_the_bucket_gets_a_generated_name(bucket):
    assert "BucketName" not in props(bucket), (
        "leave BucketName out: CloudFormation picks a unique name, and replacing the bucket stays possible")


def test_every_public_access_setting_is_blocked(bucket):
    block = props(bucket).get("PublicAccessBlockConfiguration", {})
    expected = {"BlockPublicAcls": True, "BlockPublicPolicy": True,
                "IgnorePublicAcls": True, "RestrictPublicBuckets": True}
    assert block == expected, f"PublicAccessBlockConfiguration must set all four to true, found {block}"


def test_acls_are_disabled(bucket):
    rules = props(bucket).get("OwnershipControls", {}).get("Rules", [])
    assert rules == [{"ObjectOwnership": "BucketOwnerEnforced"}], (
        f"OwnershipControls needs the single rule ObjectOwnership: BucketOwnerEnforced, found {rules}")


def test_objects_are_encrypted_with_sse_s3(bucket):
    rules = props(bucket).get("BucketEncryption", {}).get("ServerSideEncryptionConfiguration", [])
    assert len(rules) == 1, "BucketEncryption needs one ServerSideEncryptionConfiguration rule"
    default = rules[0].get("ServerSideEncryptionByDefault", {})
    assert default.get("SSEAlgorithm") == "AES256", f"SSEAlgorithm must be AES256, found {default}"


def test_versioning_is_enabled(bucket):
    assert props(bucket).get("VersioningConfiguration") == {"Status": "Enabled"}, (
        "VersioningConfiguration must be { Status: Enabled }")


def test_lifecycle_expires_old_versions_and_aborts_stale_uploads(bucket):
    rules = [r for r in props(bucket).get("LifecycleConfiguration", {}).get("Rules", [])
             if r.get("Status") == "Enabled"]
    noncurrent = [r["NoncurrentVersionExpiration"] for r in rules if "NoncurrentVersionExpiration" in r]
    assert any(n.get("NoncurrentDays") == 30 for n in noncurrent), (
        "add an enabled lifecycle rule with NoncurrentVersionExpiration: { NoncurrentDays: 30 }")
    aborts = [r["AbortIncompleteMultipartUpload"] for r in rules if "AbortIncompleteMultipartUpload" in r]
    assert any(a.get("DaysAfterInitiation") == 7 for a in aborts), (
        "add AbortIncompleteMultipartUpload: { DaysAfterInitiation: 7 } to an enabled rule")
    for rule in rules:
        assert "ExpirationInDays" not in rule and "ExpirationDate" not in rule, (
            "never expire current versions: that would delete the live site")


def test_the_bucket_outlives_the_stack(bucket):
    assert bucket.get("DeletionPolicy") == "Retain", "set DeletionPolicy: Retain on SiteBucket"
    assert bucket.get("UpdateReplacePolicy") == "Retain", "set UpdateReplacePolicy: Retain on SiteBucket"


def test_outputs_name_the_bucket_and_its_arn(tpl):
    found = outputs(tpl)
    assert "BucketName" in found and ref(found["BucketName"].get("Value")) == "SiteBucket", (
        "add the output BucketName: !Ref SiteBucket")
    assert "BucketArn" in found and getatt(found["BucketArn"].get("Value")) == ("SiteBucket", "Arn"), (
        "add the output BucketArn: !GetAtt SiteBucket.Arn")
    assert Stack(tpl).value(found["BucketArn"]["Value"]) == "arn:aws:s3:::dojo-sitebucket"
