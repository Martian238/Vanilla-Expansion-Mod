package VanillaExpansion.expand.abilities;

import VanillaExpansion.content.CustomFx;
import arc.Core;
import arc.audio.Sound;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Mathf;
import arc.struct.Seq;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.entities.abilities.Ability;
import mindustry.entities.bullet.ArtilleryBulletType;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.bullet.LaserBulletType;
import mindustry.entities.part.DrawPart;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.world.blocks.environment.Floor;

import static arc.graphics.g2d.Draw.color;
import static mindustry.Vars.world;
import static mindustry.type.UnitType.shadowTX;
import static mindustry.type.UnitType.shadowTY;

public class HoverTurretAbility extends Ability {
    public HoverTurretAbility() {
    }

    //TODO

    public float airResistance = 0.07f;
    public float shootingResistance = 0.1f;
    public boolean idleLock = false;
    public float accelerationMultiplier = 0.05f;
    public float rotateMultiplier = 0.25f;
    public float stopSpeed = 0.05f;
    public float maxSpeed = 30f;
    public float maxRange = 400f;
    public float minRotateSpeedThreshold = 0f;

    public float layer = 90f;
    public float offsetX = 0f;
    public float offsetY = -12f;
    public float shootingOffsetX = 30f;
    public float shootingOffsetY = 8f;
    public float shadowElevation = 1f;

    public String sprite = "hail";
    public TextureRegion region;
    public float regionRotation = -90f;
    public Seq<DrawPart> parts = new Seq<>();

    public float reload = 60f / 1.2f;
    public float recoil = 2f;
    public float cooldown = 15f;
    public float warmup = 30f;
    public float shootX = 0f;
    public float shootY = 4f;
    public float shake = 2f;
    public float shootCone = 10f;
    public BulletType bullet = new ArtilleryBulletType(3f, 20){{
        knockback = 0.8f;
        lifetime = 80f;
        width = height = 11f;
        collidesTiles = false;
        splashDamageRadius = 25f * 0.75f;
        splashDamage = 33f;
        homingPower = 0.08f;
        homingRange = 50f;
        trailLength = 7;
        trailWidth = 3;
        collidesAir = true;
    }};
    public Effect shootEffect = Fx.shootSmall;
    public Sound shootSound = Sounds.shootArtillerySmall;
    public float volume = 1f;
    public float minPitch = 0.9f;
    public float maxPitch = 1.1f;

    public boolean drawBeam = false;
    public Color beamColor = Color.valueOf("2eeaea");
    public float beamWidth = 8f;
    public float beamLength = 240f;
    public float beamTime = 15f;
    public float innerBeamWidth = 2f;
    public float innerBeamLength = 39f;

    private float x, y, vx, vy, ax, ay, rot;
    private float targetX, targetY, targetRot;
    private float reloadTimer, warmupTimer, heatTimer, beamTimer;

    @Override
    public void update(Unit unit) {
        setTargets(unit);
        calculatePhysics(unit);
        calculateShoot(unit);
    }

    @Override
    public void draw(Unit unit){
        drawTurret(unit);
    }

    public void calculatePhysics(Unit unit) {
        ax = accelerationMultiplier * getDistance(targetX, targetY, x, y) * Mathf.cosDeg(getAngle(targetX - x, targetY - y));
        ay = accelerationMultiplier * getDistance(targetX, targetY, x, y) * Mathf.sinDeg(getAngle(targetX - x, targetY - y));
        vx += ax;
        vy += ay;
        x += vx;
        y += vy;
        vx -= ((unit.isShooting() || heatTimer > 0f) ? shootingResistance : airResistance) * vx;
        vy -= ((unit.isShooting() || heatTimer > 0f) ? shootingResistance : airResistance) * vy;
        if(Math.abs(vx) <= stopSpeed) vx = 0;
        if(Math.abs(vy) <= stopSpeed) vy = 0;
        if(Math.abs(vx) > maxSpeed) vx = vx > 0 ? maxSpeed : -maxSpeed;
        if(Math.abs(vy) > maxSpeed) vy = vy > 0 ? maxSpeed : -maxSpeed;
        if(Mathf.sinDeg(targetRot - rot) > 0){
            rot += rotateMultiplier * Angles.angleDist(targetRot, rot) * ((unit.isShooting() || heatTimer > 0f) ? 1f : Mathf.clamp(getDistance(0, 0, vx, vy) * 2.5f / maxSpeed));
        }else if(Mathf.sinDeg(targetRot - rot) < 0){
            rot -= rotateMultiplier * Angles.angleDist(targetRot, rot) * ((unit.isShooting() || heatTimer > 0f) ? 1f : Mathf.clamp(getDistance(0, 0, vx, vy) * 2.5f / maxSpeed));
        }
        if(getDistance(x, y, unit.x, unit.y) >= maxRange){
            x = unit.x;
            y = unit.y;
            vx = unit.vel.x;
            vy = unit.vel.y;
        }
        if(idleLock && !(unit.isShooting() || heatTimer > 0f)){
            x = unit.x + getOffset(unit.rotation, offsetX, offsetY, true);
            y = unit.y + getOffset(unit.rotation, offsetX, offsetY, false);
            rot = unit.rotation;
            vx = vy = ax = ay = 0;
        }
    }

    public void setTargets(Unit unit){
        if(unit.isShooting() || heatTimer > 0f){
            targetX = unit.x + getOffset(unit.rotation, shootingOffsetX, shootingOffsetY, true);
            targetY = unit.y + getOffset(unit.rotation, shootingOffsetX, shootingOffsetY, false);
            targetRot = getAngle(unit.aimX - x, unit.aimY - y);
        }else{
            targetX = unit.x + getOffset(unit.rotation, offsetX, offsetY, true);
            targetY = unit.y + getOffset(unit.rotation, offsetX, offsetY, false);
            if(vx != 0 || vy != 0) {
                if(getDistance(0, 0, vx, vy) >= minRotateSpeedThreshold) {
                    targetRot = getAngle(vx, vy);
                }
            }
        }
    }

    public void shoot(Unit unit){
        float bx = x + getOffset(rot, shootX, shootY, true);
        float by = y + getOffset(rot, shootX, shootY, false);
        bullet.create(unit, unit.team, bx, by, rot, 1f, bullet.scaleLife? (getDistance(unit.aimX, unit.aimY, bx, by)) / (bullet.lifetime * bullet.speed) : 1f);
        shootEffect.at(bx, by, rot);
        bullet.smokeEffect.at(bx, by, rot);
        Effect.shake(shake, shake * 2f, bx, by);
        shootSound.at(x, y, Math.abs(Mathf.range(minPitch, maxPitch)), volume);
        vx += recoil * Mathf.cosDeg(rot + 180f);
        vy += recoil * Mathf.sinDeg(rot + 180f);
    }

    public void calculateShoot(Unit unit){
        if(unit.isShooting()){
            if(warmupTimer < warmup) {
                warmupTimer++;
            }else{
                if(reloadTimer <= 0f && Angles.angleDist(rot, getAngle(unit.aimX - x, unit.aimY - y)) <= shootCone){
                    shoot(unit);
                    reloadTimer = reload;
                    heatTimer = cooldown;
                    beamTimer = beamTime;
                }
            }
        }else if(warmupTimer > 0f){
            warmupTimer = 0f;
        }
        if(reloadTimer > 0f){
            reloadTimer--;
        }
        if(heatTimer > 0f){
            heatTimer--;
        }
        if(beamTimer > 0f){
            beamTimer--;
        }
        if(warmupTimer < 0f) warmupTimer = 0f;
        if(reloadTimer < 0f) reloadTimer = 0f;
        if(heatTimer < 0f) heatTimer = 0f;
        if(beamTimer < 0f) beamTimer = 0f;
    }

    @Override
    public void created(Unit unit){
        x = targetX = unit.x + getOffset(unit.rotation, offsetX, offsetY, true);;
        y = targetY = unit.y + getOffset(unit.rotation, offsetX, offsetY, false);;
        vx = vy = ax = ay = rot = targetRot = 0;
    }

    public void drawTurret(Unit unit){
        load(unit);

        if(region != null){
            Draw.z(layer);
            Draw.color(Color.white);
            Draw.rect(region, x, y, rot + regionRotation);
        }
        if(parts.size > 0){
            var params = DrawPart.params.set(warmupTimer / warmup, reloadTimer / reload, reloadTimer / reload, heatTimer / cooldown, 0f, 0f, x, y, rot);

            for(var part : parts){
                part.draw(params);
            }
        }
        if(shadowElevation > 0) {

            float normZ = Draw.z();
            Color normColor = Draw.getColor();

            float e = shadowElevation;
            float sx = x + shadowTX * e, sy = y + shadowTY * e;
            Floor floor = world.floorWorld(sx, sy);

            float dest = floor.canShadow ? 1f : 0f;
            unit.shadowAlpha = unit.shadowAlpha < 0 ? dest : Mathf.approachDelta(unit.shadowAlpha, dest, 0.11f);
            Draw.color(Pal.shadow, Pal.shadow.a * unit.shadowAlpha);

            Draw.z(Math.min(Layer.darkness, Layer.flyingUnitLow - 1f));
            Draw.rect(region, sx, sy, rot + regionRotation);
            Draw.color();

            Draw.z(normZ);
            Draw.color(normColor);
        }
        if(drawBeam && beamTimer > 0f){
            float w = 0f;
            Draw.z(Layer.bullet);
            Draw.color(beamColor);
            w = beamWidth * Mathf.clamp(beamTimer / beamTime);
            Lines.stroke(w);
            Lines.lineAngle(x + getOffset(rot, shootX, shootY + innerBeamLength + 0.5f * w, true), y + getOffset(rot, shootX, shootY + innerBeamLength + 0.5f * w, false), rot, beamLength - innerBeamLength);
            Draw.color(Color.white);
            w /= 2;
            Lines.stroke(w);
            Lines.lineAngle(x + getOffset(rot, shootX, shootY + innerBeamLength, true), y + getOffset(rot, shootX, shootY + innerBeamLength, false), rot, beamLength - innerBeamLength);
            Draw.color(beamColor);
            w = innerBeamWidth;
            Lines.stroke(w);
            Lines.lineAngle(x + getOffset(rot, shootX, shootY + 0.5f * w, true), y + getOffset(rot, shootX, shootY + 0.5f * w, false), rot, innerBeamLength);

            Drawf.light(x, y, x + beamLength * Mathf.cosDeg(rot), y + beamLength * Mathf.sinDeg(rot), beamWidth, beamColor, 1f);
            float t = beamTimer / beamTime,
                    ex = x + getOffset(rot, shootX, shootY + innerBeamLength, true),
                    ey = y + getOffset(rot, shootX, shootY + innerBeamLength, false);
            w = 2f + 10 * t;
            color(Color.valueOf("2eeaea"), Color.white, t);
            Drawf.tri(ex, ey, w, 35f * t, rot);
            Drawf.tri(ex, ey, w, 6f * t, rot + 180f);
            Drawf.tri(ex, ey, w * 0.8f, 25f * t, rot + 30f);
            Drawf.tri(ex, ey, w * 0.8f, 6f * t, rot + 210f);
            Drawf.tri(ex, ey, w * 0.8f, 25f * t, rot - 30f);
            Drawf.tri(ex, ey, w * 0.8f, 6f * t, rot + 150f);
        }
    }

    public void load(Unit unit){
        region = Core.atlas.find(sprite);
        if(parts.size > 0) {
            for (var part : parts) {
                part.load(sprite);
            }
        }
    }

    public float getDistance(float ax, float ay, float bx, float by){
        return Mathf.sqrt(Math.abs((ax - bx) * (ax - bx) + (ay - by) * (ay - by)));
    }
    public float getAngle(float x, float y){
        return Mathf.radiansToDegrees * Mathf.atan2(x, y);
    }
    public float getOffset(float unitRot, float offsetX, float offsetY, boolean cos){
        return cos ? offsetX * Mathf.cosDeg(unitRot - 90f) + offsetY * Mathf.cosDeg(unitRot) : offsetX * Mathf.sinDeg(unitRot - 90f) + offsetY * Mathf.sinDeg(unitRot);
    }
}
