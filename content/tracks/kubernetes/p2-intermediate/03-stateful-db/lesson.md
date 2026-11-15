# A Database with a StatefulSet

Run a three-member PostgreSQL as a StatefulSet: stable names, a disk per member, the password
from a Secret and protection against drains. Press **Enter** to create the kata folder, then
complete `postgres.yaml`. A real cluster also needs replication between the members, which an
operator would add (Phase 4); this kata is about the Kubernetes side.
