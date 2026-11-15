---
from: javascript
status: draft
concept: "Getting and managing a development machine"
from_way: "npm or pnpm install packages per project into node_modules, nvm picks the Node version, and the browser or Node is the platform."
native_way: "apt installs system packages; Node itself is best installed from NodeSource, nvm or a container image, while project packages stay in node_modules."
sensei_tip: "The Node.js in Debian's repositories is stable but older. For applications pin the version with an official container image (node:22-bookworm) or nvm, not with whatever apt gives you."
---

| JavaScript / TypeScript | Linux |
| --- | --- |
| npm install -g pm2 | sudo apt install <tool>, or npm -g as your user |
| nvm use 22 | a container image such as node:22, or nvm on the server |
| package.json scripts | shell scripts and systemd units |
| npx some-cli | the tool from apt, or run in docker run --rm |
