package VanillaExpansion.expand.type.unit;

import arc.struct.*;
import mindustry.entities.abilities.Ability;
import mindustry.entities.part.DrawPart;
import mindustry.gen.Unit;
import mindustry.type.Weapon;

/**
 * 可扩展单位实体接口
 * 定义了可装备物品的单位所需的所有方法
 */
public interface ExpandUnit {
    Unit getUnit();

    /** 初始化可扩展单位 */
    void initExpandable(ExpandUnitCore core);

    /** 添加动态武器 */
    void addDynamicWeapons(Seq<Weapon> weapons);

    /** 添加动态能力 */
    void addDynamicAbilities(Seq<Ability> abilities);

    /** 添加动态绘图部件 */
    void addDynamicParts(Seq<DrawPart> parts);

    /** 清除动态组件 */
    void clearDynamicComponents();

    /** 获取已装备的物品 */
    ObjectMap<Integer, StuffType> getEquippedStuff();

    /** 获取物品冷却数据 */
    ObjectMap<String, Float> getCooldowns();
}