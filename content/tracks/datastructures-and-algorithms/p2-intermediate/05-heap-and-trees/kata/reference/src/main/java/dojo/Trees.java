package dojo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Binary tree problems. Trees can be very deep: do not recurse along a path. */
public final class Trees {
    private Trees() {}

    public static boolean isValidBst(TreeNode root) {
        Deque<TreeNode> stack = new ArrayDeque<>();
        TreeNode node = root;
        boolean first = true;
        int previous = 0;
        while (node != null || !stack.isEmpty()) {
            while (node != null) {
                stack.push(node);
                node = node.left;
            }
            node = stack.pop();
            if (!first && node.val <= previous) return false;
            first = false;
            previous = node.val;
            node = node.right;
        }
        return true;
    }

    public static int kthSmallest(TreeNode root, int k) {
        if (k < 1) throw new IllegalArgumentException("k starts at 1");
        Deque<TreeNode> stack = new ArrayDeque<>();
        TreeNode node = root;
        int seen = 0;
        while (node != null || !stack.isEmpty()) {
            while (node != null) {
                stack.push(node);
                node = node.left;
            }
            node = stack.pop();
            if (++seen == k) return node.val;
            node = node.right;
        }
        throw new IllegalArgumentException("the tree has fewer than " + k + " values");
    }

    public static List<List<Integer>> levelOrder(TreeNode root) {
        List<List<Integer>> levels = new ArrayList<>();
        if (root == null) return levels;
        Deque<TreeNode> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            int width = queue.size();
            List<Integer> level = new ArrayList<>(width);
            for (int i = 0; i < width; i++) {
                TreeNode node = queue.poll();
                level.add(node.val);
                if (node.left != null) queue.add(node.left);
                if (node.right != null) queue.add(node.right);
            }
            levels.add(level);
        }
        return levels;
    }

    public static int height(TreeNode root) {
        return levelOrder(root).size();
    }

    public static TreeNode fromSorted(int[] sorted) {
        return build(sorted, 0, sorted.length - 1); // depth is only log n here
    }

    private static TreeNode build(int[] sorted, int lo, int hi) {
        if (lo > hi) return null;
        int mid = (lo + hi) >>> 1;
        return new TreeNode(sorted[mid], build(sorted, lo, mid - 1), build(sorted, mid + 1, hi));
    }

    public static int lowestCommonAncestor(TreeNode bstRoot, int a, int b) {
        int low = Math.min(a, b);
        int high = Math.max(a, b);
        TreeNode node = bstRoot;
        while (node != null) {
            if (high < node.val) {
                node = node.left;
            } else if (low > node.val) {
                node = node.right;
            } else {
                return node.val;
            }
        }
        throw new IllegalArgumentException("the values are not in the tree");
    }
}
