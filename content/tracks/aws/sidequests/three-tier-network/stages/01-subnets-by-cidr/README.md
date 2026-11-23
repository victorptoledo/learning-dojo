The starter has the `VpcCidr` parameter and the VPC. Add the subnets, nine of them: three
tiers (`Public`, `App`, `Data`) in three zones (`A`, `B`, `C`), named `PublicSubnetA` to
`DataSubnetC`.

| Piece | Rule |
| --- | --- |
| VPC | `EnableDnsSupport: true` and `EnableDnsHostnames: true` (interface endpoints need both, in stage 4) |
| zones | the `A` subnets in `!Select [0, !GetAZs ""]`, `B` in index 1, `C` in index 2; never a zone name typed in |
| blocks | nine `/20` blocks cut from the VPC's own block: `!Select [N, !Cidr [!GetAtt Vpc.CidrBlock, 9, 12]]`, with `N` = 0, 1, 2 for `PublicSubnetA`, `B`, `C`; 3, 4, 5 for the app subnets; 6, 7, 8 for the data subnets |
| VPC | every subnet has `VpcId: !Ref Vpc` |
| public IPs | `MapPublicIpOnLaunch: true` on the public subnets only |
| tags | each subnet has the tag `tier` = `public`, `app` or `data` |
| outputs | `VpcId`, and `PublicSubnetIds`, `AppSubnetIds`, `DataSubnetIds`, each `!Join [",", [A, B, C]]` of that tier; all four exported as `!Sub "${AWS::StackName}-<output name>"` so other stacks can `!ImportValue` them |

With the default `10.40.0.0/16`, `PublicSubnetA` is `10.40.0.0/20` and `DataSubnetC` is
`10.40.128.0/20`. The tests also deploy the template with `VpcCidr=172.20.0.0/16`: every
subnet must move with it.

> [!TIP]
> `!Cidr [block, count, bits]` returns `count` blocks of `bits` host bits each: in a `/16`,
> 12 host bits make `/20` blocks (4,096 addresses, 4,091 usable, since AWS keeps five in every
> subnet). Leaving blocks 9 to 15 free keeps room for a fourth tier or zone later.
