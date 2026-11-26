# 🥋 Checkpoint 4: Final Exam

This checkpoint closes the **Mastery** phase and the Frontend Developer track. It has two
parts, and you need both:

1. **The quiz** (press **Q**): eight questions over testing, web components, rendering
   strategies and production. You need 85%, so 7 right.
2. **The integration kata** (press **Enter**): the cart page on release day. A plain form that
   works without JavaScript becomes better with it: a quantity custom element, a live total in
   the visitor's language, error reports tagged with the release, and a payment script that may
   be down or too slow.

Do them in any order. Before you start, review:

| Lesson | Ask yourself |
| --- | --- |
| Testing frontends | Which kind of test fits this behavior? Why query by role and label? How do you control time and the network instead of sleeping? What does axe-core catch, and what does it miss? |
| Web components and architecture | What runs in `connectedCallback`, and when does it run again? Light DOM or shadow DOM, and what breaks across a shadow boundary? Does the page work before and without JavaScript? Which rendering strategy suits each page? |
| Frontend in production | How do you learn about an error before the support tickets do? What does a report need to point at the release that caused it? What happens when a third-party script never arrives? Which `Intl` APIs format prices, dates and plurals? |

The kata leans on the earlier phases too: accessible names (Phase 1), events and forms
(Phase 2), and timers that must not block the main thread (Phase 3).

> [!TIP]
> Build `quantity-input.js` first and check it by hand in the browser, then the summary, then
> the reports and the payments. Each hidden test is named after one behavior of the README.
