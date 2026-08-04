package VanillaExpansion.expand.world.block.power;

/**
 * RBMK 全局参数（对应 HBM 的 RBMKDials gamerule 体系）。
 * 所有值均为服务端权威，可在加载时按需覆写。
 */
public class RBMKDials {
    /** Mindustry 60tps vs HBM 20tps：每多少个游戏 tick 结算一次 RBMK 模拟（60/20=3）。数值保持 HBM 逐 tick 原值。 */
    public static int simTickEvery = 3;

    // 柱体热量
    public static float passiveCooling = 2.5f;          // dialPassiveCooling：边缘柱被动冷却(°C/t)
    public static float passiveCoolingInner = 0.1f;     // dialPassiveCoolingInner：内部柱被动冷却
    public static float columnHeatFlow = 0.2f;          // dialColumnHeatFlow：邻柱热量均分步长
    public static float columnHeight = 3f;              // dialColumnHeight：柱高（HBM 内部 -1，默认 4→3）

    // 燃料
    public static float fuelDiffusionMod = 1.0f;        // dialDiffusionMod：芯→壳热扩散倍率
    public static float heatProvision = 0.2f;           // dialHeatProvision：壳热→柱体传热比例
    public static float reactivityMod = 1.0f;           // dialReactivityMod：全局反应性倍率
    public static boolean disableDepletion = false;     // dialDisableDepletion：禁耗竭
    public static boolean disableXenon = false;         // dialDisableXenon：禁氙毒

    // 控制棒
    public static float controlSpeed = 1.0f;            // dialControlSpeed：控制棒速度倍率
    public static float surgeMod = 1.0f;                // dialControlSurgeMod：插棒浪涌倍率

    // 锅炉 / ReaSim
    public static float boilerHeatConsumption = 0.1f;   // dialBoilerHeatConsumption：每 mB 水耗热
    public static boolean reasimBoilers = false;        // dialReasimBoilers：全部件锅炉模式
    public static float reasimBoilerSpeed = 0.05f;      // dialReasimBoilerSpeed：锅炉模式产汽比例
    public static int reasimRange = 10;                 // dialReasimRange：ReaSim 扫描距离

    // 中子
    public static int fluxRange = 5;                    // dialFluxRange：中子流传播距离
    public static float moderatorEfficiency = 1.0f;     // dialModeratorEfficiency：慢化效率
    public static float absorberEfficiency = 1.0f;      // dialAbsorberEfficiency：吸收效率
    public static float reflectorEfficiency = 1.0f;     // dialReflectorEfficiency：反射效率
    public static float absorberHeatConversion = 0.05f; // dialAbsorberHeatConversion：吸收器通量→热转换

    // 其他
    public static float outgasserMod = 1.0f;            // dialOutgasserSpeedMod：放气器速度倍率
    public static boolean disableMeltdowns = false;     // dialDisableMeltdowns：禁熔毁
    public static boolean enableOverpressure = false;   // dialEnableMeltdownOverpressure：熔毁炸管道
    public static boolean enablePermaScrap = true;      // dialEnablePermaScrap：永久残骸
    public static boolean digamma = false;              // 狄加玛模式（熔毁生成狄加玛之矛）
}
