package VanillaExpansion.content;

import VanillaExpansion.expand.type.fluids.AnimationLiquid;
import VanillaExpansion.expand.type.fluids.CorrosiveLiquid;
import mindustry.type.CellLiquid;
import mindustry.type.Liquid;

public class VEJSLiquids {
    public static Liquid lava, chlorine, melonWater, melonine;
    public static CellLiquid melonWaterCorrupted, dysharmonyFluid;
    public static CorrosiveLiquid acid;
    public static void load(){
        lava = new Liquid("lava");
        chlorine = new Liquid("chlorine");
        acid = new CorrosiveLiquid("acid");
        melonWater = new Liquid("melon-water");
        melonWaterCorrupted = new CellLiquid("melon-water-corrupted");
        melonine = new AnimationLiquid("melonine");
        dysharmonyFluid = new CellLiquid("dysharmony-fluid");
    }
}
