package VanillaExpansion.expand.world.block.optics;

import VanillaExpansion.expand.world.block.RotatableBlock;
import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.Drawf;
import mindustry.logic.LAccess;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import mindustry.io.*;

import static mindustry.Vars.*;

public class LaserMirror extends RotatableBlock {

    public TextureRegion laser, laserEnd, laserCenter;

    public float mirrorRange = 200f;
    public float laserWidth = 2f;
    public Color mirrorColor = Color.valueOf("88ccff");
    public Color glowColor = Color.white;
    public float glowIntensity = 0.2f, pulseIntensity = 0.07f;
    public float glowScl = 3f;

    public float surfaceAngleOffset = 0f;
    
    public float mirrorHitboxSize = 0.8f;

    @Override
    public void load() {
        super.load();
        laser = Core.atlas.find(name + "-beam", "drill-laser");
        laserEnd = Core.atlas.find(name + "-beam-end", "drill-laser-end");
        laserCenter = Core.atlas.find(name + "-beam-center", "drill-laser-center");
    }

    public LaserMirror(String name) {
        super(name);
        rotate = true;
        autoRotate = true;
        showConfigUI = true;
        solid = true;
        absorbLasers = true;
        sync = true;
    }

    public float getSurfaceAngle(float blockRotation) {
        return Mathf.mod(blockRotation + surfaceAngleOffset, 360f);
    }

    public float getNormalAngle(float blockRotation) {
        return Mathf.mod(getSurfaceAngle(blockRotation) + 90f, 360f);
    }

    public float reflectAngle(float inputAngle, float blockRotation) {
        float normal = getNormalAngle(blockRotation);
        return Mathf.mod(2f * normal - inputAngle, 360f);
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.range, mirrorRange / tilesize, StatUnit.blocks);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        super.drawPlace(x, y, rotation, valid);
        float centerX = x * tilesize + offset;
        float centerY = y * tilesize + offset;
        float surfaceAngle = getSurfaceAngle((float)rotation);
        float normalAngle = getNormalAngle((float)rotation);

        Draw.color(mirrorColor);
        Lines.stroke(laserWidth);

        float surfaceLen = size * tilesize * 0.4f;
        float normalLen = size * tilesize * 0.3f;

        Lines.line(
            centerX - Angles.trnsx(surfaceAngle, surfaceLen),
            centerY - Angles.trnsy(surfaceAngle, surfaceLen),
            centerX + Angles.trnsx(surfaceAngle, surfaceLen),
            centerY + Angles.trnsy(surfaceAngle, surfaceLen)
        );

        Draw.color(mirrorColor);
        Lines.stroke(1f);
        float normalStartX = centerX + Angles.trnsx(surfaceAngle, surfaceLen * 0.3f);
        float normalStartY = centerY + Angles.trnsy(surfaceAngle, surfaceLen * 0.3f);
        Lines.line(
            normalStartX, normalStartY,
            normalStartX + Angles.trnsx(normalAngle, normalLen),
            normalStartY + Angles.trnsy(normalAngle, normalLen)
        );

        Draw.color();
    }

    public class MirrorBuild extends RotatableBuild {
        private Seq<LaserData> recentLasers = new Seq<>();
        private float lastRotation = 90f;

        public static final int dataChannelLaser = 0;
        public static final int dataChannelIntensity = 1;

        public class LaserData {
            public LaserEmitter.LaserEmitterBuild emitter;
            public float intensity;
            public float inputAngle;
            public float time;
            public Color laserColor;

            public LaserData(LaserEmitter.LaserEmitterBuild emitter, float intensity, float inputAngle, Color laserColor) {
                this.emitter = emitter;
                this.intensity = intensity;
                this.inputAngle = inputAngle;
                this.time = Time.time;
                this.laserColor = laserColor;
            }
        }

        @Override
        public void draw() {
            super.draw();

            float surfaceAngle = getSurfaceAngle(rotation);
            float surfaceLen = size * tilesize * 0.35f;

            Draw.color(mirrorColor);
            Lines.stroke(laserWidth);
            Lines.line(
                x - Angles.trnsx(surfaceAngle, surfaceLen),
                y - Angles.trnsy(surfaceAngle, surfaceLen),
                x + Angles.trnsx(surfaceAngle, surfaceLen),
                y + Angles.trnsy(surfaceAngle, surfaceLen)
            );

            Draw.color(mirrorColor, 0.6f);
            Lines.stroke(1f);
            float normalAngle = getNormalAngle(rotation);
            float normalStartX = x + Angles.trnsx(surfaceAngle, surfaceLen * 0.2f);
            float normalStartY = y + Angles.trnsy(surfaceAngle, surfaceLen * 0.2f);
            Lines.line(
                normalStartX, normalStartY,
                normalStartX + Angles.trnsx(normalAngle, surfaceLen * 0.4f),
                normalStartY + Angles.trnsy(normalAngle, surfaceLen * 0.4f)
            );

            for (LaserData laser : recentLasers) {
                if (laser != null && (laser.emitter == null || laser.emitter.isValid())) {
                    drawReflectedLaser(laser.inputAngle, laser.intensity, laser.laserColor, 0);
                }
            }

            Draw.color();
        }

        private void drawReflectedLaser(float inputAngle, float intensity, Color laserColor, int recursionDepth) {
            if (recursionDepth > 5) return;

            float reflectedAngle = reflectAngle(inputAngle, rotation);
            Building target = findReflectedTarget(reflectedAngle);
            float endX = target != null ? target.x : x + Angles.trnsx(reflectedAngle, mirrorRange);
            float endY = target != null ? target.y : y + Angles.trnsy(reflectedAngle, mirrorRange);

            float baseWidth = laserWidth / 5.5f;
            float width = (baseWidth + Mathf.absin(Time.time + id * 9 + recursionDepth * 5, glowScl, pulseIntensity)) * intensity + baseWidth * 0.5f;

            Draw.z(mindustry.graphics.Layer.power - 1);
            Draw.mixcol(glowColor, Mathf.absin(Time.time + id * 9 + recursionDepth * 5, glowScl, glowIntensity));

            float dist = Mathf.dst(x, y, endX, endY);
            Draw.color(laserColor);
            if (dist < 0.1f) {
                Draw.scl(width);
                Draw.rect(laserCenter, x, y);
                Draw.scl();
            } else {
                Drawf.laser(laser, laserEnd, x, y, endX, endY, width);
            }

            Draw.mixcol();
            Draw.color();

            if (target instanceof MirrorBuild mirror && recursionDepth < 5) {
                mirror.drawReflectedLaser(Mathf.mod(reflectedAngle + 180f, 360f), intensity, laserColor, recursionDepth + 1);
            }
        }

        @Override
        public void updateTile() {
            super.updateTile();
            checkIncomingLasers();
            updateReflectedLasers();
            decayRecentLasers();
            lastRotation = rotation;
        }
        
        private void updateReflectedLasers() {
            for (LaserData laser : recentLasers) {
                if (laser != null && (laser.emitter == null || laser.emitter.isValid())) {
                    float outputAngle = reflectAngle(laser.inputAngle, rotation);
                    Building reflectedTarget = findReflectedTarget(outputAngle);
                    
                    if (reflectedTarget != null) {
                        sendReflectedLaser(reflectedTarget, outputAngle, laser.intensity);
                    }
                }
            }
        }

        private void checkIncomingLasers() {
            for (LaserEmitter.LaserEmitterBuild emitter : getPotentialEmitters()) {
                if (emitter != null && emitter.isValid() && canReceiveFrom(emitter)) {
                    float inputAngle = Angles.angle(emitter.x, emitter.y, x, y);
                    float laserDirection = emitter.rotation;
                    float angleFromLaserToMirror = Angles.angleDist(inputAngle, laserDirection);

                    // 只有当发射器朝向反射镜，且激光能到达反射镜时才接收
                    if (angleFromLaserToMirror < 30f && laserCanReach(emitter)) {
                        float intensity = calculateIntensity(emitter);
                        
                        // 检查是否已经有来自同一发射器的激光，避免重复添加
                        boolean hasExisting = false;
                        for (LaserData existing : recentLasers) {
                            if (existing.emitter == emitter) {
                                // 更新已有数据
                                existing.intensity = intensity;
                                existing.inputAngle = inputAngle;
                                existing.time = Time.time;
                                hasExisting = true;
                                break;
                            }
                        }
                        
                        if (!hasExisting) {
                            Color emitterColor = ((LaserEmitter)emitter.block).laserColor;
                            receiveLaser(emitter, intensity, inputAngle, emitterColor);
                        }
                    } else {
                        // 角度不在范围内或激光无法到达，移除该发射器的激光数据
                        recentLasers.removeAll(d -> d.emitter == emitter);
                    }
                }
            }
            
            // 移除不在范围内的发射器的激光数据
            recentLasers.removeAll(d -> {
                if (d.emitter == null) return false;
                float dist = Mathf.dst(x, y, d.emitter.x, d.emitter.y);
                float maxDist = mirrorRange + ((LaserEmitter)d.emitter.block).laserRange;
                return dist > maxDist;
            });
        }

        private boolean laserCanReach(LaserEmitter.LaserEmitterBuild emitter) {
            if (emitter == null || !emitter.isValid()) return false;

            float dist = Mathf.dst(emitter.x, emitter.y, x, y);
            float laserRange = ((LaserEmitter) emitter.block).laserRange;
            
            // 反射镜必须在发射器的激光射程范围内
            if (dist > laserRange) return false;

            float angleToMirror = Angles.angle(emitter.x, emitter.y, x, y);
            float emitterAngle = emitter.rotation;

            float angleDiff = Angles.angleDist(angleToMirror, emitterAngle);
            return angleDiff < 3.5f;
        }

        private Seq<LaserEmitter.LaserEmitterBuild> getPotentialEmitters() {
            Seq<LaserEmitter.LaserEmitterBuild> result = new Seq<>();
            int range = Math.max(1, (int)(mirrorRange / tilesize) + 2);

            for (int dx = -range; dx <= range; dx++) {
                for (int dy = -range; dy <= range; dy++) {
                    Tile tile = world.tile(this.tile.x + dx, this.tile.y + dy);
                    if (tile != null && tile.build instanceof LaserEmitter.LaserEmitterBuild emitterBuild) {
                        if (emitterBuild.efficiency > 0) {
                            result.add(emitterBuild);
                        }
                    }
                }
            }
            return result;
        }

        private boolean canReceiveFrom(LaserEmitter.LaserEmitterBuild emitter) {
            if (emitter == null || !emitter.isValid()) return false;

            float dist = Mathf.dst(x, y, emitter.x, emitter.y);
            if (dist > mirrorRange + ((LaserEmitter)emitter.block).laserRange) return false;

            return true;
        }

        private float calculateIntensity(LaserEmitter.LaserEmitterBuild emitter) {
            float dist = Mathf.dst(x, y, emitter.x, emitter.y);
            float maxDist = mirrorRange + ((LaserEmitter)emitter.block).laserRange;
            float distanceFactor = 1f - (dist / maxDist);
            return emitter.efficiency * distanceFactor;
        }

        private void receiveLaser(LaserEmitter.LaserEmitterBuild emitter, float intensity, float inputAngle, Color laserColor) {
            notifyLaserReceived(emitter, intensity, inputAngle);

            if (emitter != null) {
                recentLasers.add(new LaserData(emitter, intensity, inputAngle, laserColor));
            }

            float outputAngle = reflectAngle(inputAngle, rotation);
            Building reflectedTarget = findReflectedTarget(outputAngle);

            if (reflectedTarget != null) {
                sendReflectedLaser(reflectedTarget, outputAngle, intensity);
                notifyLaserReflected(reflectedTarget, outputAngle, intensity);
            } else {
                notifyLaserMissed(inputAngle);
            }
        }

        private Building findReflectedTarget(float outputAngle) {
            float step = tilesize * 0.5f;
            float maxDist = mirrorRange;
            float halfWidth = laserWidth / 2f;
            float perpDir = outputAngle + 90f;

            Building closestHit = null;
            float closestDist = Float.MAX_VALUE;

            for (float d = step; d <= maxDist; d += step) {
                float centerX = x + Angles.trnsx(outputAngle, d);
                float centerY = y + Angles.trnsy(outputAngle, d);

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
                    return closestHit;
                }
            }
            return null;
        }

        private void sendReflectedLaser(Building target, float outputAngle, float intensity) {
            if (target instanceof LaserReceiver.ReceiverBuild receiver) {
                receiver.receiveLaserData(dataChannelIntensity, intensity, null);
            }
            if (target instanceof LaserEmitter.LaserEmitterBuild emitter) {
                if (target.block.absorbLasers) {
                }
            }
        }

        private void decayRecentLasers() {
            recentLasers.removeAll(d -> Time.time - d.time > 60f);
        }

        private void notifyLaserReceived(LaserEmitter.LaserEmitterBuild emitter, float intensity, float inputAngle) {
            if (emitter instanceof MirrorListener listener) {
                listener.onLaserReceived(this, emitter, intensity, inputAngle);
            }
        }

        private void notifyLaserReflected(Building target, float outputAngle, float intensity) {
            if (target instanceof MirrorListener listener) {
                listener.onLaserReflected(this, target, outputAngle, intensity);
            }
        }

        private void notifyLaserMissed(float inputAngle) {
        }

        public float getReflectedAngle(float inputAngle) {
            return reflectAngle(inputAngle, rotation);
        }

        public int getRecentLaserCount() {
            return recentLasers.size;
        }

        @Override
        public double sense(LAccess sensor) {
            if (sensor == LAccess.range) {
                return mirrorRange / tilesize;
            } else {
                return super.sense(sensor);
            }
        }

        @Override
        public void write(arc.util.io.Writes write) {
            super.write(write);
            write.f(mirrorRange);
        }

        @Override
        public void read(arc.util.io.Reads read, byte revision) {
            super.read(read, revision);
            mirrorRange = read.f();
        }
    }

    public interface MirrorListener {
        void onLaserReceived(MirrorBuild mirror, LaserEmitter.LaserEmitterBuild emitter, float intensity, float inputAngle);
        void onLaserReflected(MirrorBuild mirror, Building target, float outputAngle, float intensity);
        void onLaserMissed(MirrorBuild mirror, float inputAngle);
    }
}