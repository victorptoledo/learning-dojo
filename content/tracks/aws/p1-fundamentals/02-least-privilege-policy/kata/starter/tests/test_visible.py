from dojo_aws import iam

ROLE = "arn:aws:iam::111122223333:role/invoice-service"
TLS = {"aws:SecureTransport": "true"}


def test_the_service_can_read_an_invoice():
    request = iam.Request(ROLE, "s3:GetObject", "arn:aws:s3:::acme-invoices/invoices/2026/0042.pdf",
                          context=TLS)
    assert iam.evaluate(request, identity=[iam.load("policy.json")]) == iam.ALLOW
