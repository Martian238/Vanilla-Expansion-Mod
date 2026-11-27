package VanillaExpansion.expand.world.block.power;
import arc.Core;
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
        for(int i = 0; i < 16; i++){
            float fin = Mathf.curve(e.fin(), r.random(0f, 0.4f), 1f);
            float ang = r.random(360f);
            float len = r.random(8f, 40f) * Interp.pow2Out.apply(fin);
            Tmp.v1.trns(ang, len).add(e.x, e.y);
            Draw.color(Pal.darkMetal, Pal.lightishGray, fin);
            Fill.square(Tmp.v1.x, Tmp.v1.y, r.random(1.5f, 3.5f) * (1f - fin), r.random(360f));
        }
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
            () -> Core.bundle.format("rbmk.bar.heat", (int) entity.heat),
            () -> Pal.lightOrange,
            () -> entity.heat / entity.maxHeat()
        ));
    }

    public class RBMKBaseBuild extends Building {
        public float heat = 25f;
        public int reasimWater, reasimSteam;
        public static final int maxWater = 16000;
        public static final int maxSteam = 16000;
        public int craneIndicator;

        public boolean hasLid;
        public int lidType;

        protected boolean melting = false;

        public float maxHeat() {
            return 1500f;
        }

        public float passiveCooling(int neighbors) {
            float min = RBMKDials.passiveCoolingInner;
            float max = RBMKDials.passiveCooling;
            return min + (max - min) * ((4 - Mathf.clamp(neighbors, 0, 4)) / 4f);
        }

        protected void coolPassively(int neighbors) {
            heat -= passiveCooling(neighbors);
            if (heat < 20) heat = 20f;
        }
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
                if (reasim) {
                    reasimWater += waterTot % members;
                    reasimSteam += steamTot % members;
                }
            }

            coolPassively(members - 1);
        }
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
                b.spawnCorium();
            }
            if (RBMKDials.enableOverpressure) onOverpressure(cols);
            float avgX = (minX + maxX) / 2f + 0.5f;
            float avgY = (minY + maxY) / 2f + 0.5f;
            int smallDim = Math.min(maxX - minX, maxY - minY);
            Fx.bigShockwave.at(avgX * tilesize, avgY * tilesize, smallDim);
            Sounds.explosion.at(avgX * tilesize, avgY * tilesize, 50f, 1f);
            if(LensShockwaveFX.inst != null){
                LensShockwaveFX.inst.spawnShock(avgX * tilesize, avgY * tilesize, Math.max(smallDim, 2) * tilesize * 0.7f, 55f, 42f);
            }
            melting = false;
        }
        public void spawnCorium() {
            if (net.client()) return;
            int tx = tileX(), ty = tileY();
            int radius = 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (Mathf.dst2(dx, dy) > radius * radius) continue;
                    Tile tile = world.tile(tx + dx, ty + dy);
                    if (tile == null || !tile.block().isAir()) continue;
                    Puddles.deposit(tile, VELiquids.moltenCore, Mathf.random(25f, 45f));
                }
            }
            Fx.explosion.at(x, y, 4f);
            Fx.fire.at(x, y, 4f);
            Fx.fireballsmoke.at(x, y);
            Fx.blastsmoke.at(x, y);
            if(LensShockwaveFX.inst != null){
                LensShockwaveFX.inst.spawnShock(x, y, Mathf.random(20f, 28f), Mathf.random(30f, 40f), Mathf.random(16f, 24f));
            }
            metalSpray.at(x, y, Mathf.random(360f));
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
        private void getFF(RBMKBaseBuild b, ObjectSet<RBMKBaseBuild> cols) {
            if (cols.contains(b)) return;
            cols.add(b);
            for (Building nb : new Building[]{b.front(), b.right(), b.back(), b.left()}) {
                if (nb instanceof RBMKBaseBuild rbmk) getFF(rbmk, cols);
            }
        }
        protected void onOverpressure(ObjectSet<RBMKBaseBuild> cols) {}
        public void onMelt(int reduce) {
            standardMelt(reduce);
            if (hasLid) spawnDebris(DebrisType.LID);
        }
        protected void standardMelt(int reduce) {
            int h = (int) RBMKDials.columnHeight;
            reduce = Mathf.clamp(reduce, 1, h);
            if (Mathf.chance(0.33f)) reduce++;
            spawnDebris(DebrisType.BLANK);
            kill();
        }
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
            meltDebris.at(x, y, Mathf.random(360f));
        }
        public void onOverheat() {}

        public boolean isModerated() {
            return false;
        }
        public void receiveFlux(double flux, double ratio) {}
        public RBMKType getRBMKType() {
            return RBMKType.OTHER;
        }
        public ColumnType getConsoleType() {
            return consoleType;
        }
        public ObjectMap<String, Object> getConsoleData() {
            return null;
        }
        public double consoleFlux() {
            return 0;
        }
        public double consoleValue(ScreenValue screen) {
            switch (screen) {
                case COL_TEMP:
                    return heat;
                default:
                    return Double.NaN;
            }
        }
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
