package VanillaExpansion.content;
import arc.struct.*;
import arc.util.io.*;
import mindustry.type.*;
public class RBMKFuelData {
    public static class RodState {
        //剩余可吸收通量
        public double yield;
        //氙毒[0;100]
        public double xenon;
        //芯热[20;1_000_000]
        public double coreHeat = 20D;
        //壳热[20;1_000_000]
        public double hullHeat = 20D;
        public static RodState create(RBMKRodItem rod) {RodState s = new RodState();s.yield = rod.yield * rod.enrichment;return s;}
        public double getEnrichment(RBMKRodItem rod) {
            return rod.yield <= 0 ? 0 : yield / rod.yield;
        }
        public double getPoisonLevel() {
            return xenon / 100D;
        }
        public void write(Writes write) {write.d(yield);write.d(xenon);write.d(coreHeat);write.d(hullHeat);}
        public void read(Reads read) {yield = read.d();xenon = read.d();coreHeat = read.d();hullHeat = read.d();}
    }
    private static final ObjectMap<Item, RBMKRodItem> fuelDataMap = new ObjectMap<>();
    public static void registerFuel(Item item, RBMKRodItem rod) {if (item != null && rod != null) {fuelDataMap.put(item, rod);}}
    public static RBMKRodItem getFuelProperties(Item item) {
        return fuelDataMap.get(item);
    }
    //die方法，读是否是燃料
    public static boolean isFuel(Item item) {
        return item != null && fuelDataMap.containsKey(item);
    }

    //mors方法,为新燃料棒创建一个运行时状态
    public static RodState newState(Item item) {
        RBMKRodItem rod = getFuelProperties(item);
        return rod == null ? null : RodState.create(rod);
    }
    //初始化
    public static void initDefaultFuels() {
        if (VEItems.plutonium238BerylliumSource != null) registerFuel(VEItems.plutonium238BerylliumSource, VEItems.plutonium238BerylliumSource);
        if (VEItems.heu235UraniumFuel != null) registerFuel(VEItems.heu235UraniumFuel, VEItems.heu235UraniumFuel);
        if (VEItems.dgammaFuel != null) registerFuel(VEItems.dgammaFuel, VEItems.dgammaFuel);
    }
}
