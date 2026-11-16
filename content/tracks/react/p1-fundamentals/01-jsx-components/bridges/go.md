---
from: go
status: draft
concept: "Templates as typed functions"
from_way: "html/template uses {{.Field}} and {{template \"name\" .}} with untyped data; templ (a Go library) compiles components to Go functions."
native_way: "React components are TypeScript functions with typed props; JSX tags call them, and the compiler checks every prop."
sensei_tip: "If you have used templ, React will feel natural: components are functions, and composition is just calling one from another with children."
---

| Go | React |
| --- | --- |
| `{{.Name}}` | `{name}` |
| `{{template "avatar" .User}}` | `<Avatar user={user} />` |
| templ `templ Card(title string)` | `function Card({ title }: CardProps)` |
| `{{block "content" .}}` | `children` |
| html/template escapes by default | JSX escapes by default |
| `template.HTML` (trusted) | `dangerouslySetInnerHTML` |
