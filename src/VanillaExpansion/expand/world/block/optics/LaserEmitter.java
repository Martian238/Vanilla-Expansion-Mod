package VanillaExpansion.expand.world.block.optics;

import VanillaExpansion.expand.world.block.production.RotatableCrafter;
import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.Geometry;
import arc.struct.*;
import arc.util.*;
import mindustry.content.Fx;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.Drawf;
import mindustry.logic.LAccess;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import mindustry.io.*;

import static mindustry.Vars.*;

public class LaserEmitter extends RotatableCrafter {

    public TextureRegion laser, laserEnd, laserCenter;

    public float laserRange = 120f;
    public float laserWidth = 3f;
    public float laserIntensity = 1f; // 可配置的光照强度
    public Color laserColor = Color.valueOf("ff4444");
    public Color glowColor = Color.white;
    public float glowIntensity = 0.2f, pulseIntensity = 0.07f;
    public float glowScl = 3f;
    public Effect laserEffect = Fx.none;
    public Effect laserHitEffect = Fx.none;

    public boolean consumesPower = true;
    public float powerUsage = 5f;

    @Override
    public void load() {
        super.load();
        laser = Core.atlas.find(name + "-beam", "drill-laser");
        laserEnd = Core.atlas.find(name + "-beam-end", "drill-laser-end");
        laserCenter = Core.atlas.find(name + "-beam-center", "drill-laser-center");
    }

    public LaserEmitter(String name) {
        super(name);
        hasPower = consumesPower;
        consumePower(powerUsage);
    }

    @Override
    public void init() {
        hasPower = consumesPower;
        if (consumesPower) {
            consumePower(powerUsage);
        }
        super.init();
    }

    @Override
    public void setStats() {
        super.setStats();
        if (consumesPower) {
            stats.add(Stat.powerUse, powerUsage, StatUnit.powerSecond);
        }
        stats.add(Stat.range, laserRange / tilesize, StatUnit.blocks);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        super.drawPlace(x, y, rotation, valid);
        Draw.color(laserColor);
        Lines.stroke(laserWidth);
        Lines.line(x * tilesize + offset, y * tilesize + offset,
            x * tilesize + offset + Geometry.d4x(rotation) * laserRange,
            y * tilesize + offset + Geometry.d4y(rotation) * laserRange);
        Lines.stroke(1f);
        Draw.color();
    }

    public interface LaserTargetListener {
        void onLaserHit(LaserEmitterBuild emitter, Building target, float intensity);
        void onLaserContact(LaserEmitterBuild emitter, Building target);
        void onLaserDisconnect(LaserEmitterBuild emitter, Building target);
    }

    public class LaserEmitterBuild extends RotatableCrafterBuild {
        private Seq<Building> contactedBuildings = new Seq<>();
        private Seq<LaserData> laserDataQueue = new Seq<>();
        private float lastEfficiency = 0f;

        public static final int dataChannelLaser = 0;
        public static final int dataChannelIntensity = 1;
        public static final int dataChannelCustom = 100;

        public class LaserData {
            public int channel;
            public float value;
            public Building target;

            public LaserData(int channel, float value, Building target) {
                this.channel = channel;
                this.value = value;
                this.target = target;
            }
        }

        @Override
        public void draw() {
            super.draw();

            LaserEmitter emitter = (LaserEmitter) block;
            if (efficiency > 0 && emitter.laserRange > 0) {
                float startX = x;
                float startY = y;
                float endX = x + Angles.trnsx(rotation, emitter.laserRange);
                float endY = y + Angles.trnsy(rotation, emitter.laserRange);

                // 如果有接触到的方块，将激光终点设置为第一个接触的方块位置
                if (!contactedBuildings.isEmpty()) {
                    Building firstContact = contactedBuildings.first();
                    endX = firstContact.x;
                    endY = firstContact.y;
                }

                float width = (emitter.laserWidth / 8f + Mathf.absin(Time.time + id * 9, emitter.glowScl, emitter.pulseIntensity)) * efficiency * emitter.laserIntensity;

                Draw.z(mindustry.graphics.Layer.power - 1);
                Draw.mixcol(emitter.glowColor, Mathf.absin(Time.time + id * 9, emitter.glowScl, emitter.glowIntensity));

                float dist = Mathf.dst(startX, startY, endX, endY);
                Draw.color(emitter.laserColor);
                if (dist < 0.1f) {
                    Draw.scl(width);
                    Draw.rect(emitter.laserCenter, startX, startY);
                    Draw.scl();
                } else {
                    Drawf.laser(emitter.laser, emitter.laserEnd, startX, startY, endX, endY, width);
                }

                Draw.mixcol();
                Draw.color();

                if (emitter.laserEffect != Fx.none && wasVisible) {
                    emitter.laserEffect.at(startX, startY, rotation);
                }
            }
        }

        @Override
        public void updateTile() {
            float currentEfficiency = efficiency;

            if (consumesPower && currentEfficiency > 0) {
                updateLaserContact();
            } else if (lastEfficiency > 0) {
                notifyLaserDisconnect();
            }

            processLaserDataQueue();
            lastEfficiency = currentEfficiency;

            super.updateTile();
        }

        private void updateLaserContact() {
            Seq<Building> previousContacts = contactedBuildings.select(b -> b.isValid());
            Seq<Building> newContacts = getLaserContacts();

            for (Building target : newContacts) {
                if (!previousContacts.contains(target)) {
                    notifyLaserConnect(target);
                }
            }

            for (Building target : previousContacts) {
                if (!newContacts.contains(target)) {
                    notifyLaserDisconnect(target);
                }
            }

            contactedBuildings = newContacts;

            for (Building target : contactedBuildings) {
                float intensity = calculateLaserIntensity(target);
                notifyLaserHit(target, intensity);
                // 发送激光数据到接触的目标
                sendLaserData(dataChannelIntensity, intensity, target);
            }
        }

        public Seq<Building> getLaserContacts() {
            Seq<Building> result = new Seq<>();
            float dir = rotation;
            LaserEmitter emitter = (LaserEmitter) block;

            float step = tilesize * 0.5f;
            float targetX = x + Angles.trnsx(dir, emitter.laserRange);
            float targetY = y + Angles.trnsy(dir, emitter.laserRange);

            float totalDist = Mathf.dst(x, y, targetX, targetY);
            float steps = totalDist / step;
            int maxSteps = Math.min((int)steps + 1, 100);

            float halfWidth = emitter.laserWidth / 2f;
            float perpDir = dir + 90f;

            Building closestHit = null;
            float closestDist = Float.MAX_VALUE;

            for (int i = 1; i <= maxSteps; i++) {
                float progress = (float)i / maxSteps;
                float centerX = Mathf.lerp(x, targetX, progress);
                float centerY = Mathf.lerp(y, targetY, progress);

                for (float offset = -1f; offset <= 1f; offset += 0.5f) {
                    float checkX = centerX + Angles.trnsx(perpDir, halfWidth * offset);
                    float checkY = centerY + Angles.trnsy(perpDir, halfWidth * offset);

                    Tile tile = world.tileWorld(checkX, checkY);
                    if (tile != null && tile.build != null && tile.build != this) {
                        if (tile.build.block.absorbLasers) {
                            float dist = Mathf.dst(x, y, tile.build.x, tile.build.y);
                            if (dist < closestDist) {
                                closestDist = dist;
                                closestHit = tile.build;
                            }
                        }
                    }
                }

                if (closestHit != null) {
                    result.add(closestHit);
                    break;
                }
            }

            return result;
        }

        private float calculateLaserIntensity(Building target) {
            LaserEmitter emitter = (LaserEmitter) block;
            float distance = Mathf.dst(x, y, target.x, target.y);
            float distanceFactor = 1f - (distance / emitter.laserRange);
            return efficiency * distanceFactor * emitter.laserIntensity;
        }

        public void sendLaserData(int channel, float value, Building target) {
            if (target != null && target.isValid()) {
                laserDataQueue.add(new LaserData(channel, value, target));
            }
        }

        public void broadcastLaserData(int channel, float value) {
            for (Building target : contactedBuildings) {
                if (target != null && target.isValid()) {
                    laserDataQueue.add(new LaserData(channel, value, target));
                }
            }
        }

        private void processLaserDataQueue() {
            for (LaserData data : laserDataQueue) {
                if (data.target != null && data.target.isValid()) {
                    // 只向正在阻挡激光的方块传输数据
                    if (contactedBuildings.contains(data.target)) {
                        if (data.target instanceof LaserTargetListener listener) {
                            switch (data.channel) {
                                case dataChannelLaser -> listener.onLaserContact(this, data.target);
                                case dataChannelIntensity -> listener.onLaserHit(this, data.target, data.value);
                                default -> {
                                }
                            }
                        }
                        if (data.target instanceof LaserReceiver.ReceiverBuild receiver) {
                            receiver.receiveLaserData(data.channel, data.value, this);
                        }
                    }
                }
            }
            laserDataQueue.clear();
        }

        private void notifyLaserConnect(Building target) {
            if (target instanceof LaserTargetListener listener) {
                listener.onLaserContact(this, target);
            }
        }

        private void notifyLaserDisconnect() {
            for (Building target : contactedBuildings) {
                notifyLaserDisconnect(target);
            }
            contactedBuildings.clear();
        }

        private void notifyLaserDisconnect(Building target) {
            if (target != null && target.isValid() && target instanceof LaserTargetListener listener) {
                listener.onLaserDisconnect(this, target);
            }
        }

        private void notifyLaserHit(Building target, float intensity) {
            if (target instanceof LaserTargetListener listener) {
                listener.onLaserHit(this, target, intensity);
            }
        }

        @Override
        public double sense(LAccess sensor) {
            if (sensor == LAccess.range) {
                return laserRange / tilesize;
            } else if (sensor == LAccess.efficiency) {
                return efficiency;
            } else if (sensor == LAccess.progress) {
                return progress();
            } else {
                return super.sense(sensor);
            }
        }

        @Override
        public void write(arc.util.io.Writes write) {
            super.write(write);
            write.f(laserRange);
        }

        @Override
        public void read(arc.util.io.Reads read, byte revision) {
            super.read(read, revision);
            laserRange = read.f();
        }
    }
}