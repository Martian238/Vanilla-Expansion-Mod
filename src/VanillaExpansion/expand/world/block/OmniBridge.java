package VanillaExpansion.expand.world.block;

import arc.func.Boolf;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.struct.Seq;
import arc.util.Eachable;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.core.Renderer;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.input.Placement;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.ItemBridge;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;


public class OmniBridge extends ItemBridge {
    public int range;
    public float speed = 30f;
    public float bridgeWidth = 8f;
    public float arrowTimeScl = 1.5f;
    public float arrowPeriod = 1.5f;


    public TextureRegion topRegion;
    protected static int currentFindX, currentFindY;
    protected static BuildPlan currentPlan;
    protected static final Boolf<BuildPlan> planFinder = other ->
            other.block == currentPlan.block && currentPlan != other && currentFindX == other.x && currentFindY == other.y;


    @Override
    public void load() {
        super.load();
        topRegion = arc.Core.atlas.find(name + "-top");

    }

    @Override
    public void init() {
        super.init();

        transportTime = 60f / speed;
    }




    public OmniBridge(String name) {
        super(name);
        hasPower = false;
        range = 6;
        itemCapacity = 10;
        buildType = OmniBridgeBuild::new;

    }


    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.range, range, StatUnit.blocks);
    }


    @Override
    public boolean positionsValid(int x1, int y1, int x2, int y2) {
        return Mathf.dst(x1, y1, x2, y2) <= range;
    }

    @Override
    public void changePlacementPath(Seq<Point2> points, int rotation) {
        Placement.calculateNodes(points, this, rotation,
                (point, other) -> Mathf.dst(point.x, point.y, other.x, other.y) <= range);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        Tile linkTile = findLink(x, y);

        Drawf.dashCircle(x * tilesize, y * tilesize, range * tilesize, Pal.placing);

        Draw.reset();
        Draw.color(Pal.placing);
        Lines.stroke(1f);
        if (linkTile != null && Math.abs(linkTile.x - x) + Math.abs(linkTile.y - y) > 1) {
            Lines.line(x * tilesize, y * tilesize, linkTile.x * tilesize, linkTile.y * tilesize);
            Draw.rect("bridge-arrow", (x * tilesize + linkTile.x * tilesize) / 2f, (y * tilesize + linkTile.y * tilesize) / 2f, Angles.angle(linkTile.x, linkTile.y, x, y));
        }
        Draw.reset();
    }
    @Override
    public void drawPlanConfigTop(BuildPlan plan, Eachable<BuildPlan> list){
        if(plan.config instanceof Point2 p && Mathf.dst(p.x, p.y) <= range){
            currentFindX = plan.x + p.x;
            currentFindY = plan.y + p.y;
            currentPlan = plan;
            var otherReq = findPlan(list, currentFindX, currentFindY, planFinder);
            if(otherReq != null){
                drawBridge(plan, otherReq.drawx(), otherReq.drawy(), 0);
            }
        }
    }
    public void drawBridge(BuildPlan req, float ox, float oy, float flip){
        if(Mathf.zero(Renderer.bridgeOpacity)) return;
        Draw.alpha(Renderer.bridgeOpacity);
        Lines.stroke(bridgeWidth);
        Tmp.v1.set(ox, oy).sub(req.drawx(), req.drawy()).setLength(tilesize/2f);
        Lines.line(bridgeRegion,
                req.drawx() + Tmp.v1.x,
                req.drawy() + Tmp.v1.y,
                ox - Tmp.v1.x,
                oy - Tmp.v1.y, false);
        Draw.rect(arrowRegion, (req.drawx() + ox) / 2f, (req.drawy() + oy) / 2f,
                Angles.angle(req.drawx(), req.drawy(), ox, oy) + flip);
        Draw.reset();
    }

    public class OmniBridgeBuild extends ItemBridgeBuild {
        protected float warmup = 0f;




        @Override
        public void playerPlaced(Object config) {

            if (config != null) {
                if (config instanceof Point2 p) {
                    configure(p);
                } else if (config instanceof Integer i) {
                    configure(i);
                }
            }


            if (link == -1) {
                if (OmniBridge.this.lastBuild != null && OmniBridge.this.lastBuild != this &&
                        OmniBridge.this.lastBuild.isValid() &&
                        OmniBridge.this.lastBuild.link == -1 &&
                        positionsValid(tile.x, tile.y, OmniBridge.this.lastBuild.tile.x, OmniBridge.this.lastBuild.tile.y)) {
                    OmniBridge.this.lastBuild.configure(pos());
                } else {

                    Tile nearest = null;
                    float nearestDist = Float.MAX_VALUE;
                    for (int dx = -range; dx <= range; dx++) {
                        for (int dy = -range; dy <= range; dy++) {
                            if (dx == 0 && dy == 0) continue;
                            if (Mathf.dst(dx, dy) > range) continue;
                            Tile other = world.tile(tile.x + dx, tile.y + dy);
                            if (other != null && other.build instanceof OmniBridgeBuild b && other.build.team == team) {
                                if (b.link != -1) continue;
                                float dst = Mathf.dst(dx, dy);
                                if (dst < nearestDist) {
                                    nearestDist = dst;
                                    nearest = other;
                                }
                            }
                        }
                    }
                    if (nearest != null) {
                        nearest.build.configure(pos());
                    }
                }
            }

            OmniBridge.this.lastBuild = this;
        }

        @Override
        public void onProximityAdded() {


            if (link != -1) {
                Tile existing = world.tile(link);
                if (existing != null && existing.build instanceof OmniBridgeBuild other && existing.build.team == team &&
                        positionsValid(tile.x, tile.y, existing.x, existing.y)) {

                    int pos = tile.pos();
                    if (!other.incoming.contains(pos)) {
                        other.incoming.add(pos);
                    }

                }

            }
        }

        @Override
        public void drawConfigure() {
            Drawf.dashCircle(x, y, range * tilesize, Pal.accent);
            Drawf.select(x, y, block.size * tilesize / 2f + 2f, Pal.accent);

            for (int dx = -range; dx <= range; dx++) {
                for (int dy = -range; dy <= range; dy++) {
                    if (dx == 0 && dy == 0) continue;
                    if (Mathf.dst(dx, dy) > range) continue;
                    Tile other = world.tile(tile.x + dx, tile.y + dy);
                    if (other != null && other.build instanceof OmniBridgeBuild otherBuild && other.build.team == team) {

                        boolean isOutput = (link == other.pos());
                        boolean isInput = (otherBuild.link == tile.pos());

                        if (isOutput) {
                            Drawf.select(other.drawx(), other.drawy(),
                                    other.block().size * tilesize / 2f + 2f, Pal.place);
                        } else if (!isInput) {
                            Drawf.select(other.drawx(), other.drawy(),
                                    other.block().size * tilesize / 2f + 2f + Mathf.absin(Time.time, 4f, 1f),
                                    Pal.breakInvalid);
                        }
                    }
                }
            }
        }

        @Override
        public boolean onConfigureBuildTapped(Building other) {
            if (other == this) return false;
            if (other instanceof OmniBridgeBuild && other.team == team && positionsValid(tile.x, tile.y, other.tileX(), other.tileY())) {
                if (link == other.pos()) {
                    configure(-1);
                } else {
                    if (((OmniBridgeBuild) other).link == pos()) {
                        other.configure(-1);
                    }
                    configure(other.pos());
                }
                return false;
            }
            return true;
        }

        @Override
        public Point2 config() {
            return link == -1 ? null : Point2.unpack(link).sub(tile.x, tile.y);
        }

        @Override
        public void configure(Object value) {
            if (value instanceof Point2 p) {
                Tile target = world.tile(tile.x + p.x, tile.y + p.y);
                link = target == null ? -1 : target.pos();
            } else if (value instanceof Integer) {
                link = (Integer) value;
            }
        }






        @Override
        public void checkIncoming(){
            int idx = 0;
            while(idx < incoming.size){
                int i = incoming.items[idx];
                Tile other = world.tile(i);
                if(other == null || !(other.build instanceof OmniBridgeBuild b)
                        || b.link != tile.pos() || !positionsValid(tile.x, tile.y, other.x, other.y)){
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

            Draw.z(Layer.power);

            Tile other = world.tile(link);
            if (!linkValid(tile, other)) return;
            if (Mathf.zero(Renderer.bridgeOpacity)) return;

            Draw.alpha(Math.max(warmup, 0.25f) * Renderer.bridgeOpacity);

            Lines.stroke(bridgeWidth);
            Lines.line(bridgeRegion, x, y, other.worldx(), other.worldy(), false);

            float dst = Mathf.dst(x, y, other.worldx(), other.worldy()) - tilesize / 4f;
            float ang = Angles.angle(x, y, other.worldx(), other.worldy());
            int seg = Mathf.round(dst / tilesize);


            for (int i = 0; i < seg; i++) {
                Tmp.v1.trns(ang, (dst / seg) * i + tilesize / 8f).add(this);
                Draw.alpha(Mathf.absin(i - time / arrowTimeScl, arrowPeriod, 1f) * warmup * Renderer.bridgeOpacity);
                Draw.rect(arrowRegion, Tmp.v1.x, Tmp.v1.y, ang);
            }
            Draw.color();
            Draw.reset();
        }

        @Override
        public void drawSelect(){
            if(linkValid(tile, world.tile(link))){
                drawInput(world.tile(link));
            }

            incoming.each(pos -> drawInput(world.tile(pos)));

            Draw.reset();
        }

        private void drawInput(Tile other){
            if(!linkValid(tile, other, false)) return;
            boolean linked = other.pos() == link;

            Tmp.v2.trns(tile.angleTo(other), 2f);
            float tx = tile.drawx(), ty = tile.drawy();
            float ox = other.drawx(), oy = other.drawy();
            float alpha = Math.abs((linked ? 100 : 0)-(Time.time * 2f) % 100f) / 100f;
            float x = Mathf.lerp(ox, tx, alpha);
            float y = Mathf.lerp(oy, ty, alpha);

            Tile otherLink = linked ? other : tile;
            int rel = (linked ? tile : other).absoluteRelativeTo(otherLink.x, otherLink.y);

            //draw "background"
            Draw.color(Pal.gray);
            Lines.stroke(2.5f);
            Lines.square(ox, oy, 2f, 45f);
            Lines.stroke(2.5f);
            Lines.line(tx + Tmp.v2.x, ty + Tmp.v2.y, ox - Tmp.v2.x, oy - Tmp.v2.y);

            float color = (linked ? Pal.place : Pal.accent).toFloatBits();

            //draw foreground colors
            Draw.color(color);
            Lines.stroke(1f);
            Lines.line(tx + Tmp.v2.x, ty + Tmp.v2.y, ox - Tmp.v2.x, oy - Tmp.v2.y);

            Lines.square(ox, oy, 2f, 45f);
            Draw.mixcol(color);
            Draw.color();
            float angle = linked ? Angles.angle(tx, ty, ox, oy) : Angles.angle(ox, oy, tx, ty);
            Draw.rect(arrowRegion, x, y, angle);
            Draw.mixcol();
        }

    }
}