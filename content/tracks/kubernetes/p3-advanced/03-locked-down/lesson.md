# Lock Down a Namespace

The `inventory` service works, and it runs exactly as a default cluster allows: as root,
with a writable file system, a ServiceAccount that can do anything, and an open network.
Press **Enter** to create the kata folder and harden it, file by file, until it meets the
restricted Pod Security Standard, least-privilege RBAC and default-deny networking.
