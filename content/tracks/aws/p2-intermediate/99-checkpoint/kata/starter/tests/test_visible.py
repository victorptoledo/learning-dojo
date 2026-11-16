from dojo_aws import resources, template


def test_declares_the_two_functions():
    functions = sorted(resources(template("template.yaml"), "AWS::Serverless::Function"))
    assert functions == ["ApiFunction", "WorkerFunction"]
