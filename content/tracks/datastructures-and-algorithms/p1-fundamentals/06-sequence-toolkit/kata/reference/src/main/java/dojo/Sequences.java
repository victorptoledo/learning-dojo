package dojo;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/** Linear-time techniques on arrays and strings. */
public final class Sequences {
    private Sequences() {}

    public static int[] pairWithSum(int[] sorted, int target) {
        int left = 0;
        int right = sorted.length - 1;
        while (left < right) {
            long sum = (long) sorted[left] + sorted[right];
            if (sum == target) return new int[] {left, right};
            if (sum < target) {
                left++;
            } else {
                right--;
            }
        }
        return new int[0];
    }

    public static int longestUniqueRun(String text) {
        Map<Character, Integer> lastSeen = new HashMap<>();
        int best = 0;
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            Integer previous = lastSeen.put(text.charAt(i), i);
            if (previous != null && previous >= start) {
                start = previous + 1;
            }
            best = Math.max(best, i - start + 1);
        }
        return best;
    }

    public static int[] nextGreater(int[] values) {
        int[] answer = new int[values.length];
        Arrays.fill(answer, -1);
        Deque<Integer> waiting = new ArrayDeque<>();
        for (int i = 0; i < values.length; i++) {
            while (!waiting.isEmpty() && values[waiting.peek()] < values[i]) {
                answer[waiting.pop()] = values[i];
            }
            waiting.push(i);
        }
        return answer;
    }

    public static boolean balanced(String text) {
        Deque<Character> open = new ArrayDeque<>();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '(', '[', '{' -> open.push(c);
                case ')', ']', '}' -> {
                    if (open.isEmpty() || open.pop() != partner(c)) return false;
                }
                default -> {
                    // not a bracket
                }
            }
        }
        return open.isEmpty();
    }

    private static char partner(char closing) {
        return switch (closing) {
            case ')' -> '(';
            case ']' -> '[';
            default -> '{';
        };
    }

    public static long countSubarraysWithSum(int[] values, long k) {
        Map<Long, Integer> seen = new HashMap<>();
        seen.put(0L, 1);
        long prefix = 0;
        long count = 0;
        for (int value : values) {
            prefix += value;
            count += seen.getOrDefault(prefix - k, 0);
            seen.merge(prefix, 1, Integer::sum);
        }
        return count;
    }

    public static int shortestWindowWithSum(int[] positives, long atLeast) {
        int best = Integer.MAX_VALUE;
        long sum = 0;
        int left = 0;
        for (int right = 0; right < positives.length; right++) {
            sum += positives[right];
            while (sum >= atLeast && left <= right) {
                best = Math.min(best, right - left + 1);
                sum -= positives[left++];
            }
        }
        return best == Integer.MAX_VALUE ? 0 : best;
    }
}
