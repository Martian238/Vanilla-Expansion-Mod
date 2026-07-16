package VanillaExpansion.expand.world.block.multicrafter;

import arc.Core;
import arc.graphics.g2d.*;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Eachable;
import arc.util.Nullable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.Tile;
import mindustry.world.blocks.ItemSelection;
import mindustry.world.blocks.liquid.LiquidBlock;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class LiquidSorter extends LiquidBlock {
    public boolean invert = false;
    public TextureRegion cross;

    public LiquidSorter(String name) {
        super(name);
        update = false;
        destructible = true;
        underBullets = true;
        instantTransfer = true;
        group = BlockGroup.transportation;
        configurable = true;
        unloadable = false;
        saveConfig = true;
        clearOnDoubleTap = true;
        hasItems = false;

        config(Liquid.class, (LiquidSorterBuild tile, Liquid liquid) -> tile.sortLiquid = liquid);
        configClear((LiquidSorterBuild tile) -> tile.sortLiquid = null);
    }

    @Override
    public void setBars() {
        super.setBars();
        removeBar("liquid");
    }

    @Override
    public void load() {
        super.load();
        cross = Core.atlas.find(name + "-cross", "cross-full");
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.remove(Stat.liquidCapacity);
    }

    @Override
    public void drawPlanConfig(BuildPlan plan, Eachable<BuildPlan> list) {
        drawPlanConfigCenter(plan, plan.config, "center", true);
    }

    @Override
    public boolean outputsItems() {
        return false;
    }

    @Override
    public int minimapColor(Tile tile) {
        var build = (LiquidSorterBuild) tile.build;
        return build == null || build.sortLiquid == null ? 0 : build.sortLiquid.color.rgba();
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region};
    }

    private boolean shouldForward(Liquid liquid, Liquid sortLiquid) {
        if (sortLiquid == null) return !invert;
        return (liquid == sortLiquid) != invert;
    }

    public class LiquidSorterBuild extends LiquidBuild {
        public @Nullable Liquid sortLiquid;
        private int rotation = 0;

        @Override
        public void configured(Unit player, Object value) {
            super.configured(player, value);
            if (!headless) renderer.minimap.update(tile);
        }

        @Override
        public void draw() {
            if (sortLiquid == null) {
                Draw.rect(cross, x, y);
            } else {
                Draw.color(sortLiquid.color);
                Fill.circle(x, y, tilesize / 2f - 0.5f);
                Draw.color();
            }
            Draw.rect(region, x, y);
        }

        private int[] getOutputDirs(Liquid liquid, int inputDir) {
            if (inputDir == -1) return new int[0];
            int outputBase = inputDir;
            if (shouldForward(liquid, sortLiquid)) {
                return new int[]{outputBase};
            } else {
                int left = Mathf.mod(outputBase - 1, 4);
                int right = Mathf.mod(outputBase + 1, 4);
                return new int[]{left, right};
            }
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            if (!enabled) return false;
            int dir = source.relativeTo(tile.x, tile.y);
            if (dir == -1) return false;

            int[] dirs = getOutputDirs(liquid, dir);
            for (int d : dirs) {
                Building next = nearby(d);
                if (next != null && next.acceptLiquid(this, liquid)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void handleLiquid(Building source, Liquid liquid, float amount) {
            int dir = source.relativeTo(tile.x, tile.y);
            if (dir == -1) return;

            int[] dirs = getOutputDirs(liquid, dir);
            Seq<Building> targets = new Seq<>();

            for (int d : dirs) {
                Building next = nearby(d);
                if (next != null && next.acceptLiquid(this, liquid)) {
                    targets.add(next);
                }
            }

            if (targets.isEmpty()) return;

            if (targets.size == 1) {
                targets.first().handleLiquid(this, liquid, amount);
            } else {
                int idx = rotation & 1;
                Building to = targets.get(idx);
                to.handleLiquid(this, liquid, amount);
                rotation ^= 1;
            }
        }

        @Override
        public Building getLiquidDestination(Building source, Liquid liquid) {
            return this;
        }

        @Override
        public void updateTile() {
        }

        @Override
        public void buildConfiguration(Table table) {
            ItemSelection.buildTable(LiquidSorter.this, table, content.liquids(),
                    () -> sortLiquid, this::configure, selectionRows, selectionColumns);
        }

        @Override
        public Liquid config() {
            return sortLiquid;
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.s(sortLiquid == null ? -1 : sortLiquid.id);
            write.i(rotation);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            sortLiquid = content.liquid(read.s());
            rotation = read.i();
        }
    }
}