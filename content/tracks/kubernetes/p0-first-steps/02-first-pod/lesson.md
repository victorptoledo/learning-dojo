# Hello, Pod: Your First Manifest

Your first manifest: a Pod that prints a greeting. Press **Enter** to create the kata folder
and open it, edit `pod.yaml`, and save. The dojo validates the YAML against the Kubernetes
1.36 schemas and checks the objects with bats, no cluster needed. If you have one, try
`kubectl apply -f pod.yaml` and `kubectl logs hello` too.
