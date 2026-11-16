// The newspaper's scripts. Two of them hurt the Core Web Vitals: the banner causes layout
// shifts (CLS), and sorting blocks the main thread for a third of a second (INP). See README.md.

export const STORIES = Array.from({ length: 300 }, (_, i) => ({
  id: i + 1,
  title: `Story number ${i + 1}`,
  views: (i * 7919) % 1000,
  comments: (i * 104729) % 50,
}));

/**
 * The popularity of a story: views + 10 × comments. It stands for real work (say, a scoring
 * model) and takes about a millisecond on purpose. Do not change it.
 */
export function scoreStory(story) {
  const end = performance.now() + 1;
  while (performance.now() < end) {
    // busy: this is the expensive part
  }
  return story.views + 10 * story.comments;
}

/** Resolves on a later task, so the browser can handle input and paint in between. */
export function yieldToMain() {
  if (globalThis.scheduler?.yield) {
    return globalThis.scheduler.yield();
  }
  return new Promise((resolve) => setTimeout(resolve, 0));
}

const CHUNK = 10;

/** The stories by score, highest first, ties by id, scored in chunks between yields. */
export async function rankStories(stories) {
  const scored = [];
  for (let i = 0; i < stories.length; i += CHUNK) {
    for (const story of stories.slice(i, i + CHUNK)) {
      scored.push({ story, score: scoreStory(story) });
    }
    await yieldToMain();
  }
  scored.sort((a, b) => b.score - a.score || a.story.id - b.story.id);
  return scored.map(({ story }) => story);
}

function render(stories) {
  const list = document.querySelector("#stories");
  list.replaceChildren(
    ...stories.slice(0, 10).map((story) => {
      const item = document.createElement("li");
      item.textContent = story.title;
      return item;
    }),
  );
}

const status = document.querySelector("#sort-status");
const button = document.querySelector("#sort");
button.addEventListener("click", async () => {
  status.textContent = "Sorting…";
  button.disabled = true;
  try {
    render(await rankStories(STORIES));
    status.textContent = "Sorted by popularity.";
  } finally {
    button.disabled = false;
  }
});


// A promotion appears a moment after load, in a slot whose height is reserved in style.css.
setTimeout(() => {
  const banner = document.createElement("p");
  banner.className = "banner";
  banner.textContent = "Subscribe to The Daily Kata: the first month is free.";
  document.querySelector("#banner-slot").append(banner);
}, 400);
