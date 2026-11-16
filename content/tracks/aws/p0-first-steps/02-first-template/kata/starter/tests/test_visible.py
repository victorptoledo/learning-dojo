from dojo_aws import resource, template


def test_declares_one_bucket_named_reports_bucket():
    name, _ = resource(template("template.yaml"), "AWS::S3::Bucket")
    assert name == "ReportsBucket"
