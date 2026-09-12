package VanillaExpansion.expand.type.unit;

import VanillaExpansion.EntityRegister;
import VanillaExpansion.content.VEJSBlocks;
import VanillaExpansion.expand.world.block.defense.MelonicArrayPillar;
import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Time;
import arc.util.pooling.Pools;
import mindustry.Vars;
import mindustry.entities.Effect;
import mindustry.entities.units.StatusEntry;
import mindustry.gen.Building;
import mindustry.gen.TankUnit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.ConstructBlock;
import mindustry.world.blocks.defense.BaseShield;
import mindustry.world.blocks.storage.CoreBlock;

import static mindustry.Vars.*;

public class DayunTankUnit extends TankUnit {

    public float crushEnergy = 0f;
    public float crushEnergyMax = 600f;
    public float crushEnergyEach = 60f;

    public float healFraction = 0.5f;
    public @Nullable Color ringColor;
    public float ringRadius = -1;
    public float fullHealthMultiplier = 2.5f;
    public float fullSpeedMultiplier = 2.5f;
    public boolean strengthenAfterStart = true;
    public Seq<String> whitelistBlockNames = new Seq<>();

    private boolean start = false;
    private boolean keyLoaded = false;

    public static TankUnit create(){
        return new DayunTankUnit();
    }

    public void loadKeys(){
        if(type instanceof DayunTankUnitType dy){
            crushEnergy = dy.crushEnergy;
            crushEnergyMax = dy.crushEnergyMax;
            crushEnergyEach = dy.crushEnergyEach;
            healFraction = dy.healFraction;
            ringColor = dy.ringColor;
            ringRadius = dy.ringRadius;
            fullHealthMultiplier = dy.fullHealthMultiplier;
            fullSpeedMultiplier = dy.fullSpeedMultiplier;
            strengthenAfterStart = dy.strengthenAfterStart;
            whitelistBlockNames = dy.whitelistBlockNames;
        }
        keyLoaded = true;
    }
    @Override
    public int classId() {
        return EntityRegister.getID(getClass());
    }
    @Override
    public void update(){

        super.update();
        if(!keyLoaded){
            loadKeys();
        }



        if (type.crushFragile && !disarmed) {
            for (int i = 0; i < 8; i++) {
                Point2 offset = Geometry.d8[i];
                var other = Vars.world.buildWorld(x + offset.x * tilesize, y + offset.y * tilesize);
                if (other != null && other.team != team && other.block.crushFragile) {
                    other.damage(team, 1.0E9F);
                }
            }
        }
        int r = Math.max((int)(hitSize * 0.75F / tilesize), 0);
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                Tile t = Vars.world.tileWorld(x + dx * tilesize, y + dy * tilesize);
                if (type.crushDamage > 0 && !disarmed && (walked || deltaLen() >= 0.01F) && t != null && Math.max(Math.abs(dx), Math.abs(dy)) <= r - 1) {
                    if (t.build != null && t.build.team != team) {
                        GODIE(t, t.build);
                    } else if (t.block().unitMoveBreakable) {
                        ConstructBlock.deconstructFinish(t, t.block(), this);
                    }
                }
            }
        }

        if(!start && crushEnergy >= crushEnergyMax)start = true;
        if(crushEnergy > 0)crushEnergy--;
        if(crushEnergy < 0)crushEnergy = 0;
        if(crushEnergy > crushEnergyMax)crushEnergy = crushEnergyMax;
        if(start && crushEnergy <= 0)start = false;
        if (!headless && type instanceof DayunTankUnitType dy) {
            control.sound.loop(dy.truckMusic, this, dy.truckMusicVolume * (start ? crushEnergy / crushEnergyMax : 0f));
        }
        if(!strengthenAfterStart || start) {
            speedMultiplier = 1f + fullSpeedMultiplier * (crushEnergy / crushEnergyMax);
            healthMultiplier = 1f + fullHealthMultiplier * (crushEnergy / crushEnergyMax);
            if (!statuses.isEmpty()) {
                int index = 0;
                while (index < statuses.size) {
                    StatusEntry entry = statuses.get(index++);
                    entry.time = Math.max(entry.time - Time.delta, 0);
                    if (!(entry.effect == null || (entry.time <= 0 && !entry.effect.permanent))) {
                        applied.set(entry.effect.id);
                        if (entry.effect.dynamic) {
                            speedMultiplier *= entry.speedMultiplier;
                            healthMultiplier *= entry.healthMultiplier;
                        } else {
                            speedMultiplier *= entry.effect.speedMultiplier;
                            healthMultiplier *= entry.effect.healthMultiplier;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void draw(){
        super.draw();

        if(ringRadius != 0) {
            Draw.z(Layer.effect);
            Draw.color(ringColor != null ? ringColor : team.color);
            Lines.stroke(start ? 3f : 1f);
            Lines.arc(x, y, ringRadius > 0? ringRadius : type.hitSize, crushEnergy / crushEnergyMax);
        }
    }

    public boolean isWhitelisted(Building b){
        if (b instanceof CoreBlock.CoreBuild) return true;
        if (b instanceof BaseShield.BaseShieldBuild && b.efficiency > 0.1f) return true;
        if (b instanceof MelonicArrayPillar.MelonicArrayPillarBuild) return true;
        if (whitelistBlockNames.contains(b.block.name)) return true;
        return false;
    }

    @Override
    public void impulse(float x, float y){
        if(start || !strengthenAfterStart){
            return;
        }
        super.impulse(x, y);
    }

    public void GODIE(Tile t, Building b){
        if(isWhitelisted(b)){
            b.damage(team, type.crushDamage * Time.delta * t.block().crushDamageMultiplier * state.rules.unitDamage(team) * ((speedMultiplier - 1) / 5 + 1));
            return;
        }
        try {
            crushEnergy = Math.min(crushEnergyMax, crushEnergy + crushEnergyEach);
            if(start) heal(b.health * healFraction);
            TextureRegion flyRegion = b.block.fullIcon != null ? b.block.fullIcon : b.block.uiIcon;
            int size = b.block.size;
            float w = b.block.fullIcon != null ? b.block.fullIcon.width / 4f : size * tilesize;
            float h = b.block.fullIcon != null ? b.block.fullIcon.height / 4f : size * tilesize;
            float baseX = b.x;
            float baseY = b.y;
            float baseVx = vel.x;
            float baseVy = vel.y;
            float vx = (float) (Mathf.range(10f) / Math.sqrt(size));
            float vy = (float) ((Mathf.range(5f) + 10f) / Math.sqrt(size));
            float g = -0.1f;
            float rotSpeed = vx * 3f + Mathf.range(5f);
            float frontSpeed = Math.abs(Mathf.range(Math.abs(Mathf.range((float) (0.5f + Math.sqrt(Math.min(size, 9)) * 0.5f)))));
            Effect flyEffect = new Effect((float) (90f + Math.sqrt(size) * 30f), 1600f, e -> {
                Draw.z(Layer.endPixeled - (5f - frontSpeed));
                float x = baseX + (baseVx + vx) * e.lifetime * e.fin();
                float y = baseY + (baseVy + (vy + 0.5f * e.lifetime * e.fin() * g)) * e.lifetime * e.fin();
                float s = frontSpeed * e.lifetime * e.fin();
                float c = Mathf.clamp(2f - (0.2f + 0.1f * Math.min(size, 4)) * s / (size * tilesize), 0.4f, 1f);
                float ele = 0.8f * (vy * e.lifetime * e.fin() + frontSpeed * e.lifetime * e.fin());
                Draw.color(c, c, c, Mathf.clamp((e.lifetime / 60f) * e.fout()));
                Draw.rect(flyRegion, x, y,
                        w + s, h + s * (h / w), rotSpeed * e.lifetime * e.fin());
                Draw.z(Layer.flyingUnitLow - 1f);
                Drawf.shadow(flyRegion, x - ele, y - ele, w, h,
                        rotSpeed * e.lifetime * e.fin());
                Draw.color();
            });
            flyEffect.at(b.x, b.y);
            Effect.shake(size * 2f, size * 4f, b.x, b.y);
        }catch (Exception ignored){}
        b.kill();
    }
}
