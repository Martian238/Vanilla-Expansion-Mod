package VanillaExpansion.expand.world.block.liquid;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.core.Renderer;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.input.Placement;
import mindustry.type.Liquid;
import mindustry.world.Tile;
import mindustry.world.blocks.liquid.LiquidBridge;
import mindustry.world.blocks.ItemSelection;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import VanillaExpansion.expand.util.MathUtil;

import static mindustry.Vars.*;

public class AdaptLiquidBridge extends LiquidBridge {
    public static final int maxLinks = 3;
    public TextureRegion topRegion;
    public boolean invert;

    public AdaptLiquidBridge(String name) {
        super(name);

        range = 6;
        drawTeamOverlay = false;
        allowDiagonal = true;
        configurable = true;
        saveConfig = true;
        clearOnDoubleTap = true;

        config(Liquid.class, (AdaptLiquidBridgeBuild tile, Liquid liquid) -> tile.sortLiquid = liquid);
        configClear((AdaptLiquidBridgeBuild tile) -> tile.sortLiquid = null);
    }

    @Override
    public void load() {
        super.load();
        topRegion = Core.atlas.find(name + "-top");
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.range, range, StatUnit.blocks);
    }

    @Override
    public void changePlacementPath(Seq<Point2> points, int rotation) {
        Placement.calculateNodes(points, this, rotation, (point, other) -> MathUtil.dst(point, other) <= range);
    }

    @Override
    public boolean positionsValid(int x1, int y1, int x2, int y2) {
        return Mathf.dst(x1, y1, x2, y2) <= range;
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        Tile link = findLink(x, y);

        Drawf.dashCircle(x * tilesize, y * tilesize, range * tilesize, Pal.placing);

        Draw.reset();
        Draw.color(Pal.placing);
        Lines.stroke(1f);
        if (link != null && Math.abs(link.x - x) + Math.abs(link.y - y) > 1) {
            Lines.line(x * tilesize, y * tilesize, link.x * tilesize, link.y * tilesize);
            Draw.rect("bridge-arrow", (x * tilesize + link.x * tilesize) / 2f, (y * tilesize + link.y * tilesize) / 2f, Angles.angle(link.x, link.y, x, y));
        }
        Draw.reset();
    }

    public class AdaptLiquidBridgeBuild extends LiquidBridgeBuild {
        public @Nullable Liquid sortLiquid;

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            if (sortLiquid == null) return enabled;
            return ((liquid == sortLiquid) != invert) == enabled;
        }

        @Override
        public void drawSelect() {
            super.drawSelect();
            if (sortLiquid != null) {
                Drawf.selected(tile, sortLiquid.color);
            }
        }

        @Override
        public void buildConfiguration(Table table) {
            ItemSelection.buildTable(AdaptLiquidBridge.this, table, content.liquids(), () -> sortLiquid, this::configure, selectionRows, selectionColumns);
        }

        @Override
        public byte version() {
            return 1;
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.s(sortLiquid == null ? -1 : sortLiquid.id);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            sortLiquid = content.liquid(read.s());
        }

        @Override
        public void drawConfigure() {
            Drawf.dashCircle(x, y, range * tilesize, Pal.placing);
            Drawf.select(x, y, tile.block().size * tilesize / 2f + 2f, Pal.accent);
        }

        @Override
        public void checkIncoming() {
            super.checkIncoming();
            int idx = 0;
            while (idx < incoming.size) {
                int i = incoming.items[idx];
                Tile other = world.tile(i);
                if (idx > maxLinks - 1) {
                    other.build.configure(-1);
                    incoming.removeIndex(idx);
                    idx--;
                }
                idx++;
            }
        }

        @Override
        public void draw() {
            Draw.rect(region, x, y);
            Draw.z(Layer.power + 0.1f);
            Draw.rect(topRegion, x, y);

            if (sortLiquid != null) {
                Draw.color(sortLiquid.color);
                Fill.square(x, y, tilesize / 2f - 0.00001f);
                Draw.color();
            }

            Draw.z(Layer.power);

            Tile other = world.tile(link);
            if (other == null || !linkValid(tile, other)) return;
            if (Mathf.zero(Renderer.bridgeOpacity)) return;

            Lines.stroke(bridgeWidth);
            Lines.line(bridgeRegion, x, y, other.worldx(), other.worldy(), false);

            float dst = Mathf.dst(x, y, other.worldx(), other.worldy()) - tilesize / 4f;
            float ang = Angles.angle(x, y, other.worldx(), other.worldy());
            int seg = Mathf.round(dst / tilesize);

            if (seg == 0) return;
            for (int i = 0; i < seg; i++) {
                Tmp.v1.trns(ang, (dst / seg) * i + tilesize / 8f).add(this);
                Draw.alpha(Mathf.absin(i - time / arrowTimeScl, arrowPeriod, 1f) * warmup * Renderer.bridgeOpacity);
                Draw.rect(arrowRegion, Tmp.v1.x, Tmp.v1.y, ang);
            }
            Draw.color();
            Draw.reset();
        }
    }
}