package VanillaExpansion.content;

import arc.graphics.Color;
import mindustry.content.StatusEffects;
import mindustry.type.Liquid;

/**
 * Proxima模组液体定义
 */
public class VELiquids {
    public static Liquid steam;
    
    // DFC系统专用流体
    public static Liquid dfcCoolant;      // DFC冷却液
    public static Liquid dfcFuel;         // DFC燃料
    public static Liquid dfcAntimatter;   // 反物质
    public static Liquid dfcPositiveMatter; // 正物质

    public static void load(){
        // 蒸汽 - 用于RBMK反应堆换热
        steam = new Liquid("steam", Color.valueOf("dddddd")){{
            gas = true; // 是气体
            temperature = 1.5f; // 高温
            heatCapacity = 0.8f; // 热容量
            viscosity = 0.1f; // 低粘度
            explosiveness = 0.1f; // 轻微爆炸性
            effect = StatusEffects.wet; // 效果：湿润
        }};
        
        // DFC冷却液 - 高效冷却剂，用于DFC核心和交换器
        dfcCoolant = new Liquid("dfc-coolant", Color.valueOf("00aaff")){{
            viscosity = 0.5f;
            heatCapacity = 5f;
            flammability = 0f;
            gasColor = Color.valueOf("00aaff").a(0.3f);
        }};
        
        // DFC燃料 - 高能量密度燃料，用于能量发射器
        dfcFuel = new Liquid("dfc-fuel", Color.valueOf("ff8800")){{
            viscosity = 0.8f;
            heatCapacity = 2f;
            flammability = 1f;
            gasColor = Color.valueOf("ff8800").a(0.3f);
        }};
        
        // 反物质 - 用于DFC核心的高级燃料
        dfcAntimatter = new Liquid("dfc-antimatter", Color.valueOf("ff00ff")){{
            viscosity = 0.3f;
            heatCapacity = 10f;
            flammability = 0f;
            gasColor = Color.valueOf("ff00ff").a(0.3f);
        }};
        
        // 正物质 - 与反物质配对使用
        dfcPositiveMatter = new Liquid("dfc-positive-matter", Color.valueOf("00ffff")){{
            viscosity = 0.3f;
            heatCapacity = 10f;
            flammability = 0f;
            gasColor = Color.valueOf("00ffff").a(0.3f);
        }};
    }
}
