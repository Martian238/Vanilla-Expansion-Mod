package VanillaExpansion.expand.type.unit;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.abilities.Ability;
import mindustry.entities.part.DrawPart;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.type.*;

/**
 * 物品类型 - 可以装备到单位上的功能模块
 * 不继承 UnlockableContent，避免干扰内容系统
 */
public class StuffType {
    /** 所有已注册的物品列表 */
    public static Seq<StuffType> allStuffs = new Seq<>();

    /** 物品名称/显示名 */
    public String stuffName = "Unnamed Stuff";

    /** 物品描述 */
    public String description = "A modular unit attachment.";

    /** 物品图标 */
    public @Nullable TextureRegion icon;

    /** 占用的槽位类型 */
    public StuffSlot slot = StuffSlot.general;

    /** 占用的槽位数量 */
    public int slotCount = 1;

    /** 重量/负载值，影响单位速度 */
    public float weight = 1f;

    /** 是否可堆叠 */
    public boolean stackable = true;

    /** 最大堆叠数 */
    public int maxStack = 1;

    /** 此物品提供的所有武器 */
    public Seq<Weapon> weapons = new Seq<>();

    /** 此物品提供的所有能力 */
    public Seq<Ability> abilities = new Seq<>();

    /** 此物品提供的所有绘图部件 */
    public Seq<DrawPart> parts = new Seq<>();

    /** 物品提供的特殊功能处理器 */
    public @Nullable StuffProcessor processor;

    /** 物品激活条件检查器 */
    public @Nullable StuffCondition condition;

    /** 物品冷却时间 (刻) */
    public float cooldown = 0f;

    /** 是否为被动物品 (无需激活) */
    public boolean passive = false;

    /** 物品等级 (用于UI排序) */
    public int tier = 0;

    /** 物品稀有度颜色 */
    public Color rarityColor = Pal.gray;

    public StuffType(String name) {
        allStuffs.add(this);
        // 加载图标
        icon = Core.atlas.find(name + "-icon", Core.atlas.find("clear"));
    }

    /** 初始化物品 */
    public void init() {
        weapons.each(Weapon::init);
        abilities.each(ab -> ab.init(null));
    }

    /** 加载资源 */
    public void load() {
        if (icon == null || !icon.found()) {
            icon = Core.atlas.find("clear");
        }
    }

    /** 激活物品效果 */
    public void activate(Unit unit, ExpandUnitCore.StuffSlotData slotData) {
        if (cooldown > 0 && unit instanceof ExpandUnit) {
            ExpandUnit eu = (ExpandUnit) unit;
            ObjectMap<String, Float> cooldowns = eu.getCooldowns();
            float lastUse = cooldowns.get(stuffName, 0f);
            if (Time.time - lastUse < cooldown) return;
            cooldowns.put(stuffName, Time.time);
        }

        if (processor != null) {
            processor.process(unit, this, slotData);
        }
    }

    /** 检查是否满足激活条件 */
    public boolean canActivate(Unit unit) {
        return condition == null || condition.check(unit, this);
    }

    /** 获取物品描述 */
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("[lightgray]").append(description).append("\n\n");

        if (weapons.any()) {
            sb.append("[accent]Weapons: [white]").append(weapons.size).append("\n");
            for (Weapon w : weapons) {
                sb.append("  - ").append(w.name).append("\n");
            }
        }

        if (abilities.any()) {
            sb.append("[accent]Abilities: [white]").append(abilities.size).append("\n");
        }

        if (parts.any()) {
            sb.append("[accent]Visual Parts: [white]").append(parts.size).append("\n");
        }

        if (weight != 1f) {
            sb.append("[gray]Weight: [white]").append(weight).append("\n");
        }

        return sb.toString();
    }
}