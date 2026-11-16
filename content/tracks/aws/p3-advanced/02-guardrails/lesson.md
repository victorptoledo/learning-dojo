# Organization Guardrails: SCPs, Boundaries and Cross-Account Access

Acme runs AWS Organizations (`o-acme1234567`) with a development account
(`111122223333`), an analytics account (`444455556666`) and a data account
(`777788889999`). The platform team wants three things, and you write the policies:

1. **No surprises in other regions, no tampering with the audit trail**: an SCP on the
   workloads OU.
2. **Developers create their own app roles**, without being able to create an admin: a
   permissions boundary they must attach, and a policy that forces it.
3. **Analytics reads shared datasets** in the data account, and nobody outside the
   organization ever can: a bucket policy.

The tests send each request through the dojo's evaluator with the right layers: the SCP
next to `FullAWSAccess` at the OU (as AWS attaches it), the boundary on app roles that hold
an admin policy (the worst case), and both the caller's identity policy and the bucket
policy for cross-account calls.
