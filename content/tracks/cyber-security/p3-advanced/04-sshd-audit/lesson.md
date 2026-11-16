# SSH Config Audit: Find the Risky Settings

An auditor that greps for `PermitRootLogin yes` misses half the problems and reports some that
are not there. Write one that reads `sshd_config` the way `sshd` does. Press **Enter** to
create the kata folder and open it.
