package VanillaExpansion.ui.bossrush;

import arc.math.geom.Rect;
import arc.struct.Seq;
import arc.util.Log;

public class BossRushLayout{
    public float nodeWidth = 160f;
    public float nodeHeight = 220f;
    public float levelHeight = 280f;
    public float siblingSpacing = 200f;

    public Rect bounds = new Rect();

    public void layout(BossRushNode root){
        if(root == null){
            //Log.err("BossRushLayout: root is null");
            return;
        }

        Seq<BossRushNode> queue = new Seq<>();
        root.depth = 0;
        queue.add(root);
        for(int i = 0; i < queue.size; i++){
            BossRushNode n = queue.get(i);
            for(BossRushNode c : n.children){
                c.depth = n.depth + 1;
                queue.add(c);
            }
        }

        assignX(root, new float[]{0f});

        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for(BossRushNode n : queue){
            n.y = -n.depth * levelHeight;
            minX = Math.min(minX, n.x - nodeWidth / 2f);
            maxX = Math.max(maxX, n.x + nodeWidth / 2f);
            minY = Math.min(minY, n.y - nodeHeight / 2f);
            maxY = Math.max(maxY, n.y + nodeHeight / 2f);
        }
        bounds = new Rect(minX, minY, maxX - minX, maxY - minY);

        //Log.info("BossRushLayout: layout done, nodes=@, bounds=@", queue.size, bounds);
    }

    private void assignX(BossRushNode node, float[] cursor){
        if(node.children.size == 0){
            node.x = cursor[0];
            cursor[0] += siblingSpacing;
            return;
        }
        for(BossRushNode child : node.children){
            assignX(child, cursor);
        }
        float first = node.children.first().x;
        float last = node.children.peek().x;
        node.x = (first + last) / 2f;
    }
}