package VanillaExpansion.expand.world.block.power;

import VanillaExpansion.content.RBMKFuelData;
import VanillaExpansion.content.RBMKRodItem;
import arc.Core;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.io.*;
import mindustry.gen.Building;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.Bar;

import static mindustry.Vars.*;

/**
 * RBMK 燃料柱 —— 参考 HBM's Nuclear Tech 的 RBMKRod。
 * 柱内装入一根 {@link RBMKRodItem} 燃料棒，每 tick：
 * 先按上一 tick 缓存的通量（快慢比加权）裂变 → 芯热累积 → 芯→壳扩散 → 壳热回传柱体，
 * 最后把出通量向四方向扩散，供邻近燃料柱下一 tick 使用。
 * 燃料棒运行时状态（剩余产额/氙毒/芯壳热）由 {@link RBMKFuelData.RodState} 承担，
 * 存在本 Building 内（Mindustry Item 无 NBT）。
 */
public class RBMKRod extends RBMKBase {

    /** 是否被石墨慢化（影响熔毁碎片与通量慢化） */
    public boolean moderated = false;

    public RBMKRod(String name) {
        super(name);
        hasItems = true;
        itemCapacity = 1;
        consoleType = ColumnType.FUEL;
        buildType = RBMKRodBuild::new;
    }

    @Override
    public void setBars() {
        super.setBars();
        addBar("fuel", (RBMKRodBuild entity) -> new Bar(
            () -> entity.fuelState == null ? Core.bundle.get("rbmk.bar.fuel.none") : Core.bundle.format("rbmk.bar.fuel", (int) (entity.fuelState.getEnrichment(entity.currentRod()) * 100)),
            () -> entity.fuelState == null ? Pal.gray : Pal.ammo,
            () -> entity.fuelState == null ? 0f : (float) entity.fuelState.getEnrichment(entity.currentRod())
        ));
        addBar("xenon", (RBMKRodBuild entity) -> new Bar(
            () -> Core.bundle.format("rbmk.bar.xenon", (int) (entity.fuelState == null ? 0 : entity.fuelState.getPoisonLevel() * 100)),
            () -> Pal.heal,
            () -> entity.fuelState == null ? 0f : (float) entity.fuelState.getPoisonLevel()
        ));
        addBar("core", (RBMKRodBuild entity) -> new Bar(
            () -> entity.fuelState == null || entity.fuelItem == null ? Core.bundle.get("rbmk.bar.core.none") : Core.bundle.format("rbmk.bar.core", (int) entity.fuelState.coreHeat),
            () -> entity.fuelState == null ? Pal.gray : Pal.redLight,
            () -> entity.fuelState == null || entity.fuelItem == null ? 0f : (float) (entity.fuelState.coreHeat / entity.fuelItem.meltingPoint)
        ));
        addBar("hull", (RBMKRodBuild entity) -> new Bar(
            () -> entity.fuelState == null || entity.fuelItem == null ? Core.bundle.get("rbmk.bar.hull.none") : Core.bundle.format("rbmk.bar.hull", (int) entity.fuelState.hullHeat),
            () -> entity.fuelState == null ? Pal.gray : Pal.slagOrange,
            () -> entity.fuelState == null || entity.fuelItem == null ? 0f : (float) (entity.fuelState.hullHeat / entity.fuelItem.meltingPoint)
        ));
        addBar("flux", (RBMKRodBuild entity) -> new Bar(
            () -> Core.bundle.format("rbmk.bar.rodflux", (int) entity.lastFluxOut, (int) (entity.lastFluxRatio * 100)),
            () -> Pal.reactorPurple,
            () -> Math.min(1f, (float) (entity.lastFluxOut / 1000f))
        ));
    }

    public class RBMKRodBuild extends RBMKBaseBuild {
        /** 收到的通量缓存（上一 tick 累计） */
        public double fluxQuantity;
        /** 快中子比例 [0;1] */
        public double fluxFastRatio;
        /** 显示用：上一 tick 结算完成后的通量 */
        public double lastFluxQuantity;
        public double lastFluxRatio;
        /** 显示用：上一 tick 本柱产生的出通量 */
        public double lastFluxOut;

        /** 装入的燃料棒（运行时状态） */
        public RBMKFuelData.RodState fuelState;
        public RBMKRodItem fuelItem;

        @Override
        public void updateTile() {
            if (!net.client()) {
                if (shouldSimulate()) {
                    burnTick();
                }
            }
            super.updateTile();
        }

        /** 本柱一 tick 的裂变结算 */
        protected void burnTick() {
            RBMKRodItem rod = currentRod();

            if (rod != null && fuelState != null) {

                double fluxRatioOut;
                double fluxQuantityOut;

                // 反应类型决定出通量的快慢比（对应 HBM rType）
                if (rod.rType == RBMKRodItem.NType.SLOW) {
                    fluxRatioOut = 0;
                } else {
                    fluxRatioOut = 1;
                }

                double fluxIn = fluxFromType(rod.nType);
                fluxQuantityOut = rod.burn(fuelState, fluxIn);

                rod.updateHeat(fuelState, 1.0D);
                heat += (float) rod.provideHeat(fuelState, heat, 1.0D);

                // 柱体过热
                if (heat > maxHeat()) {
                    if (RBMKDials.disableMeltdowns) {
                        onOverheat();
                    } else {
                        meltdown();
                    }
                    lastFluxRatio = 0;
                    lastFluxQuantity = 0;
                    lastFluxOut = 0;
                    fluxQuantity = 0;
                    fluxFastRatio = 0;
                    return;
                }

                if (heat > 10_000) heat = 10_000;

                // 把"收到的通量"转存为显示值，然后清空缓存，准备下一 tick 接收
                lastFluxQuantity = fluxQuantity;
                lastFluxRatio = fluxFastRatio;
                lastFluxOut = fluxQuantityOut;
                fluxQuantity = 0;
                fluxFastRatio = 0;

                // 出通量扩散给四方向（下一 tick 由邻居结算）
                spreadFlux(fluxQuantityOut, fluxRatioOut);

            } else {
                lastFluxRatio = 0;
                lastFluxQuantity = 0;
                lastFluxOut = 0;
                fluxQuantity = 0;
                fluxFastRatio = 0;
            }
        }

        // ---------- 通量 ----------

        /** 累积邻居传来的通量，按快慢比加权合并（对应 HBM receiveFlux） */
        @Override
        public void receiveFlux(double flux, double ratio) {
            double fastFlux = fluxQuantity * fluxFastRatio;
            double fastFluxIn = flux * ratio;

            fluxQuantity += flux;
            fluxFastRatio = fluxQuantity <= 0 ? 0 : (fastFlux + fastFluxIn) / fluxQuantity;
        }

        /** 按燃料最适中子类型折算可用通量（对应 HBM fluxFromType） */
        private double fluxFromType(RBMKRodItem.NType type) {
            double fastFlux = fluxQuantity * fluxFastRatio;
            double slowFlux = fluxQuantity * (1 - fluxFastRatio);

            switch (type) {
                case SLOW: return slowFlux + fastFlux * 0.5;
                case FAST: return fastFlux + slowFlux * 0.3;
                case ANY: return fluxQuantity;
            }

            return 0D;
        }

        /** 向四方向等量扩散通量（对应 HBM RBMKNeutronStream：每条流沿方向传播 fluxRange 列，遇燃料柱吸收、遇非 RBMK 方块停止） */
        public void spreadFlux(double flux, double ratio) {
            if (flux == 0) return;

            int range = RBMKDials.fluxRange;
            int s = block.size;

            int[] dx = {1, 0, -1, 0};
            int[] dy = {0, 1, 0, -1};

            for (int i = 0; i < 4; i++) {
                double stream = flux;
                double streamRatio = ratio; // 快中子比例随慢化剂逐格衰减（对应 HBM fluxRatio）
                for (int step = 1; step <= range; step++) {
                    Building nb = world.build(tileX() + dx[i] * s * step, tileY() + dy[i] * s * step);

                    if (nb instanceof RBMKBaseBuild b) {
                        RBMKType type = b.getRBMKType();
                        // 慢化剂：快中子变慢中子，衰减流体的快中子比例并继续传播（fluxRatio *= 1 - moderatorEfficiency）
                        if (type == RBMKType.MODERATOR) {
                            streamRatio *= (1D - RBMKDials.moderatorEfficiency);
                            continue;
                        }
                        // 控制棒：全插（level==0）硬阻断整条流；否则按 getMult() 缩放并继续传播
                        if (type == RBMKType.CONTROL_ROD && b instanceof RBMKControl.RBMKControlBuild cb) {
                            if (cb.level > 0) {
                                stream *= cb.getMult();
                                continue;
                            }
                            break;
                        }
                        // 燃料柱：有燃料则吸收整条流并停止传播；无燃料的空棒结构让流穿过（对应 HBM 空柱不吸收）
                        if (type == RBMKType.ROD && b instanceof RBMKRodBuild rb && rb.currentRod() != null) {
                            rb.receiveFlux(stream, streamRatio);
                            break;
                        }
                        // 反射器：效率为 1 时把剩余通量整体回授给源燃料柱并停止传播（对应 HBM 完全反射）；
                        // 否则按效率衰减后继续向后穿透（对应 HBM 部分反射）。
                        if (type == RBMKType.REFLECTOR) {
                            if (RBMKDials.reflectorEfficiency != 1.0f) {
                                stream *= RBMKDials.reflectorEfficiency;
                                streamRatio *= (1D - RBMKDials.moderatorEfficiency); // 反射前亦被慢化一次
                                continue;
                            }
                            RBMKRodBuild.this.receiveFlux(stream, streamRatio);
                            break;
                        }
                        // 吸收器：把通量转化为自身柱体热；效率为 1 时完全吸收该流并停止传播，
                        // 否则按效率衰减后继续向后穿透（对应 HBM)。
                        if (type == RBMKType.ABSORBER) {
                            b.heat += RBMKDials.absorberHeatConversion * stream;
                            if (b.heat > b.maxHeat()) b.heat = b.maxHeat();
                            if (RBMKDials.absorberEfficiency != 1.0f) {
                                stream *= RBMKDials.absorberEfficiency;
                                continue;
                            }
                            break;
                        }
                    } else if (nb != null) {
                        // 非 RBMK 实心方块挡住流；空气等空格让流继续穿过
                        break;
                    }
                }
            }
        }

        // ---------- 燃料装卸 ----------

        /** 当前装入的燃料定义（从 inventory 反查） */
        public RBMKRodItem currentRod() {
            if (fuelItem != null && items.has(fuelItem, 1)) return fuelItem;
            // 从 inventory 找回
            for (Item item : content.items()) {
                if (items.get(item) > 0 && item instanceof RBMKRodItem rod) {
                    fuelItem = rod;
                    return rod;
                }
            }
            fuelItem = null;
            return null;
        }

        @Override
        public boolean acceptItem(Building source, Item item) {
            return items.total() == 0 && item instanceof RBMKRodItem;
        }

        @Override
        public void handleItem(Building source, Item item) {
            super.handleItem(source, item);
            if (item instanceof RBMKRodItem rod) {
                fuelItem = rod;
                fuelState = RBMKFuelData.RodState.create(rod);
            }
        }

        @Override
        public int removeStack(Item item, int amount) {
            int removed = super.removeStack(item, amount);
            if (items.total() == 0) {
                fuelState = null;
                fuelItem = null;
            }
            return removed;
        }

        // ---------- 控制台 ----------

        @Override
        public boolean isModerated() {
            return RBMKRod.this.moderated;
        }

        @Override
        public RBMKType getRBMKType() {
            return RBMKType.ROD;
        }

        @Override
        public ObjectMap<String, Object> getConsoleData() {
            ObjectMap<String, Object> data = super.getConsoleData();
            if (data == null) data = new ObjectMap<>();
            if (fuelState != null && fuelItem != null) {
                data.put("enrichment", fuelState.getEnrichment(fuelItem));
                data.put("xenon", fuelState.getPoisonLevel());
                data.put("hullHeat", fuelState.hullHeat);
                data.put("coreHeat", fuelState.coreHeat);
                data.put("maxHeat", fuelItem.meltingPoint);
            } else {
                data.put("hasRod", false);
            }
            data.put("flux", lastFluxQuantity);
            data.put("fluxRatio", lastFluxRatio);
            return data;
        }

        @Override
        public double consoleFlux() {
            return lastFluxQuantity;
        }

        @Override
        public double consoleValue(RBMKBase.ScreenValue screen) {
            if (fuelState == null || fuelItem == null) return Double.NaN;
            switch (screen) {
                case FUEL_DEPLETION:
                    return 100D - fuelState.getEnrichment(fuelItem) * 100D;
                case FUEL_POISON:
                    return fuelState.getPoisonLevel() * 100D;
                case FUEL_TEMP:
                    return fuelState.coreHeat;
                default:
                    return super.consoleValue(screen);
            }
        }

        // ---------- 熔毁 ----------

        @Override
        public void onMelt(int reduce) {
            boolean moderated = RBMKRod.this.moderated;
            int h = (int) RBMKDials.columnHeight;
            reduce = Mathf.clamp(reduce, 1, h);
            if (Mathf.chance(0.333f)) reduce++;

            boolean corium = currentRod() != null;

            if (corium && fuelItem != null && fuelItem.dangerous) {
                RBMKDials.digamma = true;
            }

            fuelState = null;
            fuelItem = null;
            items.clear();

            if (corium) {
                spawnDebris(DebrisType.FUEL);
                kill();
            } else {
                standardMelt(reduce);
            }

            if (moderated) {
                spawnDebris(DebrisType.GRAPHITE);
            }

            spawnDebris(DebrisType.ELEMENT);

            if (hasLid) {
                spawnDebris(DebrisType.LID);
            }
        }

        // ---------- 渲染 ----------

        @Override
        public void draw() {
            super.draw();
            if (fuelState != null && fuelItem != null) {
                float fin = (float) fuelState.getEnrichment(fuelItem);
                Draw.z(Layer.block - 0.001f);
                Draw.color(new Color(fuelItem.colorTint << 8 | 0xff));
                Draw.alpha(0.6f + 0.4f * (1f - fin));
                Fill.square(x, y, 6f);
                Draw.reset();
            }
        }

        // ---------- 序列化 ----------

        @Override
        public void write(Writes write) {
            super.write(write);
            write.d(fluxQuantity);
            write.d(fluxFastRatio);
            write.d(lastFluxQuantity);
            write.d(lastFluxRatio);
            write.d(lastFluxOut);
            boolean has = fuelState != null;
            write.bool(has);
            if (has) fuelState.write(write);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            fluxQuantity = read.d();
            fluxFastRatio = read.d();
            lastFluxQuantity = read.d();
            lastFluxRatio = read.d();
            lastFluxOut = read.d();
            boolean has = read.bool();
            if (has) {
                fuelState = new RBMKFuelData.RodState();
                fuelState.read(read);
            } else {
                fuelState = null;
                fuelItem = null;
            }
            currentRod(); // 恢复 fuelItem 引用
        }
    }
}
