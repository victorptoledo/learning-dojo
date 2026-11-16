import pytest

from dojo_aws import outputs, resource, template


@pytest.fixture
def bucket():
    _, found = resource(template("template.yaml"), "AWS::S3::Bucket")
    return found


def props(bucket):
    return bucket.get("Properties", {})


def test_keeps_the_bucket_when_the_stack_is_deleted_or_the_bucket_replaced(bucket):
    assert bucket.get("DeletionPolicy") == "Retain", "set DeletionPolicy: Retain on the bucket"
    assert bucket.get("UpdateReplacePolicy") == "Retain", "set UpdateReplacePolicy: Retain on the bucket"


def test_turns_on_versioning(bucket):
    status = props(bucket).get("VersioningConfiguration", {}).get("Status")
    assert status == "Enabled", "VersioningConfiguration.Status must be Enabled"


def test_encrypts_new_objects_with_sse_s3_by_default(bucket):
    rules = props(bucket).get("BucketEncryption", {}).get("ServerSideEncryptionConfiguration", [])
    algorithms = [rule.get("ServerSideEncryptionByDefault", {}).get("SSEAlgorithm") for rule in rules]
    assert algorithms == ["AES256"], f"expected one rule with SSEAlgorithm AES256, found {algorithms}"


@pytest.mark.parametrize("setting", ["BlockPublicAcls", "IgnorePublicAcls", "BlockPublicPolicy",
                                     "RestrictPublicBuckets"])
def test_blocks_public_access(bucket, setting):
    block = props(bucket).get("PublicAccessBlockConfiguration", {})
    assert block.get(setting) is True, f"PublicAccessBlockConfiguration.{setting} must be true"


def test_does_not_hard_code_a_bucket_name(bucket):
    assert "BucketName" not in props(bucket), (
        "leave BucketName out: names are global across all AWS accounts, and a fixed name "
        "stops CloudFormation from replacing the bucket"
    )


def test_tags_the_bucket_with_the_project(bucket):
    tags = {tag.get("Key"): tag.get("Value") for tag in props(bucket).get("Tags", [])}
    assert tags.get("project") == "dojo", f"expected the tag project=dojo, found {tags}"


def test_outputs_the_bucket_name_and_arn():
    found = outputs(template("template.yaml"))
    assert found.get("BucketName", {}).get("Value") == {"Ref": "ReportsBucket"}, (
        "Outputs.BucketName.Value must be !Ref ReportsBucket"
    )
    assert found.get("BucketArn", {}).get("Value") == {"Fn::GetAtt": ["ReportsBucket", "Arn"]}, (
        "Outputs.BucketArn.Value must be !GetAtt ReportsBucket.Arn"
    )
