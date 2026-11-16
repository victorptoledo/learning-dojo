# EC2: Instances, AMIs, EBS and Auto Scaling

**Amazon EC2** (Elastic Compute Cloud) rents virtual machines, called **instances**. It is
the most flexible compute on AWS and the one where you own the most: the operating system,
its patches, the agents and everything you install (the shared responsibility model).

## Instance types

A type such as `m7g.large` reads as **family** (`m`, general purpose), **generation** (`7`),
**options** (`g` = AWS Graviton, Arm; `i` = Intel; `a` = AMD; `d` = local NVMe disks;
`n` = more network) and **size** (`large` = 2 vCPUs and 8 GiB for `m`).

| Family | For |
| --- | --- |
| `t` (t4g, t3) | burstable: small, mostly idle workloads; CPU credits cap sustained use |
| `m` | general purpose: web apps, APIs, most services |
| `c` | compute optimised: CPU-bound work, batch, encoding |
| `r`, `x` | memory optimised: caches, in-memory analytics, large JVM heaps |
| `i`, `d` | storage optimised: fast local NVMe |
| `p`, `g`, `inf`, `trn` | GPUs and ML accelerators |

Graviton (Arm) instances usually give better price-performance for Java, Node.js, Python,
.NET and Go workloads: build multi-architecture images or artifacts and measure. Right-size
from CloudWatch metrics (CPU, memory through the CloudWatch agent), not from the old server.

## AMIs, user data and instance profiles

An **AMI** (Amazon Machine Image) is the disk image an instance boots from: Amazon Linux
2023, Ubuntu, Windows, or your own image baked with EC2 Image Builder or Packer. **User
data** is a script run once at first boot by cloud-init: install the agent, fetch the
artifact, start the service. Bake what is slow and stable into the AMI; use user data for
what changes per environment.

An **instance profile** attaches an IAM role to an instance. The SDK reads temporary
credentials for it from the **instance metadata service** at `169.254.169.254`. Require
**IMDSv2** (`HttpTokens: required`): it needs a session token obtained by a `PUT`, which
blocks the classic SSRF attack where a vulnerable app is tricked into fetching the
credentials URL.

```yaml
LaunchTemplate:
  Type: AWS::EC2::LaunchTemplate
  Properties:
    LaunchTemplateData:
      ImageId: "{{resolve:ssm:/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-arm64}}"
      InstanceType: m7g.large
      IamInstanceProfile: { Arn: !GetAtt AppInstanceProfile.Arn }
      MetadataOptions: { HttpTokens: required, HttpPutResponseHopLimit: 1 }
      UserData: !Base64 |
        #!/bin/bash
        dnf install -y java-21-amazon-corretto-headless
        aws s3 cp s3://acme-artifacts/api-1.8.0.jar /opt/api/api.jar
        systemctl enable --now api
```

## Storage: EBS, instance store and EFS

| Storage | What it is | Survives a stop? |
| --- | --- | --- |
| **EBS** volume | a network block device in **one AZ**: `gp3` (default, set IOPS and throughput), `io2` (high, steady IOPS), `st1`/`sc1` (throughput HDD) | yes; deleted on termination if `DeleteOnTermination` |
| **Instance store** | NVMe disks on the host itself, very fast | **no**: lost on stop, hibernation or host failure |
| **EFS** | a managed NFS file system shared across instances and AZs | yes |
| **S3** | object storage over HTTP (next lesson) | yes |

**Snapshots** copy an EBS volume to S3-backed storage, incrementally; they are how you
back up, copy volumes across AZs or regions, and build AMIs. Encrypt volumes by default
(an account setting per region) with KMS.

## Purchase options

- **On-Demand**: per second, no commitment.
- **Savings Plans** and **Reserved Instances**: up to ~70 % less for a 1- or 3-year commitment to a spend (Savings Plans) or an instance type (RIs).
- **Spot**: spare capacity at up to ~90 % less, reclaimed with a two-minute warning;
  only for interruptible work (batch, CI, stateless workers in a mixed fleet).
- **Dedicated Hosts**: a whole physical server, for licensing or compliance.

Phase 3 covers cost in depth.

## Auto Scaling groups

A single instance is a single point of failure. An **Auto Scaling group** (ASG) keeps a
desired number of instances from a **launch template**, spread over the subnets (AZs) you
give it:

- **Health checks**: EC2 status checks, plus load-balancer health checks when it sits
  behind an ALB; unhealthy instances are terminated and replaced.
- **Scaling policies**: target tracking (keep average CPU at 50 %, or requests per target
  at 1000), step scaling, scheduled scaling, predictive scaling.
- **Instance refresh** rolls a new launch template version through the group.
- **Lifecycle hooks** let an instance finish work (drain, upload logs) before it goes.

Treat instances as cattle: nothing on them that you cannot rebuild from the AMI and user
data. Connect for debugging with **Systems Manager Session Manager** (no SSH port, no keys,
every session logged) rather than opening port 22.

## Summary

- An instance type is family, generation, options and size; measure, and try Graviton.
- AMIs plus user data build instances; an instance profile gives them a role; require IMDSv2.
- EBS lives in one AZ and survives stops; instance store does not; share files with EFS or S3.
- On-Demand, Savings Plans, Spot: match the purchase option to how interruptible the work is.
- Auto Scaling groups from launch templates, across AZs, replace and scale instances.

## Further reading

- Amazon EC2 instance types: <https://docs.aws.amazon.com/ec2/latest/instancetypes/instance-types.html>
- Use IMDSv2: <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/configuring-instance-metadata-service.html>
- Amazon EBS volume types: <https://docs.aws.amazon.com/ebs/latest/userguide/ebs-volume-types.html>
- Instance store: <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/InstanceStorage.html>
- Amazon EC2 Auto Scaling user guide: <https://docs.aws.amazon.com/autoscaling/ec2/userguide/what-is-amazon-ec2-auto-scaling.html>
- AWS Systems Manager Session Manager: <https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager.html>
