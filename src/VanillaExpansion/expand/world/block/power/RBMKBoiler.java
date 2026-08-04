package VanillaExpansion.expand.world.block.power;

import VanillaExpansion.content.VELiquids;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.io.*;
import mindustry.content.Fx;
import mindustry.content.Liquids;
import mindustry.gen.Building;
import mindustry.graphics.*;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.ui.Styles;

import static mindustry.Vars.*;

/**
 * RBMK 锅炉 —— 参考 HBM's Nuclear Tech 的 TileEntityRBMKBoiler / RBMKBoiler。
 * <p>
 * 从柱体取热把水烧成蒸汽，是冷却 RBMK 与提取能量的主要手段。
 * 只从任何方向接收水，产出当前档位对应的蒸汽液体并向四周输出
 * （Mindustry 无竖直轴向，故水/汽进出均取四向；HBM 中水自下方、汽自上方，语义一致）。
 * <p>
 * 每 {@link RBMKDials#simTickEvery} 游戏 tick（等效 HBM 20tps 逐 tick）结算一次烧水：
 * <pre>
 * heatProvided = heat - heatCap                       // heatCap 随蒸汽档位（STEAM=100/HOT=300/SUPERHOT=450/ULTRAHOT=600）
 * waterUsed    = min( floor(heatProvided / 100), 水存量 )  // 0.1 × 1000 缩放后
 * steamProduced= floor( waterUsed * 100 / steamFactor )
 * heat        -= waterUsed * 100
 * </pre>
 * 入水/出汽量按 {@link #fluidScale}（1/100）缩放以适配 Mindustry 管道运力，
 * 每 mB 水耗热同步 ×100，保持柱体冷却平衡与 HBM 原值一致；储罐容量同步 ×1/100。
 * 蒸汽罐满时钳位并安全阀排汽（粒子 + 冷却），保证即使输出受阻仍持续消耗热量冷却柱体。
 * <p>
 * 蒸汽档位（对应 HBM getHeatFromSteam / getFactorFromSteam，压缩倍率 1/3/10/30）
 * 可通过方块配置 UI 直接切换；切换时存量按档位系数换算迁移（等效 HBM cyceCompressor）。
 */
public class RBMKBoiler extends RBMKBase {

    /** 进水容量（HBM feed tank = 10000 mB，按 1/100 缩放） */
    public float feedCapacity = 100f;
    /** 蒸汽容量（HBM steam tank = 1000000 mB，按 1/100 缩放） */
    public float steamCapacity = 10000f;
    /**
     * 流体缩放系数：HBM 以 mB 计的入水/出汽量按 1/1000 缩小（Mindustry 管道运力不足），
     * 同时每 mB 水耗热 ×1000 以保持柱体冷却平衡不变。
     */
    public static final float fluidScale = 1000f;

    public RBMKBoiler(String name) {
        super(name);
        hasLiquids = true;
        outputsLiquid = true;
        liquidCapacity = steamCapacity;
        consoleType = ColumnType.BOILER;
        configurable = true;
        config(Integer.class, (RBMKBoilerBuild tile, Integer tier) -> tile.setSteamTier(tier));
        configClear((RBMKBoilerBuild tile) -> tile.setSteamTier(0));
        buildType = RBMKBoilerBuild::new;
    }

    @Override
    public void setBars() {
        super.setBars();
        addBar("water", (RBMKBoilerBuild entity) -> new Bar(
            () -> "Water: " + (int) entity.liquids.get(Liquids.water),
            () -> Pal.water,
            () -> Math.min(1f, entity.liquids.get(Liquids.water) / feedCapacity)
        ));
        addBar("steam", (RBMKBoilerBuild entity) -> new Bar(
            () -> entity.steamTierName() + ": " + (int) entity.liquids.get(entity.currentSteam()),
            () -> entity.currentSteam().color,
            () -> Math.min(1f, entity.liquids.get(entity.currentSteam()) / steamCapacity)
        ));
    }

    public class RBMKBoilerBuild extends RBMKBaseBuild {
        /** 蒸汽档位：0=STEAM 1=HOTSTEAM 2=SUPERHOTSTEAM 3=ULTRAHOTSTEAM */
        public int steamTier;
        /** 排汽冷却 */
        public int ventDelay;

        @Override
        public void updateTile() {
            if (!net.client()) {
                if (shouldSimulate()) {
                    boil();
                }
                dumpLiquid(currentSteam());
            }
            super.updateTile();
        }

        // ---------- 烧水 ----------

        /** 一 tick 的烧水结算（数值按 HBM TileEntityRBMKBoiler.updateEntity 原样移植） */
        protected void boil() {
            if (ventDelay > 0) ventDelay--;

            double heatCap = getHeatFromSteam(steamTier);
            double heatProvided = heat - heatCap;
            if (heatProvided <= 0) return;

            double HEAT_PER_MB_WATER = RBMKDials.boilerHeatConsumption; // 0.1，HBM 原始 mB 级
            double steamFactor = getFactorFromSteam(steamTier);

            // 先按 HBM 原式求 mB 级入水/出汽量
            int waterHbm;
            int steamHbm;

            if (steamTier == 3) { // ULTRAHOTSTEAM 特殊算式
                steamHbm = (int) Math.floor((heatProvided / HEAT_PER_MB_WATER) * 100D / steamFactor);
                waterHbm = (int) Math.floor(steamHbm / 100D * steamFactor);
                float feed = liquids.get(Liquids.water);
                if (feed < waterHbm / fluidScale) {
                    steamHbm = (int) Math.floor(feed * fluidScale * 100D / steamFactor);
                    waterHbm = (int) Math.floor(steamHbm / 100D * steamFactor);
                }
            } else {
                waterHbm = (int) Math.floor(heatProvided / HEAT_PER_MB_WATER);
                waterHbm = Math.min(waterHbm, (int) (liquids.get(Liquids.water) * fluidScale));
                steamHbm = (int) Math.floor((waterHbm * 100D) / steamFactor);
            }

            // 缩放至 Mindustry 运力级：保留浮点精度，高压缩档（ULTRAHOT）产量不足 1 也不会被截断为 0
            float waterUsed = waterHbm / fluidScale;
            float steamProduced = steamHbm / fluidScale;

            if (waterUsed <= 0) return;

            Liquid out = currentSteam();
            liquids.remove(Liquids.water, waterUsed);
            liquids.add(out, steamProduced);

            // 蒸汽罐超压：钳位 + 安全阀排汽
            if (liquids.get(out) > steamCapacity) {
                liquids.set(out, steamCapacity);
                if (ventDelay <= 0) {
                    Fx.ventSteam.at(x + Mathf.range(8f), y + block.size * tilesize / 2f, Mathf.random(1f, 1.5f));
                    ventDelay = 20 + Mathf.random(10);
                }
            }

            heat -= (float) (waterUsed * HEAT_PER_MB_WATER * fluidScale);
        }

        // ---------- 蒸汽档位（对应 HBM getHeatFromSteam / getFactorFromSteam） ----------

        public static double getHeatFromSteam(int tier) {
            return switch (tier) {
                case 1 -> 300D;
                case 2 -> 450D;
                case 3 -> 600D;
                default -> 100D;
            };
        }

        public static double getFactorFromSteam(int tier) {
            return switch (tier) {
                case 1 -> 3D;
                case 2 -> 10D;
                case 3 -> 30D;
                default -> 1D;
            };
        }

        /** 当前档位对应的蒸汽液体 */
        public Liquid currentSteam() {
            return steamLiquid(steamTier);
        }

        /** 档位 → 蒸汽液体（对应 HBM 的 4 档蒸汽流体类型） */
        public static Liquid steamLiquid(int tier) {
            return switch (tier) {
                case 1 -> VELiquids.hotSteam;
                case 2 -> VELiquids.superhotSteam;
                case 3 -> VELiquids.ultrahotSteam;
                default -> VELiquids.steam;
            };
        }

        /** 档位显示名 */
        public String steamTierName() {
            return switch (steamTier) {
                case 1 -> "HOT";
                case 2 -> "SUPERHOT";
                case 3 -> "ULTRAHOT";
                default -> "STEAM";
            };
        }

        /**
         * 切换蒸汽档位：存量按档位系数换算迁移（对应 HBM cyceCompressor 的 ÷10/×1000 语义）。
         * 换算依据：不同档位产出系数 factor=1/3/10/30，同一团蒸汽在不同档位下体积比例即 factor 之比。
         */
        public void setSteamTier(int tier) {
            int t = Mathf.clamp(tier, 0, 3);
            if (t == steamTier) return;

            Liquid oldL = steamLiquid(steamTier);
            Liquid newL = steamLiquid(t);
            float oldAmount = liquids.get(oldL);
            double factor = getFactorFromSteam(steamTier) / getFactorFromSteam(t);

            liquids.remove(oldL, oldAmount);
            liquids.add(newL, Math.min((float) (oldAmount * factor), steamCapacity));
            steamTier = t;
        }

        /** 压缩机循环：STEAM→HOT→SUPERHOT→ULTRAHOT→STEAM（对应 HBM cyceCompressor） */
        public void cycleCompressor() {
            setSteamTier(steamTier + 1 >= 4 ? 0 : steamTier + 1);
        }

        // ---------- 档位切换 UI ----------

        @Override
        public void buildConfiguration(Table table) {
            table.background(Styles.black6);
            table.top().left();
            table.add("[orange]Steam Tier").growX().center().pad(6f).row();

            Table buttons = new Table();
            buttons.center().defaults().size(120f, 48f).pad(4f);
            ButtonGroup<TextButton> group = new ButtonGroup<>();
            group.setMinCheckCount(0);

            String[] names = {"STEAM", "HOT", "SUPERHOT", "ULTRAHOT"};
            for (int i = 0; i < 4; i++) {
                final int tier = i;
                TextButton btn = new TextButton(names[i] + "\n[gray]" + (int) getHeatFromSteam(i) + "°C", Styles.flatTogglet);
                btn.setChecked(steamTier == tier);
                btn.update(() -> btn.setChecked(steamTier == tier));
                btn.changed(() -> configure(tier));
                group.add(btn);
                buttons.add(btn);
                if (i % 2 == 1) buttons.row();
            }
            table.add(buttons).growX().center().pad(4f);
        }

        @Override
        public Integer config() {
            return steamTier;
        }

        // ---------- 流体 I/O ----------

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            return liquid == Liquids.water && liquids.get(Liquids.water) < feedCapacity;
        }

        @Override
        public void handleLiquid(Building source, Liquid liquid, float amount) {
            if (liquid == Liquids.water) {
                liquids.add(Liquids.water, Math.min(amount, feedCapacity - liquids.get(Liquids.water)));
            }
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

        // ---------- 控制台 API ----------

        @Override
        public ColumnType getConsoleType() {
            return ColumnType.BOILER;
        }

        @Override
        public ObjectMap<String, Object> getConsoleData() {
            ObjectMap<String, Object> data = super.getConsoleData();
            if (data == null) data = new ObjectMap<>();
            data.put("water", (int) liquids.get(Liquids.water));
            data.put("maxWater", (int) feedCapacity);
            data.put("steam", (int) liquids.get(currentSteam()));
            data.put("steamLiquid", currentSteam().name);
            data.put("maxSteam", (int) steamCapacity);
            data.put("steamTier", steamTier);
            return data;
        }

        // ---------- 序列化 ----------

        @Override
        public void write(Writes write) {
            super.write(write);
            write.i(steamTier);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            steamTier = read.i();
        }
    }
}
