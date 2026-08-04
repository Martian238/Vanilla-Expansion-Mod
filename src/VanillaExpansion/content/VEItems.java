package VanillaExpansion.content;

import arc.graphics.*;
import arc.struct.*;
import mindustry.type.*;

/**
 * Proxima物品注册
 */
public class VEItems {
    public static Item iron, uranium, manganese, gold;
    
    public static final Seq<Item> proximaOreItems = new Seq<>();
    
    public static RBMKRodItem plutonium238BerylliumSource; // 钚238-铍中子源
    public static RBMKRodItem heu235UraniumFuel; // HEU-235铀燃料棒
    public static RBMKRodItem dgammaFuel;
    

    
    public static void load(){
        iron = new Item("iron", Color.valueOf("a8a8a8")){{
            hardness = 2;
            cost = 0.8f;
            databaseTag = "basic-item";
        }};
        
        uranium = new Item("uranium", Color.valueOf("7fff00")){{
            hardness = 5;
            cost = 1.5f;
            radioactivity = 1.2f;
            explosiveness = 0.3f;
            healthScaling = 0.15f;
            databaseTag = "basic-item";
        }};
        
        manganese = new Item("manganese", Color.valueOf("E35745FF")){{
            hardness = 4;
            cost = 1.3f;
            healthScaling = 0.7f;
            databaseTag = "basic-item";
        }};

        gold = new Item("gold", Color.valueOf("ffd37f")){{
            hardness = 2;
            cost = 1.0f;
            databaseTag = "basic-item";
        }};
        
        proximaOreItems.addAll(iron, uranium, manganese, gold);
        
        // 钚238-铍中子源 - 对应 HBM rbmk_fuel_pu238be（tintPlutonium=0x656E6B）
        plutonium238BerylliumSource = new RBMKRodItem("plutonium238-beryllium-source", new Color(0.2f, 0.3f, 0.8f)){{
            isNeutronSource = true;
            enrichment = 1f;
            cost = 5000;
            radioactivity = 5f;
            databaseTag = "processed-item";
        }}
            .setYield(50_000_000D)
            .setStats(40, 40)
            .setFunction(RBMKRodItem.EnumBurnFunc.SQUARE_ROOT)
            .setHeat(0.1D)
            .setDiffusion(0.05D)
            .setMeltingPoint(1287D)
            .setNeutronTypes(RBMKRodItem.NType.SLOW, RBMKRodItem.NType.SLOW)
            .setTint(0x656E6B);
        
        // HEU-235铀燃料棒 - 对应 HBM rbmk_fuel_heu235（tintUranium=0x868D82）
        heu235UraniumFuel = new RBMKRodItem("heu235-uranium-fuel", new Color(0.3f, 0.8f, 0.2f)){{
            enrichment = 1f;
            cost = 3000;
            radioactivity = 3f;
            databaseTag = "processed-item";
        }}
            .setYield(100_000_000D)
            .setStats(50, 0)
            .setFunction(RBMKRodItem.EnumBurnFunc.SQUARE_ROOT)
            .setMeltingPoint(2865D)
            .setTint(0x868D82);
        // 迪伽马燃料棒 - 对应 HBM rbmk_fuel_drx（tintDRX=0xD77276，熔毁触发狄加玛之矛）
        dgammaFuel = new RBMKRodItem("dgamma-source", Color.valueOf("C70000FF")){{
            enrichment = 1f;
            cost = 5000;
            radioactivity = 5f;
            databaseTag = "processed-item";
        }}
            .setYield(10_000_000D)
            .setStats(1000, 10)
            .setFunction(RBMKRodItem.EnumBurnFunc.QUADRATIC)
            .setHeat(0.1D)
            .setMeltingPoint(100_000D)
            .setDangerous(true)
            .setTint(0xD77276);
    }
}
