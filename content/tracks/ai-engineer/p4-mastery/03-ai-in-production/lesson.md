# AI Features in Production: Model Migrations, Governance and Ownership

Shipping the first version is the easy part. An AI feature lives for years while models, providers,
prices, regulations and users change around it. This lesson is about owning it: making change safe,
handling incidents, and meeting the duties that come with putting a model in front of people.

## Everything that shapes output is configuration

A feature's behaviour depends on: the **model id**, the **prompt templates**, the **tool
definitions**, the **retrieval settings** (embedding model, chunking, k, thresholds, reranker), the
**index** contents, and the **guardrail rules**. Treat all of it like code:

- versioned in the repository (or a prompt registry with versions), reviewed in pull requests;
- tied to the golden set: a change runs the evals in CI before it can merge;
- recorded on every trace (Advanced), so you know which version produced which answer;
- rolled out behind feature flags, so you can switch back in seconds.

## Model upgrades are migrations

Providers release better and cheaper models every few months and **retire** old ones on published
schedules. Plan for it:

1. **Track deprecations** for every model you use; put the dates in the team calendar.
2. **Evaluate candidates** on each feature's golden set and safety set: quality per slice, refusal
   rate, format compliance, cost and latency. A new model is not better at everything.
3. **Adapt prompts.** Newer models often follow instructions more literally, handle examples
   differently or need less scaffolding; prompts tuned for the old model may hurt.
4. **Roll out safely**: shadow traffic (run the new model in parallel, compare, do not show), then a
   small percentage, then more, watching quality proxies, errors and cost; keep the rollback path
   until the old model is gone.
5. **Avoid lock-in where it is cheap**: a provider-neutral client, model ids in configuration, evals
   that run against any model, and a second provider tested as a fallback for critical features.

## A/B tests and online experiments

Offline evals tell you whether a change is safe; only users tell you whether it is **better**. Use
A/B tests for changes whose value is in user behaviour (acceptance of suggestions, resolution rate,
time saved), with a metric chosen in advance and enough traffic to be meaningful.

## Incidents

AI features fail in their own ways. Prepare runbooks for:

- **provider outage or degradation**: switch to the fallback model or degrade gracefully;
- **quality regression** (a silent model change, a bad prompt deploy, a broken index): roll back
  the configuration, compare traces before and after;
- **harmful or embarrassing output** going viral: disable the feature or the path quickly (a kill
  switch), preserve the traces, fix, add the case to the safety set;
- **data exposure** (one tenant's data in another's answer): treat it as a security incident;
- **cost spike** (a loop, abuse, a traffic surge): budgets and alerts per tenant and feature, and
  hard limits.

Blameless postmortems apply as for any service; the action items usually include new eval cases.

## Governance and regulation

Depending on where you operate and what the feature does, there are legal and organisational duties:

- **Risk classification.** The **EU AI Act** sorts AI uses into prohibited practices, high-risk
  systems (for example in hiring, credit, education, essential services) with strict requirements on
  risk management, data, documentation, human oversight and accuracy, limited-risk systems with
  **transparency** duties (people must know they are talking to an AI; AI-generated content must be
  marked in some cases), and minimal-risk ones. It also sets obligations for providers of
  general-purpose AI models. Its obligations apply in stages from 2025 onwards.
- **Risk management frameworks** such as the **NIST AI RMF** (govern, map, measure, manage) and
  **ISO/IEC 42001** (an AI management system standard) structure the work.
- **Data protection** (for example the GDPR): lawful basis for processing, minimisation, retention,
  rights of the people whose data you process, and data processing agreements with providers.
- **Documentation**: what the system is for and not for, which models and data it uses, how it was
  evaluated, its known limitations. Model and system cards are a common format.
- **Human oversight**: who can review, override or switch off the system, and how users can appeal
  or reach a human.

You are not expected to be a lawyer, but an AI engineer should recognise when a feature falls into a
regulated category and bring in legal and compliance people early.

## The team and the craft

- AI engineering is a team sport: product decides what "good" means, domain experts label the golden
  set, engineers build and measure, security reviews tools and data flows.
- Make evaluation a habit everyone owns: every bug becomes a case, every change runs the suite.
- Write down decisions (why this model, why this chunk size, why a human approves refunds) so the
  next engineer understands the trade-offs.
- Keep learning deliberately: the field moves fast, but the fundamentals of this track (context,
  contracts, tools, evaluation, safety, cost) change slowly.

## Summary

- Prompts, model ids, retrieval and guardrail settings are versioned, reviewed, evaluated and
  flagged configuration.
- Model upgrades are migrations: track deprecations, evaluate, adapt prompts, roll out gradually,
  keep a rollback, avoid cheap lock-in.
- Prepare runbooks for outages, regressions, harmful output, data exposure and cost spikes.
- Know the governance duties: risk classes and transparency (EU AI Act), frameworks (NIST AI RMF,
  ISO/IEC 42001), data protection, documentation and human oversight.
- Make evaluation and written decisions part of the team's daily work.

## Further reading

- European Union, Regulation (EU) 2024/1689 (the AI Act), official text: <https://eur-lex.europa.eu/eli/reg/2024/1689/oj>
- NIST, AI Risk Management Framework: <https://www.nist.gov/itl/ai-risk-management-framework>
- ISO/IEC 42001, AI management systems: <https://www.iso.org/standard/81230.html>
- Anthropic docs, model deprecations: <https://platform.claude.com/docs/en/about-claude/model-deprecations>
- OpenAI docs, deprecations: <https://platform.openai.com/docs/deprecations>
- Mitchell et al., "Model Cards for Model Reporting" (2019): <https://arxiv.org/abs/1810.03993>
- Sculley et al., "Hidden Technical Debt in Machine Learning Systems" (NeurIPS 2015): <https://papers.nips.cc/paper/5656-hidden-technical-debt-in-machine-learning-systems>
