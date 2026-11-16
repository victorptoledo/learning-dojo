from dojo_aws import iam

FULL_ACCESS = {"Version": "2012-10-17", "Statement": [{"Effect": "Allow", "Action": "*", "Resource": "*"}]}
DEVELOPER = "arn:aws:iam::111122223333:role/developer"


def test_developers_can_work_in_ireland():
    request = iam.Request(DEVELOPER, "dynamodb:PutItem",
                          "arn:aws:dynamodb:eu-west-1:111122223333:table/Orders",
                          context={"aws:RequestedRegion": "eu-west-1", "aws:PrincipalArn": DEVELOPER})
    found = iam.evaluate(request, identity=[iam.load("developer-policy.json")],
                         scps=[FULL_ACCESS, [FULL_ACCESS, iam.load("scp-guardrails.json")]])
    assert found == iam.ALLOW
