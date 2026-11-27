package VanillaExpansion.expand.ui;
import mindustry.ui.layout.TreeLayout;
public class InvertedPyramidTreeLayout implements TreeLayout {
    public float gapBetweenLevels = 160f;
    public float gapBetweenNodes = 20f;
    public int maxDepth;
    private float unit;
    private float maxNodeWidth = 0f;
    @Override
    public void layout(TreeNode root) {
        computeLeaves(root);
        maxDepth = computeDepth(root, 0);
        unit = maxNodeWidth + gapBetweenNodes;
        float totalWidth = Math.max(root.leaves, 1) * unit;
        position(root, -totalWidth / 2f, totalWidth / 2f, 0);
    }
    int computeLeaves(TreeNode node) {
        if (node.isLeaf()) return node.leaves = 1;
        int sum = 0;
        for (TreeNode child : node.children) sum += computeLeaves(child);
        return node.leaves = Math.max(sum, 1);
    }
    int computeDepth(TreeNode node, int depth) {
        maxNodeWidth = Math.max(maxNodeWidth, node.width);
        int max = depth;
        for (TreeNode child : node.children) max = Math.max(max, computeDepth(child, depth + 1));
        return max;
    }
    void position(TreeNode node, float start, float end, int depth) {
        node.x = (start + end) / 2f;
        node.y = (maxDepth - depth) * gapBetweenLevels;
        if (node.isLeaf()) return;
        float totalWeight = 0f;
        for (TreeNode child : node.children) totalWeight += weight(child);
        float cursor = start;
        for (TreeNode child : node.children) {
            float slice = (end - start) * weight(child) / Math.max(totalWeight, 0.0001f);
            position(child, cursor, cursor + slice, depth + 1);
            cursor += slice;
        }
    }
    float weight(TreeNode node) {
        return node.leaves + 2f;
    }
}
