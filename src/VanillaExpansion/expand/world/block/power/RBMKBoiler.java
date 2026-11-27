package VanillaExpansion.expand.world.block.power;
import VanillaExpansion.content.VELiquids;
import arc.Core;
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
public class RBMKBoiler extends RBMKBase {
    public float feedCapacity = 100f;
    public float steamCapacity = 10000f;
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
            () -> Core.bundle.format("rbmk.bar.water", (int) entity.liquids.get(Liquids.water)),
            () -> Pal.water,
            () -> Math.min(1f, entity.liquids.get(Liquids.water) / feedCapacity)
        ));
        addBar("steam", (RBMKBoilerBuild entity) -> new Bar(
            () -> Core.bundle.format("rbmk.bar.steam", entity.steamTierName(), (int) entity.liquids.get(entity.currentSteam())),
            () -> entity.currentSteam().color,
            () -> Math.min(1f, entity.liquids.get(entity.currentSteam()) / steamCapacity)
        ));
    }
    public class RBMKBoilerBuild extends RBMKBaseBuild {
        public int steamTier;
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
        protected void boil() {
            if (ventDelay > 0) ventDelay--;
            double heatCap = getHeatFromSteam(steamTier);
            double heatProvided = heat - heatCap;
            if (heatProvided <= 0) return;
            double HEAT_PER_MB_WATER = RBMKDials.boilerHeatConsumption;
            double steamFactor = getFactorFromSteam(steamTier);
            int waterHbm;
            int steamHbm;

            if (steamTier == 3) {
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
            float waterUsed = waterHbm / fluidScale;
            float steamProduced = steamHbm / fluidScale;
            if (waterUsed <= 0) return;
            Liquid out = currentSteam();
            liquids.remove(Liquids.water, waterUsed);
            liquids.add(out, steamProduced);
            if (liquids.get(out) > steamCapacity) {
                liquids.set(out, steamCapacity);
                if (ventDelay <= 0) {
                    Fx.ventSteam.at(x + Mathf.range(8f), y + block.size * tilesize / 2f, Mathf.random(1f, 1.5f));
                    ventDelay = 20 + Mathf.random(10);
                }
            }

            heat -= (float) (waterUsed * HEAT_PER_MB_WATER * fluidScale);
        }
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
        public Liquid currentSteam() {
            return steamLiquid(steamTier);
        }
        public static Liquid steamLiquid(int tier) {
            return switch (tier) {
                case 1 -> VELiquids.hotSteam;
                case 2 -> VELiquids.superhotSteam;
                case 3 -> VELiquids.ultrahotSteam;
                default -> VELiquids.steam;
            };
        }
        public String steamTierName() {
            return Core.bundle.get("rbmk.steam." + steamTier);
        }
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
        public void cycleCompressor() {
            setSteamTier(steamTier + 1 >= 4 ? 0 : steamTier + 1);
        }

        @Override
        public void buildConfiguration(Table table) {
            table.background(Styles.black6);
            table.top().left();
            table.add(Core.bundle.get("rbmk.ui.boiler.title")).growX().center().pad(6f).row();

            Table buttons = new Table();
            buttons.center().defaults().size(120f, 48f).pad(4f);
            ButtonGroup<TextButton> group = new ButtonGroup<>();
            group.setMinCheckCount(0);
            for (int i = 0; i < 4; i++) {
                final int tier = i;
                TextButton btn = new TextButton(Core.bundle.format("rbmk.ui.boiler.tier", Core.bundle.get("rbmk.steam." + i), (int) getHeatFromSteam(i)), Styles.flatTogglet);
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

        @Override
        public void onMelt(int reduce) {
            int count = 1 + Mathf.random(2);
            for (int i = 0; i < count; i++) {
                spawnDebris(DebrisType.BLANK);
            }
            kill();
        }

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
