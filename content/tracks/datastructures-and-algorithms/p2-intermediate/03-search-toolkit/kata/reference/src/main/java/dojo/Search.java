package dojo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Binary search, partitioning, merge sort and backtracking. Never modifies its inputs. */
public final class Search {
    private Search() {}

    public static int lowerBound(int[] sorted, int target) {
        int lo = 0;
        int hi = sorted.length; // the answer is in [lo, hi]
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (sorted[mid] < target) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    public static int searchRotated(int[] rotated, int target) {
        int lo = 0;
        int hi = rotated.length - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (rotated[mid] == target) return mid;
            if (rotated[lo] <= rotated[mid]) { // the left half is sorted
                if (rotated[lo] <= target && target < rotated[mid]) {
                    hi = mid - 1;
                } else {
                    lo = mid + 1;
                }
            } else { // the right half is sorted
                if (rotated[mid] < target && target <= rotated[hi]) {
                    lo = mid + 1;
                } else {
                    hi = mid - 1;
                }
            }
        }
        return -1;
    }

    public static int minShipCapacity(int[] weights, int days) {
        if (days < 1) throw new IllegalArgumentException("at least one day");
        long lo = 0;
        long hi = 0;
        for (int w : weights) {
            lo = Math.max(lo, w);
            hi += w;
        }
        while (lo < hi) {
            long mid = (lo + hi) >>> 1;
            if (daysNeeded(weights, mid) <= days) {
                hi = mid;
            } else {
                lo = mid + 1;
            }
        }
        return (int) lo;
    }

    private static int daysNeeded(int[] weights, long capacity) {
        int days = 1;
        long load = 0;
        for (int w : weights) {
            if (load + w > capacity) {
                days++;
                load = 0;
            }
            load += w;
        }
        return days;
    }

    public static int kthSmallest(int[] values, int k) {
        if (k < 1 || k > values.length) throw new IllegalArgumentException("k out of range: " + k);
        int[] a = values.clone();
        int lo = 0;
        int hi = a.length - 1;
        int want = k - 1;
        var random = ThreadLocalRandom.current();
        while (true) {
            int pivot = a[lo + random.nextInt(hi - lo + 1)];
            // three-way partition: [lo, lt) < pivot, [lt, gt] == pivot, (gt, hi] > pivot
            int lt = lo;
            int gt = hi;
            int i = lo;
            while (i <= gt) {
                if (a[i] < pivot) {
                    swap(a, lt++, i++);
                } else if (a[i] > pivot) {
                    swap(a, i, gt--);
                } else {
                    i++;
                }
            }
            if (want < lt) {
                hi = lt - 1;
            } else if (want > gt) {
                lo = gt + 1;
            } else {
                return pivot;
            }
        }
    }

    private static void swap(int[] a, int i, int j) {
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    public static long countInversions(int[] values) {
        int[] a = values.clone();
        return sortAndCount(a, new int[a.length], 0, a.length);
    }

    private static long sortAndCount(int[] a, int[] buffer, int from, int to) {
        if (to - from < 2) return 0;
        int mid = (from + to) >>> 1;
        long count = sortAndCount(a, buffer, from, mid) + sortAndCount(a, buffer, mid, to);
        int i = from;
        int j = mid;
        int k = from;
        while (i < mid && j < to) {
            if (a[i] <= a[j]) {
                buffer[k++] = a[i++];
            } else {
                count += mid - i; // a[j] jumps over everything left in the left half
                buffer[k++] = a[j++];
            }
        }
        while (i < mid) buffer[k++] = a[i++];
        while (j < to) buffer[k++] = a[j++];
        System.arraycopy(buffer, from, a, from, to - from);
        return count;
    }

    public static List<List<Integer>> combinationSum(int[] candidates, int target) {
        int[] sorted = candidates.clone();
        Arrays.sort(sorted);
        List<List<Integer>> out = new ArrayList<>();
        backtrack(sorted, 0, target, new ArrayList<>(), out);
        return out;
    }

    private static void backtrack(int[] sorted, int start, int left, List<Integer> chosen, List<List<Integer>> out) {
        if (left == 0) {
            out.add(List.copyOf(chosen));
            return;
        }
        for (int i = start; i < sorted.length && sorted[i] <= left; i++) {
            chosen.add(sorted[i]);
            backtrack(sorted, i, left - sorted[i], chosen, out);
            chosen.removeLast();
        }
    }
}
