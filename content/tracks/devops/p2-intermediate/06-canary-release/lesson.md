# A Canary Release with the Gateway API

Release a new version to 10% of the traffic: a canary Deployment next to the stable one,
Services that each select one track, and an HTTPRoute that splits traffic by weight and lets
testers opt in with a header. Press **Enter** to create the kata folder and open it, edit
the three `.yaml` files and save. The dojo validates them against the Kubernetes and Gateway
API schemas and checks them with bats, no cluster needed.
