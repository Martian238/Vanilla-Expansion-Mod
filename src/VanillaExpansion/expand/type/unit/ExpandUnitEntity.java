package VanillaExpansion.expand.type.unit;

import arc.struct.*;
import mindustry.entities.abilities.Ability;
import mindustry.entities.part.DrawPart;
import mindustry.gen.Unit;
import mindustry.gen.UnitEntity;
import mindustry.type.Weapon;

/** 可扩展单位的实体实现 */
public class ExpandUnitEntity extends UnitEntity implements ExpandUnit {
    /** 动态武器列表 */
    public Seq<Weapon> dynamicWeapons = new Seq<>();

    /** 动态能力列表 */
    public Seq<Ability> dynamicAbilities = new Seq<>();

    /** 动态部件列表 */
    public Seq<DrawPart> dynamicParts = new Seq<>();

    /** 已装备的物品 */
    public ObjectMap<Integer, StuffType> equippedStuff = new ObjectMap<>();

    /** 冷却时间存储 */
    public ObjectMap<String, Float> cooldowns = new ObjectMap<>();

    /** 单位核心引用 */
    public ExpandUnitCore core;

    @Override
    public Unit getUnit() {
        return this;
    }

    @Override
    public void initExpandable(ExpandUnitCore core) {
        this.core = core;
        this.equippedStuff = core.equippedStuff.copy();
    }

    @Override
    public void addDynamicWeapons(Seq<Weapon> weapons) {
        dynamicWeapons.addAll(weapons);
    }

    @Override
    public void addDynamicAbilities(Seq<Ability> abilities) {
        dynamicAbilities.addAll(abilities);
    }

    @Override
    public void addDynamicParts(Seq<DrawPart> parts) {
        dynamicParts.addAll(parts);
    }

    @Override
    public void clearDynamicComponents() {
        dynamicWeapons.clear();
        dynamicAbilities.clear();
        dynamicParts.clear();
    }

    @Override
    public ObjectMap<Integer, StuffType> getEquippedStuff() {
        return equippedStuff;
    }

    @Override
    public ObjectMap<String, Float> getCooldowns() {
        return cooldowns;
    }

    @Override
    public void update() {
        super.update();

        // 更新动态能力
        for (Ability ability : dynamicAbilities) {
            ability.update(this);
        }
    }

    @Override
    public void draw() {
        // 绘制动态部件
        for (DrawPart part : dynamicParts) {
            DrawPart.params.set(0, 0, 0, 0, 0, 0, x, y, rotation);
            part.draw(DrawPart.params);
        }

        super.draw();
    }
}