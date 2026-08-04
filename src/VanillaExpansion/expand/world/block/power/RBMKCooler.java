package VanillaExpansion.expand.world.block.power;

import VanillaExpansion.content.VELiquids;
import arc.math.*;
import arc.struct.*;
import arc.util.io.*;
import mindustry.gen.Building;
import mindustry.graphics.*;
import mindustry.type.Liquid;
import mindustry.ui.Bar;

import static mindustry.Vars.*;

/**
 * RBMK 冷却器 —— 参考 HBM's Nuclear Tech 的 TileEntityRBMKCooler / RBMKCooler。
 * <p>
 * 应急冷却部件：消耗冷全氟甲基（PFM），把 5×5 列范围内所有 RBMK 组件快速降温。
 * 每结算 tick 消耗 50mB 冷 PFM、产出 50mB 热 PFM，并让 5×5 网格内的每个柱体降温 200°C（下限 20°C）。
 * 与锅炉不同，冷却器不提取能量，只用于紧急/辅助散热。
 * <p>
 * 应急激活条件：仅当本柱体热达到 {@link #ACTIVATION_HEAT}（750°C）后才开始消耗冷却液工作。
 * <p>
 * 移植说明：
 * <ul>
 * <li>柱体按 2 格网格铺设（与控制台 STEP=2 一致），故 5×5 列区域 = 本柱 ±4 格、步长 2 的 25 个扫描点。</li>
 * <li>液量按 1/100 缩放（HBM 4000mB → 40 单位），耗液 0.5 单位/结算 tick（HBM 50mB/t）。</li>
 * <li>每 {@link RBMKDials#simTickEvery} 游戏 tick（等效 HBM 20tps 逐 tick）结算一次，降温 200°C 与 HBM 原值一致。</li>
 * <li>Mindustry 无竖直轴向，冷 PFM 从任意方向吸入、热 PFM 向四周输出（HBM 中冷液自下、热液自上，语义一致）。</li>
 * </ul>
 */
public class RBMKCooler extends RBMKBase {

    /** 冷 PFM 储罐容量（HBM 4000 mB，按 1/100 缩放） */
    public float coldCapacity = 40f;
    /** 热 PFM 储罐容量（HBM 4000 mB，按 1/100 缩放） */
    public float hotCapacity = 40f;
    /** 每次结算的耗液/产液量（HBM 50 mB/t，按 1/100 缩放） */
    public static final float RATE = 0.5f;
    /** 每次结算对每个 5×5 内柱体的降温量（°C） */
    public static final float COOL = 200f;
    /** 工作温度阈值：本柱体热达到 750°C 后才开始消耗冷却液工作（应急散热激活点） */
    public static final float ACTIVATION_HEAT = 750f;

    public RBMKCooler(String name) {
        super(name);
        hasLiquids = true;
        outputsLiquid = true;
        liquidCapacity = hotCapacity;
        consoleType = ColumnType.COOLER;
        buildType = RBMKCoolerBuild::new;
    }

    @Override
    public void setBars() {
        super.setBars();
        addBar("cold", (RBMKCoolerBuild entity) -> new Bar(
            () -> "Cold PFM: " + (int) entity.liquids.get(VELiquids.perfluoromethylCold),
            () -> Pal.water,
            () -> Math.min(1f, entity.liquids.get(VELiquids.perfluoromethylCold) / coldCapacity)
        ));
        addBar("hot", (RBMKCoolerBuild entity) -> new Bar(
            () -> "Hot PFM: " + (int) entity.liquids.get(VELiquids.perfluoromethyl),
            () -> Pal.slagOrange,
            () -> Math.min(1f, entity.liquids.get(VELiquids.perfluoromethyl) / hotCapacity)
        ));
    }

    public class RBMKCoolerBuild extends RBMKBaseBuild {

        @Override
        public void updateTile() {
            if (!net.client()) {
                if (shouldSimulate()) {
                    cool();
                }
                dumpLiquid(VELiquids.perfluoromethyl);
            }
            super.updateTile();
        }

        /** 一次结算：耗冷液→产热液，并把 5×5 列范围内所有柱体降温 200°C（下限 20°C） */
        protected void cool() {
            Liquid cold = VELiquids.perfluoromethylCold;
            Liquid hot = VELiquids.perfluoromethyl;

            // 应急激活条件：本柱体热达到 750°C 才开始工作
            if (heat < ACTIVATION_HEAT) return;
            if (liquids.get(cold) < RATE) return;
            if (hotCapacity - liquids.get(hot) < RATE) return;

            liquids.remove(cold, RATE);
            liquids.add(hot, RATE);

            // 5×5 列区域：本柱 ±4 格、步长 2（对应控制台网格 STEP=2）
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    Building b = world.build(tileX() + dx * 2, tileY() + dz * 2);
                    if (b instanceof RBMKBaseBuild rb) {
                        rb.heat -= COOL;
                        if (rb.heat < 20) rb.heat = 20f;
                    }
                }
            }
        }

        // ---------- 流体 I/O ----------

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            return liquid == VELiquids.perfluoromethylCold
                && liquids.get(VELiquids.perfluoromethylCold) < coldCapacity;
        }

        @Override
        public void handleLiquid(Building source, Liquid liquid, float amount) {
            if (liquid == VELiquids.perfluoromethylCold) {
                liquids.add(VELiquids.perfluoromethylCold, Math.min(amount, coldCapacity - liquids.get(VELiquids.perfluoromethylCold)));
            }
        }

        // ---------- 控制台 API ----------

        @Override
        public ColumnType getConsoleType() {
            return ColumnType.COOLER;
        }

        @Override
        public ObjectMap<String, Object> getConsoleData() {
            ObjectMap<String, Object> data = super.getConsoleData();
            if (data == null) data = new ObjectMap<>();
            data.put("cold", (int) liquids.get(VELiquids.perfluoromethylCold));
            data.put("maxCold", (int) coldCapacity);
            data.put("hot", (int) liquids.get(VELiquids.perfluoromethyl));
            data.put("maxHot", (int) hotCapacity);
            return data;
        }

        // ---------- 熔毁（对应 HBM onMelt：1+rand(2) 块 BLANK 碎片） ----------

        @Override
        public void onMelt(int reduce) {
            int count = 1 + Mathf.random(2);
            for (int i = 0; i < count; i++) {
                spawnDebris(DebrisType.BLANK);
            }
            kill();
        }
    }
}
