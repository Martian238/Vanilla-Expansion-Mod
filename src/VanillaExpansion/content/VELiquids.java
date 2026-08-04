package VanillaExpansion.content;

import arc.graphics.Color;
import mindustry.content.StatusEffects;
import mindustry.type.Liquid;

/**
 * Proxima模组液体定义
 */
public class VELiquids {
    // RBMK 蒸汽体系（对应 HBM Fluids：STEAM/HOTSTEAM/SUPERHOTSTEAM/ULTRAHOTSTEAM/SPENTSTEAM）
    public static Liquid steam;            // 常规蒸汽 100°C
    public static Liquid hotSteam;         // 热蒸汽 300°C
    public static Liquid superhotSteam;    // 超热蒸汽 450°C
    public static Liquid ultrahotSteam;    // 极热蒸汽 600°C
    public static Liquid spentSteam;       // 乏蒸汽（涡轮冷却输出）
    public static Liquid moltenCore;      // 熔融堆芯（corium）
    public static Liquid perfluoromethylCold; // 冷全氟甲基（PFC 冷却液，低温端）
    public static Liquid perfluoromethyl;     // 热全氟甲基（PFC 加热后，高温端）

    // DFC系统专用流体
    public static Liquid dfcCoolant;      // DFC冷却液
    public static Liquid dfcFuel;         // DFC燃料
    public static Liquid dfcAntimatter;   // 反物质
    public static Liquid dfcPositiveMatter; // 正物质

    public static void load(){
        // 蒸汽 - 用于RBMK反应堆换热
        steam = new Liquid("steam", Color.valueOf("e5e5e5")){{
            gas = true; // 是气体
            temperature = 1.5f; // 高温
            heatCapacity = 0.8f; // 热容量
            viscosity = 0.1f; // 低粘度
            explosiveness = 0.1f; // 轻微爆炸性
            effect = StatusEffects.wet; // 效果：湿润
        }};

        // 热蒸汽 - RBMK 一级压缩（对应 HBM HOTSTEAM，300°C）
        hotSteam = new Liquid("hot-steam", Color.valueOf("E7D6D6")){{
            gas = true;
            temperature = 2f;
            heatCapacity = 1f;
            viscosity = 0.1f;
            explosiveness = 0.12f;
            effect = StatusEffects.wet;
        }};

        // 超热蒸汽 - RBMK 二级压缩（对应 HBM SUPERHOTSTEAM，450°C）
        superhotSteam = new Liquid("superhot-steam", Color.valueOf("E7B7B7")){{
            gas = true;
            temperature = 3f;
            heatCapacity = 1.2f;
            viscosity = 0.1f;
            explosiveness = 0.15f;
            effect = StatusEffects.wet;
        }};

        // 极热蒸汽 - RBMK 三级压缩（对应 HBM ULTRAHOTSTEAM，600°C）
        ultrahotSteam = new Liquid("ultrahot-steam", Color.valueOf("E39393")){{
            gas = true;
            temperature = 4f;
            heatCapacity = 1.4f;
            viscosity = 0.1f;
            explosiveness = 0.18f;
            effect = StatusEffects.wet;
        }};

        // 乏蒸汽 - 涡轮用后低温蒸汽（对应 HBM SPENTSTEAM）
        spentSteam = new Liquid("spent-steam", Color.valueOf("445772")){{
            gas = true;
            temperature = 0.5f;
            heatCapacity = 0.6f;
            viscosity = 0.12f;
            explosiveness = 0f;
            effect = StatusEffects.wet;
        }};

        // 熔融堆芯 - RBMK熔毁后在爆炸中心形成的超高温粘稠岩浆（150%粘度）
        moltenCore = new Liquid("molten-core", Color.valueOf("ff5a1f")){{
            viscosity = 0.95f;
            temperature = 10f; // 极高温度
            heatCapacity = 5f; // 高储热
            flammability = 0f; // 自身即高温物，不额外助燃
            explosiveness = 0f;
            effect = StatusEffects.burning; // 接触即灼烧
            gasColor = Color.valueOf("ff3d00").a(0.4f);
        }};

        // 冷全氟甲基 - RBMK 冷却器输入（对应 HBM PERFLUOROMETHYL_COLD，低温高效冷却液）
        perfluoromethylCold = new Liquid("perfluoromethyl-cold", Color.valueOf("d8fcff")){{
            viscosity = 0.4f;
            temperature = 0.1f; // 低温
            heatCapacity = 6f; // 高储热，极佳冷却剂
            flammability = 0f;
            gasColor = Color.valueOf("d8fcff").a(0.3f);
        }};

        // 热全氟甲基 - RBMK 冷却器输出（对应 HBM PERFLUOROMETHYL，600°C）
        perfluoromethyl = new Liquid("perfluoromethyl", Color.valueOf("99525e")){{
            viscosity = 0.4f;
            temperature = 6f; // 高温
            heatCapacity = 6f;
            flammability = 0f;
            gasColor = Color.valueOf("99525e").a(0.3f);
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
