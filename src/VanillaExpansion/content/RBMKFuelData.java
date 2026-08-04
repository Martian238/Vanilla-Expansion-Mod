package VanillaExpansion.content;

import arc.struct.*;
import arc.util.io.*;
import mindustry.type.*;

/**
 * RBMK 燃料数据。
 * 由于 Mindustry 的 {@link Item} 是无 NBT 的单例，每根已装入燃料柱的运行时状态
 * （剩余产额/氙毒/芯热/壳热）必须独立于物品存储，由 {@link RodState} 承担，
 * 该状态保存在燃料柱 Building 内并随存档读写。
 */
public class RBMKFuelData {

    /**
     * 单根燃料棒的运行时状态（对应 HBM ItemRBMKRod 的 NBT 数据）。
     */
    public static class RodState {
        /** 剩余可吸收通量（总产额随燃耗下降） */
        public double yield;
        /** 氙毒 [0;100] */
        public double xenon;
        /** 芯热 [20;1_000_000] */
        public double coreHeat = 20D;
        /** 壳热 [20;1_000_000] */
        public double hullHeat = 20D;

        /** 根据燃料定义初始化一根全新的棒 */
        public static RodState create(RBMKRodItem rod) {
            RodState s = new RodState();
            s.yield = rod.yield * rod.enrichment;
            return s;
        }

        /** @return 富集度 [0;1] = 剩余产额 / 初始总产额 */
        public double getEnrichment(RBMKRodItem rod) {
            return rod.yield <= 0 ? 0 : yield / rod.yield;
        }

        /** @return 氙毒 [0;1] */
        public double getPoisonLevel() {
            return xenon / 100D;
        }

        public void write(Writes write) {
            write.d(yield);
            write.d(xenon);
            write.d(coreHeat);
            write.d(hullHeat);
        }

        public void read(Reads read) {
            yield = read.d();
            xenon = read.d();
            coreHeat = read.d();
            hullHeat = read.d();
        }
    }

    /** 燃料数据映射表：物品 -> 燃料定义 */
    private static final ObjectMap<Item, RBMKRodItem> fuelDataMap = new ObjectMap<>();

    /** 注册燃料数据 */
    public static void registerFuel(Item item, RBMKRodItem rod) {
        if (item != null && rod != null) {
            fuelDataMap.put(item, rod);
        }
    }

    /** 获取燃料定义，如果不是燃料则返回 null */
    public static RBMKRodItem getFuelProperties(Item item) {
        return fuelDataMap.get(item);
    }

    /** 检查物品是否为燃料 */
    public static boolean isFuel(Item item) {
        return item != null && fuelDataMap.containsKey(item);
    }

    /** 为新燃料棒创建一个运行时状态 */
    public static RodState newState(Item item) {
        RBMKRodItem rod = getFuelProperties(item);
        return rod == null ? null : RodState.create(rod);
    }

    /** 初始化默认燃料数据（在物品加载完成后调用） */
    public static void initDefaultFuels() {
        if (VEItems.plutonium238BerylliumSource != null)
            registerFuel(VEItems.plutonium238BerylliumSource, VEItems.plutonium238BerylliumSource);

        if (VEItems.heu235UraniumFuel != null)
            registerFuel(VEItems.heu235UraniumFuel, VEItems.heu235UraniumFuel);

        if (VEItems.dgammaFuel != null)
            registerFuel(VEItems.dgammaFuel, VEItems.dgammaFuel);
    }
}
