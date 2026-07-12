package MultiCraft;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.struct.Seq;
import arc.util.Eachable;
import arc.util.Time;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.input.Placement;
import mindustry.world.Tile;
import mindustry.world.meta.BlockGroup;

import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;

public class HybridOmniBridge extends OmniBridge {
    //用于设置桥是全方向连接还是原版连接方式（因为作者懒得直接设计两种混装桥了，直接拿全方向桥改的）
    public boolean canOmni = false;

    public HybridOmniBridge(String name) {
        super(name);

        hasLiquids = true;
        outputsLiquid = true;
        liquidCapacity = 10f;
        group = BlockGroup.transportation;
        buildType = HybridOmniBridgeBuild::new;
    }


    @Override
    public boolean linkValid(Tile tile, Tile other, boolean checkDouble) {
        if (!super.linkValid(tile, other, checkDouble)) return false;

        if (tile.block() instanceof HybridOmniBridge a &&
                other.block() instanceof HybridOmniBridge b) {
            return a.canOmni == b.canOmni;
        }
        return true;
    }

    @Override
    public boolean positionsValid(int x1, int y1, int x2, int y2) {
        if(canOmni){
            return Mathf.dst(x1, y1, x2, y2) <= range;
        }else{
            if(x1 == x2){
                return Math.abs(y1 - y2) <= range;
            }else if(y1 == y2){
                return Math.abs(x1 - x2) <= range;
            }else{
                return false;
            }
        }
    }




    @Override
    public void changePlacementPath(Seq<Point2> points, int rotation) {
        if (canOmni) {
            Placement.calculateNodes(points, this, rotation,
                    (point, other) -> Mathf.dst(point.x, point.y, other.x, other.y) <= range);
        } else {
            Placement.calculateNodes(points, this, rotation,
                    (point, other) -> Math.max(Math.abs(point.x - other.x), Math.abs(point.y - other.y)) <= range);
        }
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        if (canOmni) {
            super.drawPlace(x, y, rotation, valid);
        } else {
            Tile link = findLink(x, y);
            for (int i = 0; i < 4; i++) {
                Drawf.dashLine(Pal.placing,
                        x * tilesize + Geometry.d4[i].x * (tilesize / 2f + 2),
                        y * tilesize + Geometry.d4[i].y * (tilesize / 2f + 2),
                        x * tilesize + Geometry.d4[i].x * range * tilesize,
                        y * tilesize + Geometry.d4[i].y * range * tilesize);
            }
            Draw.reset();
            Draw.color(Pal.placing);
            Lines.stroke(1f);
            if (link != null && Math.abs(link.x - x) + Math.abs(link.y - y) > 1) {
                int rot = link.absoluteRelativeTo(x, y);
                float w = (link.x == x ? tilesize : Math.abs(link.x - x) * tilesize - tilesize);
                float h = (link.y == y ? tilesize : Math.abs(link.y - y) * tilesize - tilesize);
                Lines.rect((x + link.x) / 2f * tilesize - w / 2f, (y + link.y) / 2f * tilesize - h / 2f, w, h);
                Draw.rect("bridge-arrow", link.x * tilesize + Geometry.d4(rot).x * tilesize,
                        link.y * tilesize + Geometry.d4(rot).y * tilesize, rot * 90);
            }
            Draw.reset();
        }
    }

    @Override
    public void drawPlanConfigTop(BuildPlan plan, Eachable<BuildPlan> list) {
        if (canOmni) {
            super.drawPlanConfigTop(plan, list);
        } else {
            if (plan.config instanceof Point2 p && (Math.abs(p.x) <= range && Math.abs(p.y) <= range)
                    && (p.x == 0 || p.y == 0)) {
                currentFindX = plan.x + p.x;
                currentFindY = plan.y + p.y;
                currentPlan = plan;
                var otherReq = findPlan(list, currentFindX, currentFindY, planFinder);
                if (otherReq != null) {
                    drawBridge(plan, otherReq.drawx(), otherReq.drawy(), 0);
                }
            }
        }
    }



    public class HybridOmniBridgeBuild extends OmniBridgeBuild {

        @Override
        public void updateTransport(Building other) {

            super.updateTransport(other);

            if (warmup >= 0.25f) {
                moved |= moveLiquid(other, liquids.current()) > 0.05f;
            }
        }


        @Override
        public void doDump() {
            super.doDump();
            dumpLiquid(liquids.current(), 1f);
        }


        @Override
        public void drawConfigure() {
            if (canOmni) {
                super.drawConfigure();
            } else {
                Drawf.select(x, y, block.size * tilesize / 2f + 2f, Pal.accent);
                for (int i = 1; i <= range; i++) {
                    for (int j = 0; j < 4; j++) {
                        Tile other = tile.nearby(Geometry.d4[j].x * i, Geometry.d4[j].y * i);
                        if (linkValid(tile, other)) {
                            boolean linked = other.pos() == link;
                            Drawf.select(other.drawx(), other.drawy(),
                                    other.block().size * tilesize / 2f + 2f + (linked ? 0f : Mathf.absin(Time.time, 4f, 1f)),
                                    linked ? Pal.place : Pal.breakInvalid);
                        }
                    }
                }
            }
        }

        @Override
        public void playerPlaced(Object config) {

            if (config != null) {
                if (config instanceof Point2 p) configure(p);
                else if (config instanceof Integer i) configure(i);
            }


            if (link == -1) {
                Tile nearest = null;
                float nearestDist = Float.MAX_VALUE;
                for (int dx = -range; dx <= range; dx++) {
                    for (int dy = -range; dy <= range; dy++) {
                        if (dx == 0 && dy == 0) continue;
                        // 四方向模式
                        if (!canOmni && (dx != 0 && dy != 0)) continue;
                        Tile other = world.tile(tile.x + dx, tile.y + dy);
                        if (other == null || other.build == null) continue;

                        if (other.build instanceof OmniBridgeBuild ob &&
                                linkValid(tile, other) && ob.link == -1) {
                            float dst = Mathf.dst(dx, dy);
                            if (dst < nearestDist) {
                                nearestDist = dst;
                                nearest = other;
                            }
                        }
                    }
                }
                if (nearest != null) {
                    nearest.build.configure(tile.pos());
                }
            }


            HybridOmniBridge.this.lastBuild = this;
        }


        @Override
        public void onProximityAdded() {
            if (link != -1) {
                Tile existing = world.tile(link);
                if (existing != null && existing.build instanceof OmniBridgeBuild other &&
                        existing.build.team == team && linkValid(tile, existing)) {
                    int pos = tile.pos();
                    if (!other.incoming.contains(pos)) {
                        other.incoming.add(pos);
                    }
                }
            }
        }

        @Override
        public void updateTile() {
            super.updateTile();
        }
    }
}