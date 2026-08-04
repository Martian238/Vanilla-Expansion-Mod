package VanillaExpansion.expand.world.block.power;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import VanillaExpansion.Utils;
import VanillaExpansion.content.*;
import VanillaExpansion.entities.*;
import VanillaExpansion.expand.graphics.LensShockwaveFX;
import VanillaExpansion.expand.world.block.SixteenDirectionBlock;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.entities.Puddles;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Sounds;
import mindustry.graphics.*;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.Tile;

import static mindustry.Vars.*;


public class RBMKBase extends Block {

    public static final Effect meltDebris = new Effect(120f, e -> {
        float fin = e.fin();
        Draw.z(Layer.effect);
        float px = e.x + Angles.trnsx(e.rotation, 70f * fin);
        float py = e.y + Angles.trnsy(e.rotation, 70f * fin);
        Draw.color(Pal.darkMetal, Pal.lightishGray, fin);
        Fill.square(px, py, 4f * (1f - fin * 0.6f), e.rotation + fin * 720f);
    });

    public static final Effect metalSpray = new Effect(45f, 400f, e -> {
        Draw.z(Layer.effect);
        Rand r = Utils.rand;
        r.setSeed(e.id);
        // 金属碎屑
        for(int i = 0; i < 16; i++){
            float fin = Mathf.curve(e.fin(), r.random(0f, 0.4f), 1f);
            float ang = r.random(360f);
            float len = r.random(8f, 40f) * Interp.pow2Out.apply(fin);
            Tmp.v1.trns(ang, len).add(e.x, e.y);
            Draw.color(Pal.darkMetal, Pal.lightishGray, fin);
            Fill.square(Tmp.v1.x, Tmp.v1.y, r.random(1.5f, 3.5f) * (1f - fin), r.random(360f));
        }
        // 火星
        for(int i = 0; i < 12; i++){
            float fin = Mathf.curve(e.fin(), r.random(0f, 0.2f), 1f);
            float ang = r.random(360f);
            float len = r.random(12f, 55f) * Interp.pow3Out.apply(fin);
            Tmp.v1.trns(ang, len).add(e.x, e.y);
            Draw.color(Pal.lightOrange, Pal.lighterOrange, fin);
            Fill.circle(Tmp.v1.x, Tmp.v1.y, r.random(1f, 2.5f) * (1f - fin));
        }
    });

    public enum ColumnType {
        BLANK(0), FUEL(10), FUEL_SIM(90), CONTROL(20), CONTROL_AUTO(30), BOILER(40),
        MODERATOR(50), ABSORBER(60), REFLECTOR(70), OUTGASSER(80), BREEDER(100),
        STORAGE(110), COOLER(120), HEATEX(130), BURNER(140);

        public final int offset;

        ColumnType(int offset) {
            this.offset = offset;
        }
    }

    public enum RBMKType {
        ROD, MODERATOR, CONTROL_ROD, REFLECTOR, ABSORBER, OUTGASSER, OTHER
    }

    public enum ScreenValue {
        NONE, COL_TEMP, ROD_EXTRACTION, FUEL_DEPLETION, FUEL_POISON, FUEL_TEMP
    }

    public enum DebrisType {
        LID, ROD, ELEMENT, GRAPHITE, BLANK, FUEL
    }

    protected ColumnType consoleType = ColumnType.BLANK;

    public RBMKBase(String name) {
        super(name);
        size = 2;
        update = true;
        sync = true;
        solid = true;
        destructible = true;
        buildType = RBMKBaseBuild::new;
    }

    @Override
    public void setBars() {
        super.setBars();
        addBar("heat", (RBMKBaseBuild entity) -> new Bar(
            () -> "Heat: " + (int) entity.heat + "°C",
            () -> Pal.lightOrange,
            () -> entity.heat / entity.maxHeat()
        ));
    }

    public class RBMKBaseBuild extends Building {
        public float heat = 25f;                 // 柱体热
        public int reasimWater, reasimSteam;     // ReaSim 模式水/汽
        public static final int maxWater = 16000;
        public static final int maxSteam = 16000;
        public int craneIndicator;

        public boolean hasLid;
        public int lidType;                      // 0 无盖 1 混凝土盖 2 铅玻璃盖

        protected boolean melting = false;

        public float maxHeat() {
            return 1500f;
        }

        public float passiveCooling(int neighbors) {
            float min = RBMKDials.passiveCoolingInner; // 0.1
            float max = RBMKDials.passiveCooling;      // 2.5
            return min + (max - min) * ((4 - Mathf.clamp(neighbors, 0, 4)) / 4f);
        }

        protected void coolPassively(int neighbors) {
            heat -= passiveCooling(neighbors);
            if (heat < 20) heat = 20f;
        }

        /** ReaSim 锅炉模式：所有部件都可产汽 */
        public void boilWater() {
            if (heat < 100f) return;

            float heatConsumption = RBMKDials.boilerHeatConsumption;
            float availableHeat = (heat - 100f) / heatConsumption;
            float availableWater = reasimWater;
            float availableSpace = maxSteam - reasimSteam;

            int processedWater = (int) Math.floor(
                Math.min(availableHeat, Math.min(availableWater, availableSpace))
                * Mathf.clamp(RBMKDials.reasimBoilerSpeed, 0f, 1f));

            if (processedWater <= 0) return;

            reasimWater -= processedWater;
            reasimSteam += processedWater;
            heat -= processedWater * heatConsumption;
        }

        /** 以相对公平的方式向邻柱移动热量 */
        public void moveHeat() {
            boolean reasim = RBMKDials.reasimBoilers;

            Seq<RBMKBaseBuild> rec = new Seq<>();
            rec.add(this);
            float heatTot = heat;
            int waterTot = reasimWater, steamTot = reasimSteam;

            for (Building nb : new Building[]{front(), right(), back(), left()}) {
                if (nb instanceof RBMKBaseBuild b) {
                    rec.add(b);
                    heatTot += b.heat;
                    if (reasim) {
                        waterTot += b.reasimWater;
                        steamTot += b.reasimSteam;
                    }
                }
            }

            int members = rec.size;
            float stepSize = RBMKDials.columnHeatFlow; // 0.2

            if (members > 1) {
                float targetHeat = heatTot / members;

                for (RBMKBaseBuild b : rec) {
                    float delta = targetHeat - b.heat;
                    b.heat += delta * stepSize;
                    if (reasim) {
                        b.reasimWater = waterTot / members;
                        b.reasimSteam = steamTot / members;
                    }
                }
                // 把取整损失补给自身
                if (reasim) {
                    reasimWater += waterTot % members;
                    reasimSteam += steamTot % members;
                }
            }

            coolPassively(members - 1);
        }

        /**
         * 游戏 tick 结算一次，等效 20tps。用全局 tick 计数保证所有柱体同相。
         */
        protected boolean shouldSimulate() {
            return (int) Math.round(state.tick) % RBMKDials.simTickEvery == 0;
        }

        @Override
        public void updateTile() {
            if (!net.client()) {
                if (!shouldSimulate()) return;
                if (craneIndicator > 0) craneIndicator--;
                moveHeat();
                if (RBMKDials.reasimBoilers) boilWater();
            }
        }

        // ---------- 熔毁 ----------
        public void meltdown() {
            if (net.client() || melting) return;
            melting = true;

            ObjectSet<RBMKBaseBuild> cols = new ObjectSet<>();
            getFF(this, cols);

            int minX = tileX(), maxX = tileX(), minY = tileY(), maxY = tileY();
            for (RBMKBaseBuild b : cols) {
                if (b.tileX() < minX) minX = b.tileX();
                if (b.tileX() > maxX) maxX = b.tileX();
                if (b.tileY() < minY) minY = b.tileY();
                if (b.tileY() > maxY) maxY = b.tileY();
            }

            for (RBMKBaseBuild b : cols) {
                int distFromMinX = b.tileX() - minX;
                int distFromMaxX = maxX - b.tileX();
                int distFromMinY = b.tileY() - minY;
                int distFromMaxY = maxY - b.tileY();
                int minDist = Math.min(distFromMinX, Math.min(distFromMaxX, Math.min(distFromMinY, distFromMaxY)));
                b.onMelt(minDist + 1);
                // 每个熔毁结构都在自身位置生成熔融堆芯液体、剧烈火焰与 60% 概率的 16 向残骸方块
                b.spawnCorium();
            }

            // 超压事件钩子
            if (RBMKDials.enableOverpressure) onOverpressure(cols);

            // 蘑菇云
            float avgX = (minX + maxX) / 2f + 0.5f;
            float avgY = (minY + maxY) / 2f + 0.5f;
            int smallDim = Math.min(maxX - minX, maxY - minY);
            Fx.bigShockwave.at(avgX * tilesize, avgY * tilesize, smallDim);
            Sounds.explosion.at(avgX * tilesize, avgY * tilesize, 50f, 1f);

            // GL 透镜冲击波：以熔毁区域中心为主爆（本地全屏折射）
            if(LensShockwaveFX.inst != null){
                LensShockwaveFX.inst.spawnShock(avgX * tilesize, avgY * tilesize, Math.max(smallDim, 2) * tilesize * 0.7f, 55f, 42f);
            }

            melting = false;
        }

        /** 本柱熔毁：在自身位置生成熔融堆芯液体、剧烈火焰与 60% 概率的 16 向残骸方块 */
        public void spawnCorium() {
            if (net.client()) return;

            int tx = tileX(), ty = tileY();

            // 熔融堆芯以水洼形式在本结构周围扩散
            int radius = 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (Mathf.dst2(dx, dy) > radius * radius) continue;
                    Tile tile = world.tile(tx + dx, ty + dy);
                    if (tile == null || !tile.block().isAir()) continue;
                    Puddles.deposit(tile, VELiquids.moltenCore, Mathf.random(25f, 45f));
                }
            }

            // 剧烈火焰 + 蘑菇烟
            Fx.explosion.at(x, y, 4f);
            Fx.fire.at(x, y, 4f);
            Fx.fireballsmoke.at(x, y);
            Fx.blastsmoke.at(x, y);

            // GL 透镜冲击波（本柱小范围折射）+ 小范围金属粒子喷射
            if(LensShockwaveFX.inst != null){
                LensShockwaveFX.inst.spawnShock(x, y, Mathf.random(20f, 28f), Mathf.random(30f, 40f), Mathf.random(16f, 24f));
            }
            metalSpray.at(x, y, Mathf.random(360f));

            // 60% 概率留下 16 向随机朝向的残骸方块
            if (Mathf.chance(0.6f)) {
                Tile center = world.tile(tx, ty);
                if (center != null && center.block().isAir()) {
                    int rot = Mathf.random(15);
                    center.setBlock(VEBlocks.rbmkWreckage, Team.derelict, rot / 4);
                    if (center.build instanceof SixteenDirectionBlock.SixteenDirectionBuild sd) {
                        sd.configured(null, rot);
                    }
                }
            }
        }

        /** 洪泛搜索（四方向） */
        private void getFF(RBMKBaseBuild b, ObjectSet<RBMKBaseBuild> cols) {
            if (cols.contains(b)) return;
            cols.add(b);
            for (Building nb : new Building[]{b.front(), b.right(), b.back(), b.left()}) {
                if (nb instanceof RBMKBaseBuild rbmk) getFF(rbmk, cols);
            }
        }

        /** 熔毁时的超压事件（默认空实现，供管道/流体子类覆写） */
        protected void onOverpressure(ObjectSet<RBMKBaseBuild> cols) {}

        /** 每柱熔毁回调：默认标准烧毁 + 有盖则飞盖 */
        public void onMelt(int reduce) {
            standardMelt(reduce);
            if (hasLid) spawnDebris(DebrisType.LID);
        }

        /** 标准熔毁：按深度烧毁本柱并散落碎片 */
        protected void standardMelt(int reduce) {
            int h = (int) RBMKDials.columnHeight;
            reduce = Mathf.clamp(reduce, 1, h);
            if (Mathf.chance(0.33f)) reduce++;
            spawnDebris(DebrisType.BLANK);
            kill();
        }

        /** 散落碎片（服务器端）：生成带物理与贴图的飞溅碎片（对应 HBM spawnDebris + EntityRBMKDebris，渲染复用 Fragmentation） */
        public void spawnDebris(DebrisType type) {
            if (net.client()) return;
            int count = switch (type) {
                case FUEL -> 1 + Mathf.random((int) RBMKDials.columnHeight); // HBM: 1 + rand(columnHeight)
                case GRAPHITE, ROD -> 2 + Mathf.random(1);                    // HBM: 2 + rand(2)
                default -> 1;
            };
            for (int i = 0; i < count; i++) {
                RBMKDebrisEntity.create(x + Mathf.range(8f), y + Mathf.range(8f), type, block.region);
            }
            // 一次性视觉爆裂（配合物理碎片）
            meltDebris.at(x, y, Mathf.random(360f));
        }

        /** 过热点火钩子（熔毁禁用时的替代） */
        public void onOverheat() {
            // 默认无动作，子类可覆写
        }

        // ---------- 控制台 API ----------

        /** 是否被慢化（对应方块级 moderated 标志） */
        public boolean isModerated() {
            return false;
        }

        /** 接收来自邻近柱的一条中子流（默认忽略，燃料柱/吸收器/放气器覆写） */
        public void receiveFlux(double flux, double ratio) {}

        public RBMKType getRBMKType() {
            return RBMKType.OTHER;
        }

        public ColumnType getConsoleType() {
            return consoleType;
        }

        /** 返回给控制台显示的自定义数据（富集/氙毒/棒位等），子类覆写 */
        public ObjectMap<String, Object> getConsoleData() {
            return null;
        }

        /**
         * 返回给控制台统计的"本柱此刻的中子出通量"，用于通量曲线（fluxBuffer）。
         * 默认 0，燃料柱覆写为上一结算周期的出通量。
         */
        public double consoleFlux() {
            return 0;
        }

        /**
         * 控制台屏显指标取值。返回已按显示单位归一化的数值（COL_TEMP/FUEL_TEMP 为 °C，
         * ROD_EXTRACTION/FUEL_DEPLETION/FUEL_POISON 为 %），本柱不适用该指标时返回 NaN，
         * 控制台据此跳过该列，保证平局统计不受空白/无关柱污染。
         */
        public double consoleValue(ScreenValue screen) {
            switch (screen) {
                case COL_TEMP:
                    return heat;
                default:
                    return Double.NaN;
            }
        }

        // ---------- 序列化 ----------

        @Override
        public void write(Writes write) {
            super.write(write);
            write.f(heat);
            write.i(reasimWater);
            write.i(reasimSteam);
            write.i(craneIndicator);
            write.bool(hasLid);
            write.b(lidType);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            heat = read.f();
            reasimWater = read.i();
            reasimSteam = read.i();
            craneIndicator = read.i();
            hasLid = read.bool();
            lidType = read.b();
        }
    }
}
