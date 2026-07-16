package VanillaExpansion.content;

import arc.struct.*;
import mindustry.type.*;

/**
 * RBMK燃料棒数据
 * 存储燃料棒的属性和数据
 */
public class RBMKFuelData {
    // 燃料数据映射表
    private static final ObjectMap<Item, FuelProperties> fuelDataMap = new ObjectMap<>();
    
    /**
     * 燃料属性类
     */
    public static class FuelProperties {
        public float heat = 10f;           // 产热量
        public float enrichment = 1f;      // 富集度
        public boolean isNeutronSource = false; // 是否为中子源
        public float meltingPoint = 2000f; // 熔点
        public float xenonGenerationRate = 0.01f; // 氙毒产生速率
        public float fuelConsumptionRate = 0.001f; // 燃料消耗速率
        public boolean dangerous = false;  // 是否危险（爆炸时产生无限范围杀伤）

        public FuelProperties() {}

        public FuelProperties(float heat, float enrichment, boolean isNeutronSource, float meltingPoint) {
            this.heat = heat;
            this.enrichment = enrichment;
            this.isNeutronSource = isNeutronSource;
            this.meltingPoint = meltingPoint;
        }

        public FuelProperties(float heat, float enrichment, boolean isNeutronSource, float meltingPoint,
                              float xenonGenerationRate, float fuelConsumptionRate) {
            this.heat = heat;
            this.enrichment = enrichment;
            this.isNeutronSource = isNeutronSource;
            this.meltingPoint = meltingPoint;
            this.xenonGenerationRate = xenonGenerationRate;
            this.fuelConsumptionRate = fuelConsumptionRate;
        }

        // 新增：完整构造函数，包含dangerous参数
        public FuelProperties(float heat, float enrichment, boolean isNeutronSource, float meltingPoint,
                              float xenonGenerationRate, float fuelConsumptionRate, boolean dangerous) {
            this.heat = heat;
            this.enrichment = enrichment;
            this.isNeutronSource = isNeutronSource;
            this.meltingPoint = meltingPoint;
            this.xenonGenerationRate = xenonGenerationRate;
            this.fuelConsumptionRate = fuelConsumptionRate;
            this.dangerous = dangerous;
        }
    }
    
    /**
     * 注册燃料数据
     * @param item 燃料物品
     * @param properties 燃料属性
     */
    public static void registerFuel(Item item, FuelProperties properties) {
        if (item != null && properties != null) {
            fuelDataMap.put(item, properties);
        }
    }
    
    /**
     * 获取燃料属性
     * @param item 燃料物品
     * @return 燃料属性，如果不是燃料则返回null
     */
    public static FuelProperties getFuelProperties(Item item) {
        return fuelDataMap.get(item);
    }
    
    /**
     * 检查物品是否为燃料
     * @param item 物品
     * @return 是否为燃料
     */
    public static boolean isFuel(Item item) {
        return item != null && fuelDataMap.containsKey(item);
    }
    
    /**
     * 初始化默认燃料数据
     */
    public static void initDefaultFuels() {
        // 钚238-铍中子源
        if (VEItems.plutonium238BerylliumSource != null) {
            registerFuel(VEItems.plutonium238BerylliumSource,
                new FuelProperties(1.5f, 1.2f, true, 2000f, 0.015f, 0.0008f));
        }
        
        // HEU-235铀燃料棒
        if (VEItems.heu235UraniumFuel != null) {
            registerFuel(VEItems.heu235UraniumFuel,
                new FuelProperties(10f, 1.0f, false, 2000f, 0.01f, 0.001f));
        }
        if (VEItems.dgammaFuel != null) {
            registerFuel(VEItems.dgammaFuel,
                    new FuelProperties(25f, 1.5f, false, 2500f, 0.025f, 0.0005f, true)); // dangerous = true
        }
    }
}
