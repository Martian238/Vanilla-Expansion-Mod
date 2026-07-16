package VanillaExpansion.content;

import arc.graphics.*;
import arc.struct.*;
import mindustry.type.*;

/**
 * Proxima物品注册
 */
public class VEItems {
    public static Item iron, uranium, manganese, quartz;
    
    public static final Seq<Item> proximaOreItems = new Seq<>();
    
    public static Item plutonium238BerylliumSource; // 钚238-铍中子源
    public static Item heu235UraniumFuel; // HEU-235铀燃料棒
    public static Item dgammaFuel;
    

    
    public static void load(){
        iron = new Item("iron", Color.valueOf("a8a8a8")){{
            hardness = 2;
            cost = 0.8f;
        }};
        
        uranium = new Item("uranium", Color.valueOf("7fff00")){{
            hardness = 5;
            cost = 1.5f;
            radioactivity = 1.2f;
            explosiveness = 0.3f;
            healthScaling = 0.15f;
        }};
        
        manganese = new Item("manganese", Color.valueOf("E35745FF")){{
            hardness = 4;
            cost = 1.3f;
            healthScaling = 0.7f;
        }};
        
        quartz = new Item("quartz", Color.valueOf("f0f0f0")){{
            cost = 0.9f;
        }};
        
        proximaOreItems.addAll(iron, uranium, manganese, quartz);
        
        // 钚238-铍中子源 - 深蓝色带放射性
        plutonium238BerylliumSource = new RBMKRodItem("plutonium238-beryllium-source", new Color(0.2f, 0.3f, 0.8f)){{
            yield = 0.8f;
            heat = 15f;
            selfRate = 0.3f;
            diffusion = 0.8f;
            meltingPoint = 2500f;
            isNeutronSource = true;
            enrichment = 1f;
            cost = 5000;
            radioactivity = 5f;
        }};
        
        // HEU-235铀燃料棒 - 亮绿色
        heu235UraniumFuel = new RBMKRodItem("heu235-uranium-fuel", new Color(0.3f, 0.8f, 0.2f)){{
            yield = 1f;
            heat = 20f;
            selfRate = 0.05f;
            diffusion = 1f;
            meltingPoint = 2000f;
            isNeutronSource = false;
            enrichment = 0.95f;
            cost = 3000;
            radioactivity = 3f;
        }};
        // 迪伽马燃料棒
        dgammaFuel = new RBMKRodItem("dgamma-source", Color.valueOf("C70000FF")){{
            yield = 0.8f;
            heat = 15f;
            selfRate = 0.3f;
            diffusion = 0.8f;
            meltingPoint = 2500f;
            isNeutronSource = false;
            enrichment = 1f;
            cost = 5000;
            radioactivity = 5f;
        }};
    }
}
