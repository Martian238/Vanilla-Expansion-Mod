package VanillaExpansion.expand.type.unit;

import mindustry.gen.Unit; /** 物品处理器接口 - 改为 public */
@FunctionalInterface
public interface StuffProcessor {
    void process(Unit unit, StuffType stuff, ExpandUnitCore.StuffSlotData slot);
}
