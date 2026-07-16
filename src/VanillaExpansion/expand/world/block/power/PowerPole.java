package VanillaExpansion.expand.world.block.power;

import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.Point2;
import arc.struct.*;
import arc.util.*;
import mindustry.core.Renderer;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.power.PowerNode;

import static mindustry.Vars.*;

public class PowerPole extends PowerNode {
    public int lineCount = 4;           // 线轴数量
    public float lineSpacing = 8f;      // 线轴间距（像素）
    public float lineRadius = 8f;       // 线轴连接检测半径（像素）

    // 第二重连接（区域连接）
    public float areaRange = 10f;        // 区域连接范围（格），正方形边长的一半

    public PowerPole(String name) {
        super(name);
        rotate = true;
        rotateDraw = true;
        update = true;
    }

    @Override
    public void init() {
        super.init();
        autolink = false;
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        Lines.stroke(1f);
        Draw.color(Pal.placing);
        Drawf.circles(x * tilesize + offset, y * tilesize + offset, laserRange * tilesize);

        Draw.color(Pal.accent);
        Drawf.square(x * tilesize + offset, y * tilesize + offset, areaRange * tilesize, 0f);

        Tile tile = world.tile(x, y);
        if (tile == null) return;

        getPotentialLinks(tile, player.team(), other -> {
            Draw.color(laserColor1, Renderer.laserOpacity * 0.5f);
            drawLaser(x * tilesize + offset, y * tilesize + offset, other.x, other.y, size, other.block.size);
            Drawf.square(other.x, other.y, other.block.size * tilesize / 2f + 2f, Pal.place);
        });

        Draw.reset();
    }

    protected float getLineX(Building building, int lineId, int rotation) {
        float half = (lineCount - 1) * lineSpacing / 2f;
        float offset = lineId * lineSpacing - half;

        int correctedRotation = (rotation - 1 + 4) % 4;
        float angleRad = correctedRotation * 90f * Mathf.degRad;
        float cos = Mathf.cos(angleRad);

        float xOffset = offset * cos;
        return building.x + xOffset;
    }

    protected float getLineY(Building building, int lineId, int rotation) {
        float half = (lineCount - 1) * lineSpacing / 2f;
        float offset = lineId * lineSpacing - half;

        int correctedRotation = (rotation - 1 + 4) % 4;
        float angleRad = correctedRotation * 90f * Mathf.degRad;
        float sin = Mathf.sin(angleRad);

        float yOffset = offset * sin;
        return building.y + yOffset;
    }

    // 线轴连接验证 - 只允许电线杆之间
    @Override
    public boolean linkValid(Building tile, Building link, boolean checkMaxNodes) {
        if (tile == link || link == null || !link.block.hasPower || !link.block.connectedPower || tile.team != link.team) return false;

        // 只允许电线杆之间的连接
        if (!(link instanceof PowerPoleBuild) || !(tile instanceof PowerPoleBuild)) {
            return false;
        }

        PowerPoleBuild thisPole = (PowerPoleBuild) tile;
        PowerPoleBuild pole = (PowerPoleBuild) link;

        float thisX = thisPole.getLineX(0);
        float thisY = thisPole.getLineY(0);
        float otherX = pole.getLineX(0);
        float otherY = pole.getLineY(0);
        float dist = Mathf.dst(thisX, thisY, otherX, otherY);

        if (dist <= laserRange * tilesize) {
            if (checkMaxNodes && link.block instanceof PowerNode node) {
                return link.power.links.size < node.maxNodes || link.power.links.contains(tile.pos());
            }
            return true;
        }
        return false;
    }

    public class PowerPoleBuild extends PowerNodeBuild {
        public int cachedLineCount;
        public float updateTimer = 0;
        public static final float UPDATE_INTERVAL = 20f;

        @Override
        public void created() {
            super.created();
            cachedLineCount = lineCount;
        }

        @Override
        public void placed() {
            super.placed();
            if (!net.client()) {
                autoConnectLine();
                scanAndConnectArea();
            }
        }

        @Override
        public void onProximityAdded() {
            super.onProximityAdded();
            scanAndConnectArea();
        }

        @Override
        public void onProximityRemoved() {
            super.onProximityRemoved();
            // 断开所有二重连接
            for (int i = 0; i < power.links.size; i++) {
                int pos = power.links.get(i);
                Building other = world.build(pos);
                if (other != null && other.power != null && !(other instanceof PowerPoleBuild)) {
                    other.power.links.removeValue(pos());
                }
            }
            // 只保留线轴连接（电线杆之间的连接）
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
            updateTimer += Time.delta;
            if (updateTimer >= UPDATE_INTERVAL) {
                updateTimer = 0;
                scanAndConnectArea();
            }
        }

        // 二重连接验证 - 使用格子坐标，与绘制保持一致
        private boolean areaLinkValid(Building other) {
            if (other == null || other == this) return false;
            if (!other.block.hasPower || !other.block.connectedPower) return false;
            if (other.team != team) return false;
            // 排除电线杆（电线杆之间通过线轴连接）
            if (other instanceof PowerPoleBuild) return false;
            if (PowerNode.insulated(this, other)) return false;

            // 使用格子坐标判断，与扫描范围完全一致
            int dx = Math.abs(tileX() - other.tileX());
            int dy = Math.abs(tileY() - other.tileY());
            return dx <= areaRange && dy <= areaRange;
        }

        // 扫描并连接二重范围内的普通建筑（直接添加到 power.links）
        private void scanAndConnectArea() {
            IntSeq newAreaLinks = new IntSeq();
            int tx = tileX(), ty = tileY();
            int rad = (int)Math.ceil(areaRange);

            for (int dx = -rad; dx <= rad; dx++) {
                for (int dy = -rad; dy <= rad; dy++) {
                    Tile t = world.tile(tx + dx, ty + dy);
                    if (t == null || t.build == null || t.build == this) continue;
                    Building other = t.build;
                    if (areaLinkValid(other)) {
                        newAreaLinks.add(other.pos());
                    }
                }
            }

            boolean changed = false;

            // 移除不再在范围内的连接
            for (int i = 0; i < power.links.size; i++) {
                int pos = power.links.get(i);
                Building other = world.build(pos);
                // 只处理普通建筑（非电线杆）
                if (other != null && !(other instanceof PowerPoleBuild)) {
                    if (!newAreaLinks.contains(pos)) {
                        other.power.links.removeValue(this.pos());
                        power.links.removeIndex(i);
                        i--;
                        changed = true;
                    }
                }
            }

            // 添加新的连接
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
                // 使用父类的电力图更新
                updatePowerGraph();
            }
        }

        // 线轴自动连接（只连接电线杆）
        private void autoConnectLine() {
            Building nearest = null;
            float nearestDist = Float.MAX_VALUE;

            int tx = tileX(), ty = tileY();
            int rad = (int)Math.ceil(laserRange);

            for (int dx = -rad; dx <= rad; dx++) {
                for (int dy = -rad; dy <= rad; dy++) {
                    Tile t = world.tile(tx + dx, ty + dy);
                    if (t == null || t.build == null || t.build == this) continue;

                    Building other = t.build;
                    if (other instanceof PowerPoleBuild pole && other != this) {
                        float thisX = getLineX(0);
                        float thisY = getLineY(0);
                        float otherX = pole.getLineX(0);
                        float otherY = pole.getLineY(0);
                        float dist = Mathf.dst(thisX, thisY, otherX, otherY);

                        if (dist <= laserRange * tilesize && !PowerNode.insulated(this, other)) {
                            if (power.links.size < maxNodes && other.power.links.size < maxNodes) {
                                if (!power.links.contains(other.pos())) {
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

        public float getLineX(int lineId) {
            float half = (cachedLineCount - 1) * lineSpacing / 2f;
            float offset = lineId * lineSpacing - half;

            int correctedRotation = (rotation - 1 + 4) % 4;
            float angleRad = correctedRotation * 90f * Mathf.degRad;
            float cos = Mathf.cos(angleRad);

            float xOffset = offset * cos;
            return x + xOffset;
        }

        public float getLineY(int lineId) {
            float half = (cachedLineCount - 1) * lineSpacing / 2f;
            float offset = lineId * lineSpacing - half;

            int correctedRotation = (rotation - 1 + 4) % 4;
            float angleRad = correctedRotation * 90f * Mathf.degRad;
            float sin = Mathf.sin(angleRad);

            float yOffset = offset * sin;
            return y + yOffset;
        }

        private int getNearestLine(float worldX, float worldY) {
            float bestDist = Float.MAX_VALUE;
            int bestIndex = -1;
            for (int i = 0; i < cachedLineCount; i++) {
                float lx = getLineX(i);
                float ly = getLineY(i);
                float dist = Mathf.dst(lx, ly, worldX, worldY);
                if (dist < bestDist && dist < lineRadius) {
                    bestDist = dist;
                    bestIndex = i;
                }
            }
            return bestIndex;
        }

        @Override
        public boolean onConfigureBuildTapped(Building other) {
            // 双击清空线轴连接（不清空二重连接）
            if (this == other) {
                if (power.links.size == 0) {
                    autoConnectLine();
                } else {
                    // 清空线轴连接，保留二重连接
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
                }
                deselect();
                return false;
            }

            if (other == null) return false;

            // 线轴手动连接：只允许连接电线杆
            if (linkValid(this, other)) {
                configure(other.pos());
                return false;
            }

            return false;
        }

        @Override
        public void draw() {
            Draw.rect(block.region, x, y);

            if (Mathf.zero(Renderer.laserOpacity) || team == Team.derelict) return;

            Draw.z(powerLayer);
            setupColor(power.graph.getSatisfaction());

            // 只绘制线轴连接（电线杆之间）
            for (int i = 0; i < power.links.size; i++) {
                int pos = power.links.get(i);
                Building other = world.build(pos);
                if (other == null || other == this) continue;

                if (!(other instanceof PowerPoleBuild)) continue;
                if (!linkValid(this, other)) continue;

                PowerPoleBuild pole = (PowerPoleBuild) other;
                for (int lineId = 0; lineId < cachedLineCount; lineId++) {
                    float startX = getLineX(lineId);
                    float startY = getLineY(lineId);
                    float endX = pole.getLineX(lineId);
                    float endY = pole.getLineY(lineId);
                    drawLaser(startX, startY, endX, endY, size, other.block.size);
                }
            }
            Draw.reset();
        }

        @Override
        public void drawSelect() {
            super.drawSelect();
            // 使用格子坐标绘制，与检测范围完全一致
            Draw.color(Pal.accent, 0.35f);
            float halfSize = areaRange * tilesize;
            Drawf.square(x, y, halfSize, 0f);
            Draw.color();

            for (int i = 0; i < cachedLineCount; i++) {
                float lx = getLineX(i);
                float ly = getLineY(i);
                Draw.color(Pal.accent);
                Drawf.circles(lx, ly, 4f);
                Draw.color(Color.white);
                Fonts.outline.draw(String.valueOf(i + 1), lx - 3, ly + 5);
            }
            Draw.reset();
        }

        @Override
        public void drawConfigure() {
            super.drawConfigure();
            Drawf.circles(x, y, laserRange * tilesize);
            Draw.color(Pal.place);
            float halfSize = areaRange * tilesize;
            Drawf.square(x, y, halfSize, 0f);
            Draw.color();

            for (int i = 0; i < cachedLineCount; i++) {
                float lx = getLineX(i);
                float ly = getLineY(i);
                Draw.color(Pal.place);
                Drawf.circles(lx, ly, 5f);
                Draw.color(Color.white);
                Fonts.outline.draw(String.valueOf(i + 1), lx - 3, ly + 5);
            }
            Draw.reset();
        }
    }
}