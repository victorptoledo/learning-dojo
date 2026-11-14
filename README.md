# 🥋 Learning Dojo

Uma plataforma de aprendizado gamificada e extensível diretamente no terminal (TUI), construída em **Rust** com [`ratatui`](https://ratatui.rs/).

Inspirada no catálogo completo do [roadmap.sh](https://roadmap.sh/), com **29 jornadas oficiais**, **árvore de habilidades interativa (Katas)**, **sistema de faixas e graus**, **streak diário global** e uma camada de **tradução mental (Rosetta Stone)** comparando a visão tradicional do ecossistema Java com cada tecnologia.

---

## 🚀 Como Executar

Abra o seu terminal (PowerShell, Windows Terminal ou VS Code) na pasta do projeto e execute:

```powershell
cd c:\dev\learning-dojo
cargo run
```

---

## 🗺️ Catálogo de Jornadas Disponíveis (29 Trilhas do roadmap.sh)

Aperte **`M`** ou **`T`** a qualquer momento no app para abrir o **Menu de Jornadas** e escolher seu tatame:

### 💼 Carreiras & Roles
* ⚙️ **Backend Developer** (`backend`): HTTP, JWT/OAuth2, SQL vs NoSQL, Caching & Message Queues.
* 🎨 **Frontend Developer** (`frontend`): DOM, CSS Grid/Flexbox, Reatividade, Core Web Vitals.
* 🌐 **Full Stack Developer** (`full-stack`): Type-safe APIs, SSR/SSG/ISR, Edge & Serverless.
* 🤖 **AI Engineer** (`ai-engineer`): LLMs, Embeddings, Bancos Vetoriais, RAG, AI Agents.
* 📊 **Data Engineer** (`data-engineer`): ETL vs ELT, Kimball, Parquet, Apache Spark distribuído.
* 🛡️ **Cyber Security** (`cyber-security`): OWASP Top 10, SSRF, Criptografia, Argon2, Secrets Management.
* 🐳 **DevOps & Cloud** (`devops`): Linux, Docker, Kubernetes, CI/CD GitOps, Terraform, Observabilidade.
* 📐 **Software Architecture** (`software-architect`): DDD Estratégico/Tático, Hexagonal, CQRS, Sagas.
* ⚛️ **React Developer** (`react`): Hooks, Fiber, Diffing, Suspense e Server Components.

### 💻 Linguagens de Programação
* 🦀 **Rust** (`rust`): Ownership, Borrow Checker, Lifetimes, Traits, Smart Pointers, Concorrência e Tokio.
* 🐹 **Go** (`go`): Goroutines, Channels, Interfaces Duck-Typing, `if err != nil`, Context e Microsserviços.
* ☕ **Java Moderno & Spring Boot** (`java`): Records, Pattern Matching, Virtual Threads (Project Loom), GraalVM.
* 🐍 **Python Moderno** (`python`): Type Hints, Pydantic v2, Generators, Decorators, Asyncio e FastAPI.
* 🔷 **TypeScript** (`typescript`): Tipagem Estrutural, Tipos Condicionais, Utilitários e Type Narrowing.
* ⚡ **C++ Moderno** (`cpp`): RAII, Smart Pointers, Move Semantics, Templates e Zero-Cost Abstractions.
* 🟢 **Node.js** (`nodejs`): Event Loop, Libuv, Streams, Buffers e I/O não bloqueante.

### 🏛️ Arquitetura, Engenharia & Sistemas
* 🏛️ **System Design** (`system-design`): CDNs, Load Balancers, Redis, Sharding, CAP/PACELC, Kafka e Resiliência.
* 🔌 **API Design Guidelines** (`api-design`): RESTful Maturity Model, gRPC, Protobuf, Versionamento e Idempotência.
* 🧩 **Design Patterns** (`design-patterns`): Strategy, Factory, Decorator, Adapter e Composição sobre Herança.
* 🧠 **Prompt Engineering** (`prompt-engineering`): Few-Shot, Chain-of-Thought (CoT), ReAct e Structured Outputs.

### 🗄️ Bancos de Dados & Persistência
* 🐘 **PostgreSQL** (`postgresql`): EXPLAIN ANALYZE, Índices GIN/BRIN/Parciais, MVCC e Tuning.
* 🗄️ **SQL & Relational DBs** (`sql`): Window Functions, CTEs Recursivas, Transações e Teoria dos Conjuntos.

### ☁️ Infraestrutura, Cloud & Ferramentas
* 📦 **Docker & Containers** (`docker`): Namespaces, cgroups, Multi-stage builds, Distroless e Hardening.
* ☸️ **Kubernetes Mastery** (`kubernetes`): Control Plane, etcd, HPA, KEDA, Ingress e Operators.
* 🐧 **Linux Mastery** (`linux`): Permissões POSIX, systemd, Signals, Network sockets e Bash.
* 🐙 **Git & GitHub** (`git`): DAG de commits, Blobs/Trees, Rebase interativo, Bisect e Git Reflog.
* ☁️ **AWS Cloud Architecture** (`aws`): IAM Roles, VPC, Subnets Privadas, Well-Architected Framework.

### 🎓 Fundamentos da Computação
* 🎓 **Computer Science Fundamentals** (`computer-science`): Hierarquia de Memória (L1/RAM), Cache Lines, Big-O.
* 🧠 **Data Structures & Algorithms** (`datastructures-and-algorithms`): Hash Tables, Grafos, BFS/DFS, Árvores.

---

## 🎮 Mecânicas do Tatame (Gamificação)

* **🔥 Daily Streak Global:**
  * Seu streak diário é unificado entre todas as 29 jornadas! Estude um kata por dia para manter sua chama ativa.
* **🥋 Faixas & Títulos Temáticos:**
  * Cada trilha possui suas próprias faixas (da Faixa Branca à Faixa Preta / Sensei) e títulos temáticos divertidos.
* **🗺️ Skill Tree com Pré-requisitos:**
  * Katas avançados só são liberados quando os pré-requisitos forem dominados.
* **🥋 Tradução Mental (Rosetta Stone):**
  * Toda trilha possui notas explicando o conceito, como você pensaria em Java/Tradicional, o jeito moderno da tecnologia e a dica do Sensei.

---

## ⌨️ Controles no Terminal

| Tecla | Ação |
| :--- | :--- |
| **`M`** ou **`T`** | **Abrir Seletor de Jornadas / Trocar Trilha** |
| `↑` ou `k` | Subir no menu ou na Skill Tree |
| `↓` ou `j` | Descer no menu ou na Skill Tree |
| `Espaço` ou `Enter` | Dominar Kata / Confirmar Trilha |
| `q` ou `Esc` | Fechar modal ou sair do Dojo (salvando progresso) |
