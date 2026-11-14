# 🥋 Learning Dojo

Uma plataforma de aprendizado gamificada e extensível diretamente no terminal (TUI), construída em **Rust** com [`ratatui`](https://ratatui.rs/).

Inspirada no [roadmap.sh](https://roadmap.sh/), com **árvore de habilidades interativa**, **sistema de faixas e graus**, **streak diário global** e uma camada de **tradução mental (Rosetta Stone)** comparando a visão tradicional do ecossistema Java com cada tecnologia.

---

## 🚀 Como Executar

Abra o seu terminal (PowerShell, Windows Terminal ou VS Code) na pasta do projeto e execute:

```powershell
cd c:\dev\learning-dojo
cargo run
```

---

## 🗺️ Trilhas Oficiais Disponíveis (do roadmap.sh)

Aperte **`M`** a qualquer momento dentro do app para abrir o **Menu de Jornadas** e escolher seu tatame:

| Ícone | Trilha | Categoria | Foco Principal |
| :---: | :--- | :--- | :--- |
| 🦀 | **Rust** (`roadmap.sh/rust`) | Linguagens & Sistemas | Ownership, Borrow Checker, Lifetimes, Traits, Concorrência e Tokio. |
| 🐹 | **Go** (`roadmap.sh/golang`) | Linguagens & Backend | Goroutines, Channels, Interfaces implícitas, Context e Microserviços. |
| 🏛️ | **System Design** (`roadmap.sh/system-design`) | Arquitetura & Escala | CDN, Load Balancers, Redis, Sharding, CAP/PACELC, Kafka e Resiliência. |
| 🐳 | **DevOps & Cloud** (`roadmap.sh/devops`) | Infraestrutura & Cloud | Linux, Docker, Kubernetes, CI/CD GitOps, Terraform e Observabilidade. |
| 📐 | **Software Architecture** (`roadmap.sh/software-architect`) | Arquitetura & Engenharia | DDD Estratégico/Tático, Clean Architecture, Hexagonal, CQRS e Sagas. |
| 🐍 | **Python Moderno** (`roadmap.sh/python`) | Linguagens & Backend | Type Hints, Pydantic v2, Generators, Decorators, Asyncio e FastAPI. |

---

## 🎮 Mecânicas do Tatame (Gamificação)

* **🔥 Daily Streak Global:**
  * Seu streak diário é unificado! Se hoje você treinar um kata de Rust e amanhã ler um conceito de System Design, sua chama diária continua subindo.
* **🥋 Faixas & Títulos Temáticos por Trilha:**
  * Cada trilha possui sua própria progressão de faixas (da Faixa Branca à Faixa Preta / Sensei) e títulos temáticos (ex: *Java Refugee*, *Borrow Apprentice*, *Goroutine Novice*, *Cache Master*).
* **🗺️ Skill Tree com Pré-requisitos:**
  * Os katas avançados ficam bloqueados (`[🔒]`) até que você domine os fundamentos daquela trilha.
* **🥋 Tradução Mental (Rosetta Stone):**
  * Para cada nó, o app explica:
    * 🎯 O Conceito Chave.
    * ☕ Ponto de Partida (como você pensaria em Java/Tradicional).
    * 🚀 O Jeito Desta Trilha.
    * 💡 O Conselho do Sensei.

---

## ⌨️ Controles no Terminal

| Tecla | Ação |
| :--- | :--- |
| **`M`** ou **`T`** | **Abrir Seletor de Jornadas / Trocar Trilha** |
| `↑` ou `k` | Subir no menu ou na Skill Tree |
| `↓` ou `j` | Descer no menu ou na Skill Tree |
| `Espaço` ou `Enter` | Dominar Kata / Confirmar Trilha |
| `q` ou `Esc` | Fechar modal ou sair do Dojo (salvando progresso) |

---

## 🔌 Adicionando Novas Trilhas no Futuro

Para criar qualquer nova trilha (ex: *Kubernetes avançado*, *C++*, *SQL & PostgreSQL*), basta criar uma subpasta em `data/tracks/<nome>/track.json`. O Dojo descobre e carrega a nova jornada automaticamente na inicialização!
