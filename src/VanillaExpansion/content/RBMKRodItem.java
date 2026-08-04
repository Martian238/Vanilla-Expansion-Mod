package VanillaExpansion.content;

import arc.graphics.*;
import mindustry.type.*;
import mindustry.world.meta.*;
import VanillaExpansion.expand.world.block.power.RBMKDials;

/**
 * RBMK 燃料棒物品 —— HBM's Nuclear Tech {@code ItemRBMKRod} 的忠实移植。
 * 物品本身只携带"燃料定义"（静态参数 + 算法）；每根已装入的燃料棒的运行时状态
 * （剩余产额/氙毒/芯热/壳热）由 {@link RBMKFuelData.RodState} 持有
 * （Mindustry 的 Item 是无 NBT 的单例，状态必须存放在 Building 内）。
 */
public class RBMKRodItem extends Item {

    // ---------- 反应性/燃耗参数（对应 HBM ItemRBMKRod） ----------

    public String fullName = "";
    /** 反应性函数终点值 */
    public double reactivity = 0;
    /** 自发通量（中子源/自持燃料） */
    public double selfRate = 0;
    /** 反应性曲线类型 */
    public EnumBurnFunc function = EnumBurnFunc.LOG_TEN;
    /** 富集度-反应性关系 */
    public EnumDepleteFunc depFunc = EnumDepleteFunc.GENTLE_SLOPE;
    /** 氙毒产生系数（线性） */
    public double xGen = 0.5;
    /** 氙毒燃尽系数（二次） */
    public double xBurn = 50;
    /** 每单位出通量产生的芯热 */
    public double heat = 1;
    /** 生命周期总可吸收通量 */
    public double yield = 100;
    /** 壳热熔点（芯热可以多高都行） */
    public double meltingPoint = 1000;
    /** 芯→壳热扩散速度 */
    public double diffusion = 0.02;
    /** 最有效裂变中子类型 */
    public NType nType = NType.SLOW;
    /** 裂变释放中子类型 */
    public NType rType = NType.FAST;
    /** 燃料柱内渲染颜色 */
    public int colorTint = 0x304825;
    /** 热系数开始生效的芯热 */
    public double heatCoeffStart = 0;
    /** 热系数从1降到0的芯热跨度 */
    public double heatCoeffLength = 0;
    /** 熔毁触发狄加玛之矛等危险事件 */
    public boolean dangerous = false;

    // ---------- 兼容旧字段 ----------

    /** 初始产额比例 [0;1]，新棒初始产额 = yield * enrichment */
    public float enrichment = 1f;
    /** 是否为中子源（纯标记，物理上由 selfRate 决定） */
    public boolean isNeutronSource = false;

    public RBMKRodItem(String name, Color color) {
        super(name, color);
        cost = 1000;
    }

    // ---------- 流式配置 ----------

    public RBMKRodItem setYield(double yield) {
        this.yield = yield;
        return this;
    }

    public RBMKRodItem setStats(double reactivity, double selfRate) {
        this.reactivity = reactivity;
        this.selfRate = selfRate;
        return this;
    }

    public RBMKRodItem setFunction(EnumBurnFunc func) {
        this.function = func;
        return this;
    }

    public RBMKRodItem setDepletionFunction(EnumDepleteFunc func) {
        this.depFunc = func;
        return this;
    }

    public RBMKRodItem setXenon(double gen, double burn) {
        this.xGen = gen;
        this.xBurn = burn;
        return this;
    }

    public RBMKRodItem setHeat(double heat) {
        this.heat = heat;
        return this;
    }

    public RBMKRodItem setDiffusion(double diffusion) {
        this.diffusion = diffusion;
        return this;
    }

    public RBMKRodItem setMeltingPoint(double meltingPoint) {
        this.meltingPoint = meltingPoint;
        return this;
    }

    public RBMKRodItem setHeatCoeff(double start, double length) {
        this.heatCoeffStart = start;
        this.heatCoeffLength = length;
        return this;
    }

    public RBMKRodItem setNeutronTypes(NType nType, NType rType) {
        this.nType = nType;
        this.rType = rType;
        return this;
    }

    public RBMKRodItem setTint(int tint) {
        this.colorTint = tint;
        return this;
    }

    public RBMKRodItem setDangerous(boolean dangerous) {
        this.dangerous = dangerous;
        return this;
    }

    // ---------- 中子类型 ----------

    public enum NType {
        FAST, SLOW, ANY;

        public String unlocalized;

        NType() {
            this.unlocalized = "trait.rbmk.neutron." + name().toLowerCase();
        }
    }

    // ---------- 反应性曲线 ----------

    public enum EnumBurnFunc {
        PASSIVE("SAFE / PASSIVE"),                  // 常量，无反应性
        LOG_TEN("MEDIUM / LOGARITHMIC"),            // log10(x + 1) * 0.5 * reactivity
        PLATEU("SAFE / EULER"),                     // (1 - e^(-x/25)) * reactivity * 100
        ARCH("DANGEROUS / NEGATIVE-QUADRATIC"),     // (x - x²/10000) / 100 * reactivity
        SIGMOID("SAFE / SIGMOID"),                  // reactivity / (1 + e^(-(x-50)/10))
        SQUARE_ROOT("MEDIUM / SQUARE ROOT"),        // sqrt(x) * reactivity / 10
        LINEAR("DANGEROUS / LINEAR"),               // x / 100 * reactivity
        QUADRATIC("DANGEROUS / QUADRATIC"),         // x² / 10000 * reactivity
        SLOW_LINEAR("MEDIUM / SLOW LINEAR"),        // sqrt(2x + 30) / 10 * reactivity / 2.5
        EXPERIMENTAL("EXPERIMENTAL / SINE SLOPE");  // x * (sin(x) + 1) * reactivity

        public final String title;

        EnumBurnFunc(String title) {
            this.title = title;
        }
    }

    /** @param enrichment 富集度 [0;1]，未经氙毒修正 */
    public double reactivityFunc(double in, double enrichment) {
        double flux = in * reactivityModByEnrichment(enrichment);

        switch (function) {
            case PASSIVE: return selfRate * enrichment;
            case LOG_TEN: return Math.log10(flux + 1) * 0.5 * reactivity;
            case PLATEU: return (1 - Math.pow(Math.E, -flux / 25D)) * reactivity;
            case ARCH: return Math.max((flux - flux * flux / 10000D) / 100D * reactivity, 0D);
            case SIGMOID: return reactivity / (1 + Math.pow(Math.E, -(flux - 50D) / 10D));
            case SQUARE_ROOT: return Math.sqrt(flux) * reactivity / 10D;
            case LINEAR: return flux / 100D * reactivity;
            case QUADRATIC: return flux * flux / 10000D * reactivity;
            case SLOW_LINEAR: return Math.sqrt(2 * flux + 30) / 10 * reactivity / 2.5;
            case EXPERIMENTAL: return flux * (Math.sin(flux) + 1) * reactivity;
        }

        return 0;
    }

    // ---------- 耗竭曲线 ----------

    public enum EnumDepleteFunc {
        LINEAR,            // 旧函数，直降
        RAISING_SLOPE,     // 增殖燃料（如 MEU），约 28% 耗竭处峰值 110%
        BOOSTED_SLOPE,     // 强增殖燃料（如 Th232），约 64% 耗竭处峰值 132%
        GENTLE_SLOPE,      // 多数燃料推荐，起始附近略超 100%
        STATIC,            // 街机式中子源
        CF_SLOPE           // 模拟锎积累，约 60% 耗竭处峰值 ~193%
    }

    public double reactivityModByEnrichment(double enrichment) {
        switch (depFunc) {
            default:
            case LINEAR: return enrichment;
            case STATIC: return 1D;
            case BOOSTED_SLOPE: return enrichment + Math.sin((enrichment - 1) * (enrichment - 1) * Math.PI);
            case RAISING_SLOPE: return enrichment + (Math.sin(enrichment * Math.PI) / 2D);
            case GENTLE_SLOPE: return enrichment + (Math.sin(enrichment * Math.PI) / 3D);
            case CF_SLOPE: return enrichment + (Math.sin(enrichment * Math.PI)) * 1.4;
        }
    }

    // ---------- 氙毒 ----------

    /** 每 tick 氙毒产生，线性 */
    public double xenonGenFunc(double flux) {
        return flux * xGen;
    }

    /** 每 tick 氙毒燃尽，二次 */
    public double xenonBurnFunc(double flux) {
        return (flux * flux) / xBurn;
    }

    // ---------- 燃耗结算 ----------

    /**
     * 每 tick 裂变结算（对应 HBM burn()）：
     * 氙毒"先用后生"（先烧后产）→ 热系数 → 反应性函数 → 耗竭 → 芯热累积。
     * @param state 该棒的运行时状态
     * @param inFlux 进入该棒的通量（已按 nType 折算）
     * @return 出通量（用于继续传播）
     */
    public double burn(RBMKFuelData.RodState state, double inFlux) {

        inFlux += selfRate;

        if (!RBMKDials.disableXenon) {
            double xenon = state.xenon;
            xenon -= xenonBurnFunc(inFlux);
            inFlux *= (1D - state.getPoisonLevel());
            xenon += xenonGenFunc(inFlux);
            if (xenon < 0D) xenon = 0D;
            if (xenon > 100D) xenon = 100D;
            state.xenon = xenon;
        }

        double mult = 1D;
        if (heatCoeffStart != 0 && state.coreHeat >= heatCoeffStart) {
            double prog = (state.coreHeat - heatCoeffStart) / heatCoeffLength;
            if (prog > 1) prog = 1;
            mult = Math.sin((prog * Math.PI + Math.PI) / 2);
        }

        double outFlux = reactivityFunc(inFlux, state.getEnrichment(this) * mult) * RBMKDials.reactivityMod;

        if (!RBMKDials.disableDepletion) {
            state.yield -= inFlux;
            if (state.yield < 0D) state.yield = 0D;
        }

        state.coreHeat = rectify(state.coreHeat + outFlux * heat);

        return outFlux;
    }

    private double rectify(double num) {
        if (num > 1_000_000D) num = 1_000_000D;
        if (num < 20D || Double.isNaN(num)) num = 20D;
        return num;
    }

    /** 芯→壳热扩散（对应 HBM updateHeat） */
    public void updateHeat(RBMKFuelData.RodState state, double mod) {
        if (state.coreHeat > state.hullHeat) {
            double mid = (state.coreHeat - state.hullHeat) / 2D;
            state.coreHeat = rectify(state.coreHeat - mid * diffusion * RBMKDials.fuelDiffusionMod * mod);
            state.hullHeat = rectify(state.hullHeat + mid * diffusion * RBMKDials.fuelDiffusionMod * mod);
        }
    }

    /**
     * 壳热→柱体传热（对应 HBM provideHeat）。
     * 若壳热超过熔点则瞬间熔毁：芯/壳/柱体温取平均，全部回传给柱体。
     * @param state 该棒运行时状态
     * @param columnHeat 柱体现在温度
     * @param mod 倍率
     * @return 传递给柱体的热量增量
     */
    public double provideHeat(RBMKFuelData.RodState state, double columnHeat, double mod) {

        double hullHeat = state.hullHeat;

        if (hullHeat > meltingPoint) {
            double coreHeat = state.coreHeat;
            double avg = (columnHeat + hullHeat + coreHeat) / 3D;
            state.coreHeat = avg;
            state.hullHeat = avg;
            return avg - columnHeat;
        }

        if (hullHeat <= columnHeat)
            return 0;

        double ret = (hullHeat - columnHeat) / 2D;
        ret *= RBMKDials.heatProvision * mod;

        state.hullHeat = hullHeat - ret;

        return ret;
    }

    @Override
    public void setStats() {
        super.setStats();

        stats.add(Stat.abilities, "[gold]Reactivity: " + reactivity);
        stats.add(Stat.abilities, "[gold]Function: " + function.title);
        stats.add(Stat.abilities, "[gold]Heat: " + heat + "°C/outFlux");
        stats.add(Stat.abilities, "[gold]Diffusion: " + diffusion);
        stats.add(Stat.abilities, "[gold]Melting Point: " + (int) meltingPoint + "°C");
        stats.add(Stat.abilities, "[gold]Xenon Gen: x * " + xGen + " / Burn: x² / " + xBurn);
        stats.add(Stat.abilities, "[blue]Splits With: " + nType.name() + " / Into: " + rType.name());

        if (selfRate > 0 || function == EnumBurnFunc.SIGMOID) {
            stats.add(Stat.abilities, "[red]Neutron Source" + (selfRate > 0 ? " (+" + selfRate + ")" : ""));
        }
    }
}
