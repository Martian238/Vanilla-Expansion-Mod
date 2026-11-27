package VanillaExpansion.content;
import arc.graphics.*;
import mindustry.type.*;
import mindustry.world.meta.*;
import VanillaExpansion.expand.world.block.power.RBMKDials;
public class RBMKRodItem extends Item {
    public static final Stat reactivityStat = new Stat("rbmk-reactivity");
    public static final Stat functionStat = new Stat("rbmk-function");
    public static final Stat heatStat = new Stat("rbmk-heat");
    public static final Stat diffusionStat = new Stat("rbmk-diffusion");
    public static final Stat meltingPointStat = new Stat("rbmk-melting-point");
    public static final Stat xenonStat = new Stat("rbmk-xenon");
    public static final Stat neutronStat = new Stat("rbmk-neutron");
    public static final Stat neutronSourceStat = new Stat("rbmk-neutron-source");
    public double reactivity = 0;
    public double selfRate = 0;
    public EnumBurnFunc function = EnumBurnFunc.LOG_TEN;
    public EnumDepleteFunc depFunc = EnumDepleteFunc.GENTLE_SLOPE;
    public double xGen = 0.5;
    public double xBurn = 50;
    public double heat = 1;
    public double yield = 100;
    public double meltingPoint = 1000;
    public double diffusion = 0.02;
    public NType nType = NType.SLOW;
    public NType rType = NType.FAST;
    public int colorTint = 0x304825;
    public double heatCoeffStart = 0;
    public double heatCoeffLength = 0;
    public boolean dangerous = false;
    public float enrichment = 1f;
    public boolean isNeutronSource = false;
    public RBMKRodItem(String name, Color color) {super(name, color);cost = 1000;}
    public RBMKRodItem setYield(double yield) {this.yield = yield;return this;}
    public RBMKRodItem setStats(double reactivity, double selfRate) {this.reactivity = reactivity;this.selfRate = selfRate;return this;}
    public RBMKRodItem setFunction(EnumBurnFunc func) {this.function = func;return this;}
    public RBMKRodItem setHeat(double heat) {this.heat = heat;return this;}
    public RBMKRodItem setDiffusion(double diffusion) {this.diffusion = diffusion;return this;}
    public RBMKRodItem setMeltingPoint(double meltingPoint) {this.meltingPoint = meltingPoint;return this;}
    public RBMKRodItem setNeutronTypes(NType nType, NType rType) {this.nType = nType;this.rType = rType;return this;}
    public RBMKRodItem setTint(int tint) {this.colorTint = tint;return this;}
    public RBMKRodItem setDangerous(boolean dangerous) {this.dangerous = dangerous;return this;}
    public enum NType {
        FAST, SLOW, ANY;
        public String unlocalized;
        NType() {
            this.unlocalized = "trait.rbmk.neutron." + name().toLowerCase();
        }
    }
    public enum EnumBurnFunc {
        PASSIVE("SAFE / PASSIVE"),                  //常量，无反应性
        LOG_TEN("MEDIUM / LOGARITHMIC"),            //log10(x + 1) * 0.5 * reactivity
        PLATEU("SAFE / EULER"),                     //(1 - e^(-x/25)) * reactivity * 100
        ARCH("DANGEROUS / NEGATIVE-QUADRATIC"),     //(x - x²/10000) / 100 * reactivity
        SIGMOID("SAFE / SIGMOID"),                  //reactivity / (1 + e^(-(x-50)/10))
        SQUARE_ROOT("MEDIUM / SQUARE ROOT"),        //sqrt(x) * reactivity / 10
        LINEAR("DANGEROUS / LINEAR"),               //x / 100 * reactivity
        QUADRATIC("DANGEROUS / QUADRATIC"),         //x² / 10000 * reactivity
        SLOW_LINEAR("MEDIUM / SLOW LINEAR"),        //sqrt(2x + 30) / 10 * reactivity / 2.5
        EXPERIMENTAL("EXPERIMENTAL / SINE SLOPE");  //x * (sin(x) + 1) * reactivity
        public final String title;
        EnumBurnFunc(String title) {
            this.title = title;
        }
    }
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
    public enum EnumDepleteFunc {
        LINEAR,            //旧函数，直降
        RAISING_SLOPE,     //增殖燃料，约 28% 耗竭处峰值 110%
        BOOSTED_SLOPE,     //强增殖燃料，约 64% 耗竭处峰值 132%
        GENTLE_SLOPE,      //多数燃料推荐，起始附近略超 100%
        STATIC,            //街机式中子源
        CF_SLOPE           //锎积累，约 60% 耗竭处峰值 ~193%
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
    public double xenonGenFunc(double flux) {
        return flux * xGen;
    }
    public double xenonBurnFunc(double flux) {
        return (flux * flux) / xBurn;
    }
    public double burn(RBMKFuelData.RodState state, double inFlux) {
        inFlux += selfRate;
        if (!RBMKDials.disableXenon) {double xenon = state.xenon;xenon -= xenonBurnFunc(inFlux);inFlux *= (1D - state.getPoisonLevel());xenon += xenonGenFunc(inFlux);if (xenon < 0D) xenon = 0D;if (xenon > 100D) xenon = 100D;state.xenon = xenon;}
        double mult = 1D;
        if (heatCoeffStart != 0 && state.coreHeat >= heatCoeffStart) {double prog = (state.coreHeat - heatCoeffStart) / heatCoeffLength;if (prog > 1) prog = 1;mult = Math.sin((prog * Math.PI + Math.PI) / 2);}
        double outFlux = reactivityFunc(inFlux, state.getEnrichment(this) * mult) * RBMKDials.reactivityMod;
        if (!RBMKDials.disableDepletion) {state.yield -= inFlux;if (state.yield < 0D) state.yield = 0D;}
        state.coreHeat = rectify(state.coreHeat + outFlux * heat);
        return outFlux;
    }
    private double rectify(double num) {if (num > 1_000_000D) num = 1_000_000D;if (num < 20D || Double.isNaN(num)) num = 20D;return num;}
    public void updateHeat(RBMKFuelData.RodState state, double mod) {
        if (state.coreHeat > state.hullHeat) {double mid = (state.coreHeat - state.hullHeat) / 2D;state.coreHeat = rectify(state.coreHeat - mid * diffusion * RBMKDials.fuelDiffusionMod * mod);state.hullHeat = rectify(state.hullHeat + mid * diffusion * RBMKDials.fuelDiffusionMod * mod);}
    }
    public double provideHeat(RBMKFuelData.RodState state, double columnHeat, double mod) {
        double hullHeat = state.hullHeat;
        if (hullHeat > meltingPoint) {double coreHeat = state.coreHeat;double avg = (columnHeat + hullHeat + coreHeat) / 3D;state.coreHeat = avg;state.hullHeat = avg;return avg - columnHeat;}
        if (hullHeat <= columnHeat) return 0;
        double ret = (hullHeat - columnHeat) / 2D;
        ret *= RBMKDials.heatProvision * mod;
        state.hullHeat = hullHeat - ret;
        return ret;
    }
    @Override
    public void setStats() {
        super.setStats();
        stats.add(reactivityStat, (float) reactivity);
        stats.add(functionStat, function.title);
        stats.add(heatStat, "[gold]" + heat + "°C/flux");
        stats.add(diffusionStat, (float) diffusion);
        stats.add(meltingPointStat, "[gold]" + (int) meltingPoint + "°C");
        stats.add(xenonStat, "[gold]gen ×" + xGen + " / burn x²/" + xBurn);
        stats.add(neutronStat, nType.name() + " → " + rType.name());
        if (selfRate > 0 || function == EnumBurnFunc.SIGMOID) {stats.add(neutronSourceStat, selfRate > 0 ? "[red](+" + selfRate + ")" : "[red]yes");}
    }
}
