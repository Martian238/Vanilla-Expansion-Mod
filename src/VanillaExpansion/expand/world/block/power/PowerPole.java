package VanillaExpansion.expand.world.block.power;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.Vars;
import mindustry.core.Renderer;
import mindustry.entities.units.*;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.Placement;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.meta.*;
import mindustry.world.modules.PowerModule;

import static mindustry.Vars.*;

public class PowerPole extends PowerBlock {
    public static final int DIRECTIONS = 16;
    public static final float DEG_PER_DIR = 360f / DIRECTIONS;

    private static final float[] ANGLE_MAP = new float[16];
    static {
        for (int i = 0; i < 16; i++) ANGLE_MAP[i] = i * DEG_PER_DIR;
    }

    public static float angleFrom(int fullRot) {
        return ANGLE_MAP[Mathf.mod(fullRot, 16)];
    }

    public static int angleToFullRotation(float angleDeg) {
        return Mathf.round(angleDeg / DEG_PER_DIR) % DIRECTIONS;
    }

    public static boolean isCardinal(int fullRot) {
        return fullRot % 4 == 0;
    }

    public int lineCount = 4;
    public float lineSpacing = 8f;
    //线轴大小
    public float lineRadius = 2f;
    //实际激光绘制线轴大小
    public int laserRadius = (int)lineRadius -1;
    public float laserRange = 20f;
    public float areaRange = 10f;
    public int maxNodes = 12;

    public TextureRegion laser, laserEnd;
    public float laserScale = 0.25f;
    public boolean useLod = true;
    public float powerLayer = Layer.power;
    public Color laserColor1 = Color.white;
    public Color laserColor2 = Pal.powerLight;
    public boolean drawRange = true;

    public PowerPole(String name) {
        super(name);
        configurable = true;
        ignoreResizeConfig = true;
        consumesPower = false;
        outputsPower = false;
        canOverdrive = false;
        swapDiagonalPlacement = true;
        schematicPriority = -10;
        drawDisabled = false;
        envEnabled |= Env.space;
        destructible = true;
        delayLandingConfig = true;
        update = true;
        rotate = false;
        quickRotate = false;
        drawCached = false;

        config(Integer.class, (Building entity, Integer value) -> {
            PowerPoleBuild pole = (PowerPoleBuild) entity;
            PowerModule power = pole.power;
            Building other = world.build(value);
            boolean contains = power.links.contains(value);
            boolean valid = other != null && other.power != null;

            if (contains) {
                power.links.removeValue(value);
                if (valid) other.power.links.removeValue(entity.pos());
                PowerGraph newGraph = new PowerGraph();
                newGraph.reflow(entity);
                newGraph.update();
                if (valid && other.power.graph != newGraph) {
                    PowerGraph og = new PowerGraph();
                    og.reflow(other);
                    og.update();
                }
                pole.autoOrient();
            } else if (linkValid(entity, other) && valid && pole.wireLinkCount() < maxNodes) {
                power.links.addUnique(other.pos());
                if (other.team == entity.team) {
                    other.power.links.addUnique(entity.pos());
                }
                power.graph.addGraph(other.power.graph);
                pole.autoOrient();
            }
        });

        config(Point2[].class, (Building entity, Point2[] value) -> {
            IntSeq old = new IntSeq(entity.power.links);
            for (int i = 0; i < old.size; i++) {
                configurations.get(Integer.class).get(entity, old.get(i));
            }
            for (Point2 p : value) {
                configurations.get(Integer.class).get(entity, Point2.pack(p.x + entity.tileX(), p.y + entity.tileY()));
            }
            if (entity instanceof PowerPoleBuild pole) {
                pole.autoOrient();
            }
        });

        buildType = () -> new PowerPoleBuild();
    }

    @Override
    public void init() {
        super.init();
        clipSize = Math.max(clipSize, laserRange * tilesize);
    }

    @Override
    public void load() {
        super.load();
        if (Vars.headless) return;
        TextureRegion fallbackLaser = Core.atlas.find("laser");
        TextureRegion fallbackEnd = Core.atlas.find("laser-end");
        laser = Core.atlas.find(name + "-laser", fallbackLaser);
        laserEnd = Core.atlas.find(name + "-laser-end", fallbackEnd);
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.powerRange, laserRange, StatUnit.blocks);
        stats.add(Stat.powerConnections, maxNodes, StatUnit.none);
    }

    @Override
    public void setBars() {
        super.setBars();
        addBar("power", PowerNode.makePowerBalance());
        addBar("batteries", PowerNode.makeBatteryBalance());
        addBar("connections", entity -> new Bar(() ->
            Core.bundle.format("bar.powerlines", ((PowerPoleBuild)entity).wireLinkCount(), maxNodes),
            () -> Pal.items,
            () -> (float)((PowerPoleBuild)entity).wireLinkCount() / (float)maxNodes
        ));
    }

    protected void setupColor(float satisfaction) {
        Draw.color(Tmp.c1.set(laserColor1)
            .lerp(laserColor2, (1f - satisfaction) * 0.86f + Mathf.absin(3f, 0.1f))
            .a(Renderer.laserOpacity * (useLod ? Lod.alpha2 : 1f)));
    }

    public void drawLaser(float x1, float y1, float x2, float y2, int size1, int size2) {
        drawLaser(x1, y1, x2, y2, size1, size2, true);
    }

    public void drawLaser(float x1, float y1, float x2, float y2, int size1, int size2, boolean light) {
        float angle1 = Angles.angle(x1, y1, x2, y2),
            vx = Mathf.cosDeg(angle1), vy = Mathf.sinDeg(angle1),
            len1 = size1 * tilesize / 2f - 1.5f, len2 = size2 * tilesize / 2f - 1.5f;
        Drawf.laser(laser, laserEnd, laserEnd, x1 + vx * len1, y1 + vy * len1, x2 - vx * len2, y2 - vy * len2, laserScale, light, useLod);
    }

    public boolean overlaps(Building src, Building other, float range) {
        return Intersector.overlaps(Tmp.cr1.set(src.x, src.y, range), other.tile.getHitbox(Tmp.r1));
    }

    public boolean overlaps(@Nullable Tile src, @Nullable Tile other) {
        if (src == null || other == null) return true;
        return Intersector.overlaps(
            Tmp.cr1.set(src.worldx() + offset, src.worldy() + offset, laserRange * tilesize),
            Tmp.r1.setSize(size * tilesize).setCenter(other.worldx() + offset, other.worldy() + offset));
    }

    public boolean linkValid(Building tile, Building link) {
        if (tile == link || link == null || !link.block.hasPower || !link.block.connectedPower || tile.team != link.team)
            return false;
        if (!(tile instanceof PowerPoleBuild) || !(link instanceof PowerPoleBuild))
            return false;

        if (!overlaps(tile, link, laserRange * tilesize)) return false;
        if (PowerNode.insulated(tile.tile, link.tile)) return false;
        return true;
    }

    @Override
    public void changePlacementPath(Seq<Point2> points, int rotation) {
        Placement.calculateNodes(points, this, rotation, (point, other) -> overlaps(world.tile(point.x, point.y), world.tile(other.x, other.y)));
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        Lines.stroke(1f);
        Draw.color(Pal.placing);
        Drawf.circles(x * tilesize + offset, y * tilesize + offset, laserRange * tilesize);

        Draw.color(Pal.accent);
        Drawf.dashSquare(Pal.accent, x * tilesize + offset, y * tilesize + offset, areaRange * tilesize * 2);

        float wx = x * tilesize + offset;
        float wy = y * tilesize + offset;

        int rad = (int) Math.ceil(laserRange);
        for (int dx = -rad; dx <= rad; dx++) {
            for (int dy = -rad; dy <= rad; dy++) {
                Tile t = world.tile(x + dx, y + dy);
                if (t == null || t.build == null || !(t.build instanceof PowerPoleBuild)) continue;
                PowerPoleBuild pole = (PowerPoleBuild) t.build;
                float dist = Mathf.dst(wx, wy, pole.x, pole.y);
                if (dist <= laserRange * tilesize) {
                    Draw.color(laserColor1, Renderer.laserOpacity * 0.5f);
                    drawLaser(wx, wy, pole.x, pole.y, size, pole.block.size);
                    Drawf.square(pole.x, pole.y, pole.block.size * tilesize / 2f + 2f, Pal.place);
                }
            }
        }

        Draw.reset();
    }

    @Override
    public void flipRotation(BuildPlan req, boolean x) {
        if (req.config instanceof Integer full) {
            int flipped = x ? (16 - full) % 16 : (24 - full) % 16;
            req.config = flipped;
            req.rotation = flipped / 4;
        }
        if (req.config instanceof Point2[] points) {
            for (Point2 p : points) {
                if (x) p.x = -p.x;
                else p.y = -p.y;
            }
        }
    }

    public class PowerPoleBuild extends Building {
        protected int fullRotation = 0;
        public int updateTimer = 0;

        public int wireLinkCount() {
            int count = 0;
            for (int i = 0; i < power.links.size; i++) {
                Building other = world.build(power.links.get(i));
                if (other instanceof PowerPoleBuild) count++;
            }
            return count;
        }

        public float getLineX(int lineId) {
            float half = (lineCount - 1) * lineSpacing / 2f;
            float offset = lineId * lineSpacing - half;
            int corrected = (fullRotation - 4 + 16) % 16;
            float angleRad = corrected * DEG_PER_DIR * Mathf.degRad;
            return x + offset * Mathf.cos(angleRad);
        }

        public float getLineY(int lineId) {
            float half = (lineCount - 1) * lineSpacing / 2f;
            float offset = lineId * lineSpacing - half;
            int corrected = (fullRotation - 4 + 16) % 16;
            float angleRad = corrected * DEG_PER_DIR * Mathf.degRad;
            return y + offset * Mathf.sin(angleRad);
        }

        public void autoOrient() {
            if (power.links.size == 0) {
                fullRotation = 0;
                rotation = 0;
                return;
            }

            int best = 0;
            float bestScore = Float.NEGATIVE_INFINITY;

            for (int candidate = 0; candidate < 16; candidate++) {
                float score = 0;
                int corr = (candidate - 4 + 16) % 16;
                float sRad = corr * DEG_PER_DIR * Mathf.degRad;
                float c = Mathf.cos(sRad), s = Mathf.sin(sRad);

                for (int i = 0; i < power.links.size; i++) {
                    Building other = world.build(power.links.get(i));
                    if (!(other instanceof PowerPoleBuild)) continue;
                    PowerPoleBuild pole = (PowerPoleBuild) other;

                    for (int li = 0; li < lineCount; li++) {
                        float half = (lineCount - 1) * lineSpacing / 2f;
                        float off = li * lineSpacing - half;
                        float ax = x + off * c, ay = y + off * s;
                        float bx = pole.getLineX(li), by = pole.getLineY(li);
                        score += Mathf.dst(ax, ay, bx, by);

                        for (int lj = 0; lj < lineCount; lj++) {
                            if (li == lj) continue;
                            off = lj * lineSpacing - half;
                            float ajx = x + off * c, ajy = y + off * s;
                            float bjx = pole.getLineX(lj), bjy = pole.getLineY(lj);
                            score += Mathf.dst(ax, ay, bjx, bjy);
                            score += Mathf.dst(ajx, ajy, bx, by);
                        }
                    }
                }

                if (score > bestScore) {
                    bestScore = score;
                    best = candidate;
                }
            }

            fullRotation = best;
            rotation = fullRotation / 4;
        }

        private boolean areaLinkValid(Building other) {
            if (other == null || other == this) return false;
            if (!other.block.hasPower || !other.block.connectedPower) return false;
            if (other.team != team) return false;
            if (other instanceof PowerPoleBuild) return false;
            if (PowerNode.insulated(this.tile, other.tile)) return false;
            int dx = Math.abs(tileX() - other.tileX());
            int dy = Math.abs(tileY() - other.tileY());
            return dx <= areaRange && dy <= areaRange;
        }

        private void autoConnectArea() {
            IntSeq newAreaLinks = new IntSeq();
            int tx = tileX(), ty = tileY();
            int rad = (int) Math.ceil(areaRange);

            for (int dx = -rad; dx <= rad; dx++) {
                for (int dy = -rad; dy <= rad; dy++) {
                    Tile t = world.tile(tx + dx, ty + dy);
                    if (t == null || t.build == null || t.build == this) continue;
                    if (areaLinkValid(t.build)) {
                        newAreaLinks.add(t.build.pos());
                    }
                }
            }

            boolean changed = false;

            for (int i = 0; i < power.links.size; i++) {
                int pos = power.links.get(i);
                Building other = world.build(pos);
                if (other != null && !(other instanceof PowerPoleBuild)) {
                    if (!newAreaLinks.contains(pos)) {
                        other.power.links.removeValue(this.pos());
                        power.links.removeIndex(i);
                        i--;
                        changed = true;
                    }
                }
            }

            for (int i = 0; i < newAreaLinks.size; i++) {
                int pos = newAreaLinks.get(i);
                if (!power.links.contains(pos)) {
                    Building other = world.build(pos);
                    if (other != null && other.power != null) {
                        power.links.add(pos);
                        if (!other.power.links.contains(this.pos())) {
                            other.power.links.add(this.pos());
                        }
                        changed = true;
                    }
                }
            }

            if (changed) {
                updatePowerGraph();
            }
        }

        private void autoConnectWires() {
            Building nearest = null;
            float nearestDist = Float.MAX_VALUE;
            int tx = tileX(), ty = tileY();
            int rad = (int) Math.ceil(laserRange);

            for (int dx = -rad; dx <= rad; dx++) {
                for (int dy = -rad; dy <= rad; dy++) {
                    Tile t = world.tile(tx + dx, ty + dy);
                    if (t == null || t.build == null || t.build == this) continue;
                    Building other = t.build;
                    if (other instanceof PowerPoleBuild && other != this) {
                        if (linkValid(this, other)) {
                            if (wireLinkCount() < maxNodes && ((PowerPoleBuild)other).wireLinkCount() < maxNodes) {
                                if (!power.links.contains(other.pos())) {
                                    float dist = Mathf.dst2(x, y, other.x, other.y);
                                    if (dist < nearestDist) {
                                        nearestDist = dist;
                                        nearest = other;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (nearest != null) {
                configure(nearest.pos());
            }
        }

        @Override
        public void placed() {
            super.placed();
            if (!net.client()) {
                if (power.links.size == 0) {
                    autoConnectWires();
                }
                autoOrient();
                autoConnectArea();
            }
        }

        @Override
        public void onProximityAdded() {
            super.onProximityAdded();
            autoConnectArea();
            autoOrient();
        }

        @Override
        public void onProximityRemoved() {
            super.onProximityRemoved();
            for (int i = 0; i < power.links.size; i++) {
                int pos = power.links.get(i);
                Building other = world.build(pos);
                if (other != null && other.power != null && !(other instanceof PowerPoleBuild)) {
                    other.power.links.removeValue(pos());
                }
            }
            for (int i = 0; i < power.links.size; i++) {
                int pos = power.links.get(i);
                Building other = world.build(pos);
                if (other != null && !(other instanceof PowerPoleBuild)) {
                    power.links.removeIndex(i);
                    i--;
                }
            }
        }

        @Override
        public void updateTile() {
            super.updateTile();
            if (++updateTimer >= 20) {
                updateTimer = 0;
                autoOrient();
                autoConnectArea();
            }
        }

        @Override
        public boolean onConfigureBuildTapped(Building other) {
            if (this == other) {
                if (power.links.size == 0) {
                    autoConnectWires();
                } else {
                    for (int i = 0; i < power.links.size; i++) {
                        int pos = power.links.get(i);
                        Building link = world.build(pos);
                        if (link instanceof PowerPoleBuild) {
                            if (link.power != null) {
                                link.power.links.removeValue(this.pos());
                            }
                            power.links.removeIndex(i);
                            i--;
                        }
                    }
                    updatePowerGraph();
                    autoOrient();
                }
                deselect();
                return false;
            }

            if (other == null) return false;

            if (linkValid(this, other)) {
                configure(other.pos());
                return false;
            }

            return false;
        }

        @Override
        public void draw() {
            Draw.rect(block.region, x, y, angleFrom(fullRotation));

            if (Mathf.zero(Renderer.laserOpacity) || team == Team.derelict) return;

            Draw.z(powerLayer);
            setupColor(power.graph.getSatisfaction());

            for (int i = 0; i < power.links.size; i++) {
                int pos = power.links.get(i);
                Building other = world.build(pos);
                if (other == null || other == this) continue;
                if (!(other instanceof PowerPoleBuild)) continue;
                if (!linkValid(this, other)) continue;
                if (id >= other.id) continue;

                PowerPoleBuild pole = (PowerPoleBuild) other;
                for (int lineId = 0; lineId < lineCount; lineId++) {
                    float startX = getLineX(lineId);
                    float startY = getLineY(lineId);
                    float endX = pole.getLineX(lineId);
                    float endY = pole.getLineY(lineId);
                    drawLaser(startX, startY, endX, endY, laserRadius, laserRadius);
                }
            }
            Draw.reset();
        }

        @Override
        public void drawSelect() {
            super.drawSelect();
            Draw.color(Pal.accent, 0.35f);
            float halfSize = areaRange * tilesize;
            Drawf.dashSquare(Pal.accent, x, y, halfSize * 2);
            Draw.color();

            for (int i = 0; i < lineCount; i++) {
                float lx = getLineX(i);
                float ly = getLineY(i);
                Draw.color(Pal.accent);
                Drawf.circles(lx, ly, lineRadius - 1f);
                Draw.color(Color.white);
                Fonts.outline.getData().setScale(1f / 6f);
                Fonts.outline.draw(String.valueOf(i + 1), lx, ly);
                Fonts.outline.getData().setScale(1f);
            }
            Draw.reset();
        }

        @Override
        public void drawConfigure() {
            super.drawConfigure();

            for (int i = 0; i < power.links.size; i++) {
                Building link = world.build(power.links.get(i));
                if (link == null) continue;
                float s = link.block.size * tilesize * 1.1f + Mathf.absin(Time.time, 5f, 2);
                if (link instanceof PowerPoleBuild) {
                    Drawf.dashSquare(Pal.place, link.x, link.y, s);
                } else {
                    Drawf.square(link.x, link.y, s, Pal.place);
                }
            }

            Drawf.circles(x, y, laserRange * tilesize);
            Draw.color(Pal.place);
            float halfSize = areaRange * tilesize;
            Drawf.dashSquare(Pal.place, x, y, halfSize * 2);
            Draw.color();

            for (int i = 0; i < lineCount; i++) {
                float lx = getLineX(i);
                float ly = getLineY(i);
                Draw.color(Pal.place);
                Drawf.circles(lx, ly, lineRadius);
                Draw.color(Color.white);
                Fonts.outline.getData().setScale(1f / 6f);
                Fonts.outline.draw(String.valueOf(i + 1), lx, ly);
                Fonts.outline.getData().setScale(1f);
            }
            Draw.reset();
        }

        @Override
        public Point2[] config() {
            Point2[] out = new Point2[power.links.size];
            for (int i = 0; i < out.length; i++) {
                out[i] = Point2.unpack(power.links.get(i)).sub(tile.x, tile.y);
            }
            return out;
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.s(fullRotation);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            fullRotation = read.s();
        }
    }
}
