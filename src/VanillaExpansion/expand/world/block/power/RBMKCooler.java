package VanillaExpansion.expand.world.block.power;
import arc.Core;
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
public class RBMKCooler extends RBMKBase {
    public float tankCapacity = 400f;
    public static final float RATE = 4f;
    public static final float COOL = 100f;
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
            () -> Core.bundle.format("rbmk.bar.coolant", (int) entity.liquids.get(Liquids.cryofluid)),
            () -> Pal.water,
            () -> Math.min(1f, entity.liquids.get(Liquids.cryofluid) / tankCapacity)
        ));
    }
    public class RBMKCoolerBuild extends RBMKBaseBuild {
        public int flameTimer;
        @Override
        public void updateTile() {
            if (!net.client()) {
                if (flameTimer > 0) flameTimer--;
                if (shouldSimulate()) {
                    cool();
                }
                if (active()) {
                    damageUnitsAbove(10f);
                }
            }
            super.updateTile();
        }
        protected boolean active() {
            return heat >= ACTIVATION_HEAT && liquids.get(Liquids.cryofluid) >= RATE;
        }
        protected void damageUnitsAbove(float amount) {
            Damage.damageUnits(null, x, y, block.size * tilesize / 2f, amount, u -> true, u -> {});
        }
        protected void cool() {
            Liquid coolant = Liquids.cryofluid;
            if (heat < ACTIVATION_HEAT) return;
            if (liquids.get(coolant) < RATE) return;
            liquids.remove(coolant, RATE);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    Building b = world.build(tileX() + dx * 2, tileY() + dz * 2);
                    if (b instanceof RBMKBaseBuild rb) {
                        rb.heat -= COOL;
                        if (rb.heat < 20) rb.heat = 20f;
                    }
                }
            }
            if (flameTimer <= 0) {
                flameTimer = 12;
                Fx.fire.at(x, y, block.size);
            }
        }
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
        @Override
        public void onMelt(int reduce) {
            int count = 1 + Mathf.random(2);
            for (int i = 0; i < count; i++) {
                spawnDebris(DebrisType.BLANK);
            }
            kill();
        }
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