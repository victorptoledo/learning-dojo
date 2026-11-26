from dojo_aws import resources, template


def test_the_key_rotates():
    key = next(iter(resources(template("backup.yaml"), "AWS::KMS::Key").values()))
    assert key["Properties"].get("EnableKeyRotation") is True
