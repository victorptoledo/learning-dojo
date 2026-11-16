# Configuration Management: Idempotence, Drift and Immutable Servers

A server is configured by its packages, files, users, services and kernel settings. Change them
by hand and nobody can say what a server *should* look like; after a few years every machine
is a unique "snowflake" that nobody dares to touch. **Configuration management** describes the
desired state in code and lets a tool bring machines to it, again and again.

## Desired state, not steps

A script describes **steps**: "append this line, start that service". Run it twice and the line
is there twice. Configuration management describes the **state**: "this line is present, this
service is running". The tool checks each resource and changes only what differs.

That property is **idempotence**: applying the same configuration once or ten times leaves the
same result, and the second run reports "no changes". It is what lets you run configuration
management every 30 minutes to repair anything that drifted.

| Script (imperative) | Desired state (declarative) |
| --- | --- |
| `echo "10.0.0.5 db" >> /etc/hosts` | the line `10.0.0.5 db` is in `/etc/hosts` |
| `useradd deploy` (fails the second time) | the user `deploy` exists |
| `systemctl restart nginx` every run | restart nginx only when its config changed |

## Ansible's model

**Ansible** is agentless: it connects over SSH (or WinRM), copies small modules and runs them.

```ini
# inventory.ini
[web]
web-01.internal
web-02.internal

[web:vars]
app_port=8080
```

```yaml
# site.yml
- name: Configure the web servers
  hosts: web
  become: true
  tasks:
    - name: nginx is installed
      ansible.builtin.apt:
        name: nginx
        state: present
        update_cache: true

    - name: the site configuration is in place
      ansible.builtin.template:
        src: templates/site.conf.j2
        dest: /etc/nginx/conf.d/site.conf
        mode: "0644"
      notify: reload nginx

    - name: nginx runs and starts at boot
      ansible.builtin.service:
        name: nginx
        state: started
        enabled: true

  handlers:
    - name: reload nginx
      ansible.builtin.service:
        name: nginx
        state: reloaded
```

- **Modules** (`apt`, `template`, `copy`, `lineinfile`, `user`, `service`) are idempotent; the
  `shell` and `command` modules are not, unless you add `creates:` or `changed_when:`.
- **Handlers** run once at the end, and only if a task that notifies them reported a change:
  nginx is reloaded only when its configuration really changed.
- **Variables** come from the inventory, `group_vars/`, `host_vars/` and roles; templates use
  Jinja2.
- **Roles** package tasks, templates and defaults for reuse; collections ship them on Ansible
  Galaxy.
- `ansible-playbook site.yml --check --diff` is a dry run that shows what would change.

Puppet, Chef and Salt use agents on each machine that pull their configuration from a server;
the ideas are the same.

## Drift

**Drift** is the gap between the declared state and the real one: a hot fix made over SSH, a
package updated by hand, a file edited "just for now". Drift is dangerous because the next
deploy or rebuild silently undoes it, or fails because of it. Detect it:

- run configuration management in **check mode** on a schedule and alert on any change;
- for infrastructure, `terraform plan` on a schedule (Phase 2);
- for Kubernetes, a GitOps controller that shows "OutOfSync" (Phase 2).

Fix drift by putting the change in code, or by reverting it; never by leaving it.

## Mutable versus immutable servers

Configuration management updates servers **in place** (mutable). The alternative is
**immutable infrastructure**: bake everything into an image (a container image, or a VM image
with Packer), and replace servers instead of changing them.

| | Mutable, managed in place | Immutable, replaced |
| --- | --- | --- |
| a change | the tool edits running servers | build a new image, roll it out, delete old servers |
| drift | possible, must be detected | impossible by construction (nobody logs in) |
| rollback | apply the old configuration, hope it reverses cleanly | deploy the previous image |
| speed of change | seconds per server | minutes to build and roll |
| still needed for | databases, legacy hosts, network devices, the image build itself | stateless services |

Most modern platforms are mostly immutable: containers on Kubernetes, VM images in auto-scaling
groups. Configuration management survives in building those images, configuring the hosts
underneath, and managing the long-lived machines that cannot be replaced.

## Secrets

Configuration needs passwords, keys and certificates; none of them belong in Git in plain text.
Use a secrets manager (HashiCorp Vault, AWS Secrets Manager, Azure Key Vault, Google Secret
Manager) and fetch at run time, or at least encrypt them in the repository (Ansible Vault,
SOPS). Rotate them, and audit who reads them.

## Summary

- Configuration management declares the desired state; the tool changes only what differs.
- Idempotence: a second run changes nothing, so it can run often to repair drift.
- Ansible: inventory, playbooks of tasks using idempotent modules, handlers on change,
  variables, roles, `--check --diff`.
- Drift is detected with scheduled dry runs and fixed in code.
- Immutable images replace most in-place management; configuration management still builds
  and manages what cannot be replaced.

## Further reading

- Ansible documentation, getting started: <https://docs.ansible.com/ansible/latest/getting_started/index.html>
- Ansible check mode and diff mode: <https://docs.ansible.com/ansible/latest/playbook_guide/playbooks_checkmode.html>
- Martin Fowler, "SnowflakeServer" and "ImmutableServer": <https://martinfowler.com/bliki/ImmutableServer.html>
- Packer documentation: <https://developer.hashicorp.com/packer/docs>
- *Infrastructure as Code*, 3rd edition, Kief Morris (O'Reilly, 2025)
