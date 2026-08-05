package VanillaExpansion.expand.world.block.power;

import arc.func.*;
import arc.math.*;
import arc.struct.*;
import arc.util.io.*;
import mindustry.content.Fx;
import mindustry.content.Liquids;
import mindustry.entities.Damage;
import mindustry.gen.Building;
import mindustry.graphics.*;
import mindustry.type.Liquid;
import mindustry.ui.Bar;

import static mindustry.Vars.*;

/**
 * RBMK 冷却器 —— 参考 HBM's Nuclear Tech 的 TileEntityRBMKCooler / RBMKCooler。
 * <p>
 * 应急冷却部件：消耗原版冷冻液（cryofluid），把 5×5 列范围内所有 RBMK 组件快速降温。
 * 每结算 tick 消耗 0.5 单位 cryofluid，并让 5×5 网格内的每个柱体降温 200°C（下限 20°C）。
 * 与锅炉不同，冷却器不提取能量，只用于紧急/辅助散热；冷却液直接消耗掉，不产出换热产物。
 * <p>
 * 应急激活条件：仅当本柱体热达到 {@link #ACTIVATION_HEAT}（750°C）后才开始消耗冷却液工作。
 * <p>
 * 移植说明：
 * <ul>
 * <li>柱体按 2 格网格铺设（与控制台 STEP=2 一致），故 5×5 列区域 = 本柱 ±4 格、步长 2 的 25 个扫描点。</li>
 * <li>耗液 0.5 单位/结算 tick（对应 HBM 50 mB/t 的 1/100 缩放）。</li>
 * <li>每 {@link RBMKDials#simTickEvery} 游戏 tick（等效 HBM 20tps 逐 tick）结算一次，降温 200°C 与 HBM 原值一致。</li>
 * <li>Mindustry 无竖直轴向，cryofluid 从任意方向吸入（HBM 中冷液自下，语义一致）。</li>
 * </ul>
 */
public class RBMKCooler extends RBMKBase {

    /** 冷冻液储罐容量 */
    public float tankCapacity = 400f;
    /** 每次结算的耗液量 */
    public static final float RATE = 4f;
    /** 每次结算对每个 5×5 内柱体的降温量（°C） */
    public static final float COOL = 100f;
    /** 工作温度阈值：本柱体热达到 750°C 后才开始消耗冷却液工作（应急散热激活点） */
    public static final float ACTIVATION_HEAT = 750f;

    public RBMKCooler(String name) {
        super(name);
        hasLiquids = true;
        liquidCapacity = tankCapacity;
        consoleType = ColumnType.COOLER;
        buildType = RBMKCoolerBuild::new;
    }

    @Override
    public void setBars() {
        super.setBars();
        addBar("coolant", (RBMKCoolerBuild entity) -> new Bar(
            () -> "Cryofluid: " + (int) entity.liquids.get(Liquids.cryofluid),
            () -> Pal.water,
            () -> Math.min(1f, entity.liquids.get(Liquids.cryofluid) / tankCapacity)
        ));
    }

    public class RBMKCoolerBuild extends RBMKBaseBuild {
        /** 火焰喷发间隔计数（避免每结算 tick 都喷导致粒子过密） */
        public int flameTimer;

        @Override
        public void updateTile() {
            if (!net.client()) {
                if (flameTimer > 0) flameTimer--;
                if (shouldSimulate()) {
                    cool();
                }
                // 喷火（工作中）期间，正上方单位每帧受到 10 点伤害
                if (active()) {
                    damageUnitsAbove(10f);
                }
            }
            super.updateTile();
        }

        /** 工作状态：本柱体热达到阈值且有冷却液 */
        protected boolean active() {
            return heat >= ACTIVATION_HEAT && liquids.get(Liquids.cryofluid) >= RATE;
        }

        /** 对正上方（重叠本柱占地 2×2 格区域）的单位造成伤害 */
        protected void damageUnitsAbove(float amount) {
            Damage.damageUnits(null, x, y, block.size * tilesize / 2f, amount, u -> true, u -> {});
        }

        /** 一次结算：耗冷冻液，并把 5×5 列范围内所有柱体降温 200°C（下限 20°C） */
        protected void cool() {
            Liquid coolant = Liquids.cryofluid;

            // 应急激活条件：本柱体热达到 750°C 才开始工作
            if (heat < ACTIVATION_HEAT) return;
            if (liquids.get(coolant) < RATE) return;

            liquids.remove(coolant, RATE);

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

            // 工作期间顶部喷火焰粒子（间隔喷发）
            if (flameTimer <= 0) {
                flameTimer = 12;
                Fx.fire.at(x, y, block.size);
            }
        }

        // ---------- 流体 I/O ----------

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            return liquid == Liquids.cryofluid
                && liquids.get(Liquids.cryofluid) < tankCapacity;
        }

        @Override
        public void handleLiquid(Building source, Liquid liquid, float amount) {
            if (liquid == Liquids.cryofluid) {
                liquids.add(Liquids.cryofluid, Math.min(amount, tankCapacity - liquids.get(Liquids.cryofluid)));
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
            data.put("coolant", (int) liquids.get(Liquids.cryofluid));
            data.put("maxCoolant", (int) tankCapacity);
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

        // ---------- 序列化 ----------

        @Override
        public void write(Writes write) {
            super.write(write);
            write.i(flameTimer);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            flameTimer = read.i();
        }
    }
}