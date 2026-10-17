package VanillaExpansion.ui.bossrush;

import arc.math.geom.Vec3;
import arc.struct.Seq;
import mindustry.maps.Map;

public class BossRushNode{
    public String id;
    public String titleKey;
    public String descriptionKey;
    /** 解析后的本地化文本 */
    public String title;
    public String description;

    public String imagePath;
    public BossRushCardType type;
    public String mapPath;
    public String savedMapPath;

    public Seq<String> prerequisites = new Seq<>();
    public Seq<BossRushNode> children = new Seq<>();
    public BossRushNode parent;
    public float x, y;
    public int depth;
    public float screenX, screenY;
    public Vec3 pos = new Vec3();
    public boolean unlocked;
    public boolean completed;
    public Map map;
}