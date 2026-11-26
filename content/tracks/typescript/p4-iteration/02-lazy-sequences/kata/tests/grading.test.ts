import assert from "node:assert/strict";
import { describe, test } from "node:test";
import { chunk, filter, map, pairwise, range, Seq, take, zip } from "../seq.ts";

/** A source that counts how many items were pulled and whether it was closed. */
function tracked<T>(items: Iterable<T>) {
  const state = { pulled: 0, closed: false, finished: false };
  function* source(): Generator<T, void, undefined> {
    try {
      for (const item of items) {
        state.pulled += 1;
        yield item;
      }
      state.finished = true;
    } finally {
      state.closed = true;
    }
  }
  return { source: source(), state };
}

describe("range", () => {
  test("counts up, down and by steps, end excluded", () => {
    assert.deepEqual([...range(0, 5)], [0, 1, 2, 3, 4]);
    assert.deepEqual([...range(10, 0, -3)], [10, 7, 4, 1]);
    assert.deepEqual([...range(0, 1, 0.25)], [0, 0.25, 0.5, 0.75]);
    assert.deepEqual([...range(3, 3)], []);
  });

  test("rejects a zero step when it is called, not when it is iterated", () => {
    assert.throws(() => range(0, 10, 0), RangeError);
  });
});

describe("map and filter", () => {
  test("map passes the index and is lazy", () => {
    const { source, state } = tracked(["a", "b", "c"]);
    const mapped = map(source, (item, index) => `${index}:${item}`);
    assert.equal(state.pulled, 0, "nothing is pulled before the first next()");
    assert.deepEqual(mapped.next(), { value: "0:a", done: false });
    assert.equal(state.pulled, 1);
    assert.deepEqual([...mapped], ["1:b", "2:c"]);
  });

  test("filter keeps the matching items", () => {
    assert.deepEqual([...filter(range(0, 10), (n) => n % 3 === 0)], [0, 3, 6, 9]);
  });
});

describe("take", () => {
  test("works on an endless source", () => {
    assert.deepEqual([...take(range(5), 3)], [5, 6, 7]);
  });

  test("pulls exactly count items and closes the source", () => {
    const { source, state } = tracked([1, 2, 3, 4, 5]);
    assert.deepEqual([...take(source, 2)], [1, 2]);
    assert.equal(state.pulled, 2, "take must not pull a third item to find out it is done");
    assert.equal(state.closed, true, "the source's finally must run");
    assert.equal(state.finished, false);
  });

  test("take 0 pulls nothing, and a bad count fails at the call", () => {
    const { source, state } = tracked([1, 2]);
    assert.deepEqual([...take(source, 0)], []);
    assert.equal(state.pulled, 0);
    assert.throws(() => take([1], -1), RangeError);
    assert.throws(() => take([1], 1.5), RangeError);
  });
});

describe("chunk and pairwise", () => {
  test("chunk groups items and keeps a short last group", () => {
    assert.deepEqual([...chunk(range(0, 7), 3)], [[0, 1, 2], [3, 4, 5], [6]]);
    assert.deepEqual([...chunk([], 2)], []);
  });

  test("chunk is lazy and fails at the call on a bad size", () => {
    const { source, state } = tracked(range(0, 100));
    const first = chunk(source, 4).next();
    assert.deepEqual(first.value, [0, 1, 2, 3]);
    assert.equal(state.pulled, 4);
    assert.throws(() => chunk([1], 0), RangeError);
  });

  test("pairwise gives each item with the next one", () => {
    assert.deepEqual([...pairwise("abcd")], [["a", "b"], ["b", "c"], ["c", "d"]]);
    assert.deepEqual([...pairwise([1])], []);
    assert.deepEqual([...pairwise([undefined, 0])], [[undefined, 0]]);
  });
});

describe("zip", () => {
  test("stops at the shortest source", () => {
    assert.deepEqual([...zip([1, 2, 3], "ab", range(10))], [[1, "a", 10], [2, "b", 11]]);
    assert.deepEqual([...zip()], []);
  });

  test("closes the sources that still have items", () => {
    const long = tracked([1, 2, 3, 4]);
    const short = tracked(["a"]);
    assert.deepEqual([...zip(long.source, short.source)], [[1, "a"]]);
    assert.equal(long.state.closed, true, "the longer source must be closed");
    assert.equal(short.state.finished, true);
  });

  test("closes every source when the consumer stops early", () => {
    const a = tracked(range(0));
    const b = tracked(range(100));
    for (const pair of zip(a.source, b.source)) {
      if (pair[0] === 2) break;
    }
    assert.equal(a.state.closed, true);
    assert.equal(b.state.closed, true);
  });
});

describe("Seq", () => {
  test("builds a pipeline without running it", () => {
    const { source, state } = tracked([1, 2, 3, 4, 5, 6]);
    const seq = Seq.from(source)
      .filter((n) => n % 2 === 0)
      .map((n) => n * 10)
      .take(2);
    assert.equal(state.pulled, 0);
    assert.deepEqual(seq.toArray(), [20, 40]);
    assert.equal(state.pulled, 4);
    assert.equal(state.closed, true);
  });

  test("can be iterated again when its source can", () => {
    const seq = Seq.from([1, 2, 3]).map((n) => n + 1);
    assert.deepEqual([...seq], [2, 3, 4]);
    assert.deepEqual([...seq], [2, 3, 4]);
    assert.deepEqual(seq.take(1).toArray(), [2]);
  });

  test("take checks its count at the call", () => {
    assert.throws(() => Seq.from([1]).take(-1), RangeError);
  });
});
