Now the routes. A NAT gateway costs money every hour it exists, so `dev` gets one and `prod`
gets one per zone: losing a zone in `prod` must not cut the other two off the internet.

1. Add the parameter `Environment` (`AllowedValues: [dev, prod]`, `Default: dev`) and the
   condition `IsProd`.
2. Public tier: an `InternetGateway`, its `VPCGatewayAttachment` (call it
   `GatewayAttachment`), and one route table for the three public subnets with
   `0.0.0.0/0` → the internet gateway. That route needs `DependsOn: GatewayAttachment`: a
   route to a gateway that is not attached yet fails the deployment.
3. NAT: `NatGatewayA` (with `NatEipA`, `Domain: vpc`) always exists, in `PublicSubnetA`.
   `NatGatewayB` and `NatGatewayC`, and their Elastic IPs, exist only when `IsProd`, each in
   the public subnet of its zone.
4. App tier: one route table **per zone**. Its `0.0.0.0/0` route goes to the NAT gateway of
   the same zone in `prod`, and to `NatGatewayA` in `dev` (`!If`). Never to the internet
   gateway.
5. Data tier: a route table with **no** default route, IPv4 or IPv6, for the three data
   subnets. Local traffic inside the VPC needs no route.

The tests deploy the template twice, with `Environment=dev` and `Environment=prod`, and in
each one follow every subnet's route table. They also resolve every `!Ref` and `!GetAtt` of
the resources that exist: a resource that refers to one whose condition is false fails, as it
would in CloudFormation.

> [!TIP]
> A resource with `Condition: IsProd` simply does not exist in `dev`, so nothing may refer
> to it there. `!If [IsProd, !Ref NatGatewayB, !Ref NatGatewayA]` is fine: CloudFormation
> only resolves the branch it picks.
