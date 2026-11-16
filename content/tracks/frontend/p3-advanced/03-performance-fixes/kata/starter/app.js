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
  throw new Error("todo: write yieldToMain()");
}

/**
 * The stories by score, highest first, ties by id. Today it scores all 300 in one task.
 * todo: make it async and yield to the main thread at least every ~10 stories.
 */
export function rankStories(stories) {
  const scored = stories.map((story) => ({ story, score: scoreStory(story) }));
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
document.querySelector("#sort").addEventListener("click", () => {
  status.textContent = "Sorting…";
  // todo: await the async rankStories here
  render(rankStories(STORIES));
  status.textContent = "Sorted by popularity.";
});


// A promotion appears a moment after load, at the very top of the page.
setTimeout(() => {
  const banner = document.createElement("p");
  banner.className = "banner";
  banner.textContent = "Subscribe to The Daily Kata: the first month is free.";
  document.querySelector("#banner-slot").append(banner);
}, 400);
