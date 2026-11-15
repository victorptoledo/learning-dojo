---
from: java
status: draft
concept: "Operating the platform, not only the app"
from_way: "Ops upgrades the application servers and the JDK on a schedule, and releases go out in change windows."
native_way: "The platform is upgraded continuously within the skew policy, releases flow through Git and GitOps, and policies and SLOs replace change boards."
sensei_tip: "Treat removed Kubernetes APIs like removed JDK APIs: scan for them before every upgrade, the way jdeprscan finds deprecated Java APIs."
---

| Java world | Kubernetes |
| --- | --- |
| a JDK and app server upgrade plan | a cluster upgrade, one minor at a time |
| `jdeprscan` | Pluto or kubent for removed APIs |
| a change advisory board | pull requests and GitOps |
| JMX dashboards | Prometheus and SLO alerts |
