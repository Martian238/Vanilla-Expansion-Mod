package VanillaExpansion.expand.type.unit;

import mindustry.gen.Unit; /** 物品条件接口 - 改为 public */
@FunctionalInterface
public interface StuffCondition {
    boolean check(Unit unit, StuffType stuff);
}
