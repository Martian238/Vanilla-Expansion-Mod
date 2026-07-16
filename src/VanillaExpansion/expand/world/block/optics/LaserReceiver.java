package VanillaExpansion.expand.world.block.optics;

import VanillaExpansion.expand.world.block.production.RotatableCrafter;
import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.Pal;
import mindustry.type.*;
import mindustry.logic.LAccess;
import mindustry.ui.Bar;
import mindustry.world.*;
import mindustry.world.meta.*;
import mindustry.io.*;

import static mindustry.Vars.*;

public class LaserReceiver extends RotatableCrafter {

    public float receiveRange = 120f;
    public Color receiveColor = Color.valueOf("44ff44");
    public Color receiveColor2 = Color.valueOf("66ff66");

    public boolean outputsPower = false;
    public float powerOutput = 0f;

    public LaserReceiver(String name) {
        super(name);
        hasPower = outputsPower;
        outputsPower = true;
        consumePower(powerOutput);
        absorbLasers = true;
    }

    @Override
    public void init() {
        hasPower = outputsPower;
        outputsPower = true;
        if (powerOutput > 0) {
            consumePower(powerOutput);
        }
        super.init();
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.range, receiveRange / tilesize, StatUnit.blocks);
        if (outputsPower && powerOutput > 0) {
            stats.add(Stat.output, powerOutput, StatUnit.powerSecond);
        }
    }

    @Override
    public void setBars() {
        super.setBars();
        addBar("power", (ReceiverBuild entity) -> new Bar(() -> "bar.power", () -> Pal.powerBar, () -> entity.power != null ? entity.power.status : 0f));
        addBar("laser-intensity", (ReceiverBuild entity) -> new Bar(() -> "Laser Intensity: " + entity.getTotalIntensity(), () -> Color.valueOf("88ccff"), () -> entity.getTotalIntensity()));
    }

    public interface LaserReceiverListener {
        void onDataReceived(int channel, float value, LaserEmitter.LaserEmitterBuild sender);
        void onLaserLocked(LaserEmitter.LaserEmitterBuild emitter);
        void onLaserUnlocked();
    }

    public class ReceiverBuild extends RotatableCrafterBuild {
        private Seq<LaserEmitter.LaserEmitterBuild> linkedEmitters = new Seq<>();
        private final Seq<LaserData> receivedData = new Seq<>();
        private IntFloatMap dataChannels = new IntFloatMap();
        private float totalIntensity = 0f;
        private boolean laserLocked = false;
        private float powerProduction = 0f;

        public static final int dataChannelLaser = 0;
        public static final int dataChannelIntensity = 1;

        public class LaserData {
            public int channel;
            public float value;
            public LaserEmitter.LaserEmitterBuild sender;
            public float time;

            public LaserData(int channel, float value, LaserEmitter.LaserEmitterBuild sender) {
                this.channel = channel;
                this.value = value;
                this.sender = sender;
                this.time = Time.time;
            }
        }

        @Override
        public void draw() {
            super.draw();

            if (totalIntensity > 0) {
                float displayIntensity = Math.min(totalIntensity, 1f);
                Draw.color(receiveColor.r, receiveColor.g, receiveColor.b, displayIntensity * 0.5f);
                Draw.rect("laser-receive", x, y, rotation);
                Draw.color();
            }
        }

        @Override
        public void updateTile() {
            findLinkedEmitters();
            updateReceivedData();
            decayDataChannels();
            updatePowerOutput();
        }

        private void findLinkedEmitters() {
            Seq<LaserEmitter.LaserEmitterBuild> previousLock = linkedEmitters.select(e -> e != null && e.isValid() && laserCanReach(e));
            Seq<LaserEmitter.LaserEmitterBuild> newLock = new Seq<>();

            for (LaserEmitter.LaserEmitterBuild emitter : previousLock) {
                if (emitter != null && emitter.isValid() && laserCanReach(emitter)) {
                    newLock.add(emitter);
                }
            }

            boolean lockStatusChanged = (laserLocked && newLock.isEmpty()) || (!laserLocked && !newLock.isEmpty());
            laserLocked = !newLock.isEmpty();

            if (lockStatusChanged) {
                if (laserLocked && newLock.any()) {
                    notifyLaserLocked(newLock.first());
                } else {
                    notifyLaserUnlocked();
                }
            }

            linkedEmitters = newLock;
            
            float directIntensity = calculateTotalIntensity();
            totalIntensity = Math.max(totalIntensity, directIntensity);
        }

        private boolean laserCanReach(LaserEmitter.LaserEmitterBuild emitter) {
            if (emitter == null || !emitter.isValid()) return false;

            float dist = Mathf.dst(x, y, emitter.x, emitter.y);
            float laserRange = ((LaserEmitter) emitter.block).laserRange;
            
            if (dist > laserRange + receiveRange) return false;

            float angleToReceiver = Angles.angle(emitter.x, emitter.y, x, y);
            float emitterAngle = emitter.rotation;

            float angleDiff = Angles.angleDist(angleToReceiver, emitterAngle);
            return angleDiff < 1f;
        }

        private float calculateTotalIntensity() {
            float total = 0f;
            for (LaserEmitter.LaserEmitterBuild emitter : linkedEmitters) {
                if (emitter != null && emitter.isValid()) {
                    float dist = Mathf.dst(x, y, emitter.x, emitter.y);
                    float laserRange = ((LaserEmitter) emitter.block).laserRange;
                    float distanceFactor = 1f - (dist / laserRange);
                    float laserIntensity = ((LaserEmitter) emitter.block).laserIntensity;
                    total += emitter.efficiency * distanceFactor * laserIntensity;
                }
            }
            return Math.min(total, 1f);
        }

        public void receiveLaserData(int channel, float value, LaserEmitter.LaserEmitterBuild sender) {
            receivedData.add(new LaserData(channel, value, sender));
            dataChannels.put(channel, value);
            
            if (channel == dataChannelIntensity) {
                totalIntensity += value;
                totalIntensity = Math.min(totalIntensity, 1f);
            }
        }

        private void updateReceivedData() {
            for (LaserData data : receivedData) {
                if (Time.time - data.time < 60f) {
                    notifyDataReceived(data.channel, data.value, data.sender);
                }
            }
            receivedData.removeAll(d -> Time.time - d.time >= 60f);
        }

        private void decayDataChannels() {
            IntFloatMap newChannels = new IntFloatMap();
            for (IntFloatMap.Entry entry : dataChannels) {
                float decayed = entry.value * 0.95f;
                if (decayed > 0.01f) {
                    newChannels.put(entry.key, decayed);
                }
            }
            dataChannels = newChannels;
            
            totalIntensity *= 0.95f;
            if (totalIntensity < 0.01f) {
                totalIntensity = 0f;
            }
        }

        private void updatePowerOutput() {
            if (outputsPower) {
                powerProduction = totalIntensity > 0.01f ? powerOutput * totalIntensity : 0f;
            }
        }

        private void notifyLaserLocked(LaserEmitter.LaserEmitterBuild emitter) {
            if (emitter instanceof LaserReceiverListener listener) {
                listener.onLaserLocked(emitter);
            }
        }

        private void notifyLaserUnlocked() {
            for (LaserEmitter.LaserEmitterBuild emitter : linkedEmitters) {
                if (emitter != null && emitter.isValid() && emitter instanceof LaserReceiverListener listener) {
                    listener.onLaserUnlocked();
                }
            }
        }

        private void notifyDataReceived(int channel, float value, LaserEmitter.LaserEmitterBuild sender) {
            if (sender instanceof LaserReceiverListener listener) {
                listener.onDataReceived(channel, value, sender);
            }
        }

        public float getChannelValue(int channel) {
            return dataChannels.get(channel, 0f);
        }

        public boolean isLaserLocked() {
            return laserLocked;
        }

        public float getTotalIntensity() {
            return totalIntensity;
        }

        public int getLinkedEmitterCount() {
            return linkedEmitters.size;
        }

        @Override
        public double sense(LAccess sensor) {
            if (sensor == LAccess.range) {
                return receiveRange / tilesize;
            } else if (sensor == LAccess.efficiency) {
                return totalIntensity;
            } else if (sensor == LAccess.enabled) {
                return laserLocked ? 1.0 : 0.0;
            } else {
                return super.sense(sensor);
            }
        }

        @Override
        public void write(arc.util.io.Writes write) {
            super.write(write);
            write.f(totalIntensity);
            write.bool(laserLocked);
        }

        @Override
        public void read(arc.util.io.Reads read, byte revision) {
            super.read(read, revision);
            totalIntensity = read.f();
            laserLocked = read.bool();
        }
    }
}