package VanillaExpansion.expand.abilities;

import arc.audio.Sound;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.input.KeyCode;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.struct.Seq;
import arc.util.Nullable;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.entities.bullet.BombBulletType;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.bullet.LiquidBulletType;
import mindustry.entities.part.DrawPart;
import mindustry.gen.*;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.world.blocks.environment.Floor;

import static mindustry.Vars.world;
import static mindustry.type.UnitType.shadowTX;
import static mindustry.type.UnitType.shadowTY;

public class DetainerClawAbility extends Ability {


    public DetainerClawAbility(){

    }


    public float clawLength = 48f;
    public float clawWidth = 4f;
    public float clawRotateSpeed = 3f;
    public float clawShadow = 30f;
    public float clawShadowElevation = 1f;
    public @Nullable Color clawLaserColor;
    public Color clawLaserCenterColor = Color.white;

    public boolean clawFollowAim = true;
    public float clawDefaultAngle = 0f;
    public float clawDistanceDefault = 60f;
    public float clawDistanceMax = 160f;
    public float clawDistanceMin = 40f;

    public float absorbRadius = 24f;
    public boolean absorbSameTeam = true;
    public float releaseSpeed = 13f;
    public Effect absorbEffect = Fx.circleColorSpark;
    public Sound absorbSound = Sounds.shieldHit;
    public Effect releaseEffect = Fx.shockwave;
    public Sound releaseSound = Sounds.none;
    public float releaseShake = 4f;

    public String clawRegion = "test-claw-outline";
    public String clawBottomRegion = "test-claw-bottom-outline";
    public String clawShadowRegion = "test-claw-full";
    public String clawArmRegion = "test-claw-arm";
    public Seq<DrawPart> clawParts = new Seq<>();

    //stereoMode
    public boolean stereoMode = true;
    private boolean aimControlling = false;
    public float rootX = 16f, rootY = 0f;
    public float armLength = 120f;
    public float armDefaultScale = 0.6f;
    public float wristLength = 60f;
    public float wristAngleDefault = 90f;
    public int armSide = 0; //0 - right, 1 - left, 2 - middle
    public int controlKeyJson = 1;

    public String clawSideOuterRegion = "copper-wall";
    public String clawSideInnerRegion = "tungsten-wall";
    public String clawSideSideRegion = "titanium-wall";
    public String clawBottomMainRegion = "thorium-wall";
    public String clawBottomSideRegion = "beryllium-wall";
    public String wristRegion = "carbide-wall";
    public String wristJointRegion = "reinforce-surge-wall";
    public String armBaseRegion = "plastanium-wall";
    public String armJointRegion = "scrap-wall1";
    public String armRegion = "surge-wall";
    public String rootRegion = "phase-wall";

    public float clawSpriteWidth = 12f;
    public float clawSpriteHeight = 24f;
    public float clawSpriteBeamOffset = 12f;
    public float clawSpriteThickness = 2f;
    public float wristSpriteWidth = 8f;
    public float armBaseSpriteWidth = 12f;
    public float armSpriteWidth = 14f;
    public float wristJointSpriteSize = 16f;
    public float armJointSpriteSize = 16f;
    public float rootSpriteSize = 20f;






    private float x, y, rot, rotMultiplier = 1f, totalDamage, clawSpeedMultiplier = 0.2f,
            rotZ, armDistance, armDistanceTarget, wristAngle, wristAngleTarget, armNodeOffset, armRot;
    private final boolean activated = true;
    private boolean hasBullet = false, releaseSoundPlay = false;
    private final BulletType[] tempBulletType = new BulletType[99];
    private int tempBulletNumber = 1;
    private KeyCode controlKey = KeyCode.byOrdinal(28 + controlKeyJson);
    private KeyCode deflectModeKey = KeyCode.num4;
    private KeyCode outputModeKey = KeyCode.num5;
    private KeyCode payloadModeKey = KeyCode.num6;

    private final Effect clawLaserEffect = new Effect(2f, 300f, e -> {
        Draw.z(99.99f);
        Draw.color(e.color, e.fout());
        Lines.stroke(paramField.clawWidth);
        if(!stereoMode) {
            float lx = paramField.x + paramUnit.x;
            float ly = paramField.y + paramUnit.y;
            float lr = 0.5f * paramField.clawLength - 0.5f * paramField.clawWidth;
            Lines.line(lx - lr * Mathf.cosDeg(paramField.rot), ly - lr * Mathf.sinDeg(paramField.rot),
                    lx + lr * Mathf.cosDeg(paramField.rot), ly + lr * Mathf.sinDeg(paramField.rot));
            Drawf.light(lx - lr * Mathf.cosDeg(paramField.rot), ly - lr * Mathf.sinDeg(paramField.rot),
                    lx + lr * Mathf.cosDeg(paramField.rot), ly + lr * Mathf.sinDeg(paramField.rot),
                    paramField.clawWidth, e.color, 0.5f);
        }else{
            float lx = paramField.x + paramUnit.x;
            float ly = paramField.y + paramUnit.y;
            float lr = 0.5f * (paramField.clawLength - paramField.clawWidth) * Mathf.cosDeg(paramField.rot);
            Lines.line(lx - lr * Mathf.cosDeg(paramField.rotZ - 90f), ly - lr * Mathf.sinDeg(paramField.rotZ - 90f),
                    lx + lr * Mathf.cosDeg(paramField.rotZ - 90f), ly + lr * Mathf.sinDeg(paramField.rotZ - 90f));
            Drawf.light(lx - lr * Mathf.cosDeg(paramField.rotZ - 90f), ly - lr * Mathf.sinDeg(paramField.rotZ - 90f),
                    lx + lr * Mathf.cosDeg(paramField.rotZ - 90f), ly + lr * Mathf.sinDeg(paramField.rotZ - 90f),
                    paramField.clawWidth, e.color, 0.5f);
        }
    });
    private final Effect clawLaserCenterEffect = new Effect(2f, 300f, e -> {
        Draw.z(110f);
        Draw.color(e.color, e.fout());
        Lines.stroke(paramField.clawWidth * 0.3f);
        if(!stereoMode) {
            float lx = paramField.x + paramUnit.x;
            float ly = paramField.y + paramUnit.y;
            float lr = 0.5f * paramField.clawLength - 0.5f * paramField.clawWidth * 0.3f;
            Lines.line(lx - lr * Mathf.cosDeg(paramField.rot), ly - lr * Mathf.sinDeg(paramField.rot),
                    lx + lr * Mathf.cosDeg(paramField.rot), ly + lr * Mathf.sinDeg(paramField.rot));
            Drawf.light(lx - lr * Mathf.cosDeg(paramField.rot), ly - lr * Mathf.sinDeg(paramField.rot),
                    lx + lr * Mathf.cosDeg(paramField.rot), ly + lr * Mathf.sinDeg(paramField.rot),
                    paramField.clawWidth * 0.3f, e.color, 0.25f);
        }else{
            float lx = paramField.x + paramUnit.x;
            float ly = paramField.y + paramUnit.y;
            float lr = 0.5f * (paramField.clawLength - paramField.clawWidth * 0.3f) * Mathf.cosDeg(paramField.rot);
            Lines.line(lx - lr * Mathf.cosDeg(paramField.rotZ - 90f), ly - lr * Mathf.sinDeg(paramField.rotZ - 90f),
                    lx + lr * Mathf.cosDeg(paramField.rotZ - 90f), ly + lr * Mathf.sinDeg(paramField.rotZ - 90f));
            Drawf.light(lx - lr * Mathf.cosDeg(paramField.rotZ - 90f), ly - lr * Mathf.sinDeg(paramField.rotZ - 90f),
                    lx + lr * Mathf.cosDeg(paramField.rotZ - 90f), ly + lr * Mathf.sinDeg(paramField.rotZ - 90f),
                    paramField.clawWidth * 0.3f, e.color, 0.25f);
        }
    });




    private static Unit paramUnit;
    private static DetainerClawAbility paramField;
    private static Vec2 paramPos = new Vec2();




    @Override
    public void created(Unit unit){
        rot = 0f;
        x = clawDistanceDefault * Mathf.cosDeg(clawDefaultAngle + unit.rotation);
        y = clawDistanceDefault * Mathf.sinDeg(clawDefaultAngle + unit.rotation);
        tempBulletNumber = 1;
        armDistance = armDistanceTarget = armLength * armDefaultScale;
        rotZ = unit.rotation;
        wristAngle = wristAngleTarget = wristAngleDefault;
    }

    public float getDistance(float ax, float ay, float bx, float by){
        return Mathf.sqrt(Math.abs((ax - bx) * (ax - bx) + (ay - by) * (ay - by)));
    }

    public boolean checkBulletRecreate(Bullet b){
        if(b.type.collidesAir != b.type.collidesGround || !b.type.collidesAir){
            return true;
        }
        if(b.type.drag < 0 || b.type.accel > 0 || b.type.speed < 0.01){
            return true;
        }
        return !b.type.collides || b.type.scaleLife;
    }

    public void clawMove(Unit unit){

        if(clawFollowAim) {
            float tx, ty;
            float aimDistance = getDistance(unit.aimX, unit.aimY, unit.x, unit.y);
            tx = unit.aimX - unit.x;
            ty = unit.aimY - unit.y;
            if (aimDistance > clawDistanceMax || aimDistance < clawDistanceMin) {
                if (aimDistance > clawDistanceMax) {
                    tx = clawDistanceMax * ((unit.aimX - unit.x) / aimDistance);
                    ty = clawDistanceMax * ((unit.aimY - unit.y) / aimDistance);
                } else {
                    tx = clawDistanceMin * ((unit.aimX - unit.x) / aimDistance);
                    ty = clawDistanceMin * ((unit.aimY - unit.y) / aimDistance);
                }
            }
            x = Mathf.approachDelta(x, tx, 0.05f + clawSpeedMultiplier * Math.min(absorbRadius, Math.abs(x - tx)));
            y = Mathf.approachDelta(y, ty, 0.05f + clawSpeedMultiplier * Math.min(absorbRadius, Math.abs(y - ty)));
        }else{
            float tx = clawDistanceDefault * Mathf.cosDeg(clawDefaultAngle + unit.rotation);
            float ty = clawDistanceDefault * Mathf.sinDeg(clawDefaultAngle + unit.rotation);
            x = Mathf.approachDelta(x, tx, 0.05f + clawSpeedMultiplier * Math.min(absorbRadius, Math.abs(x - tx)));
            y = Mathf.approachDelta(y, ty, 0.05f + clawSpeedMultiplier * Math.min(absorbRadius, Math.abs(y - ty)));
        }

    }

    public void clawMoveStereo(Unit unit){
        float tx = unit.x, ty = unit.y,
                aimDistance, c, sinA, sinB, tryDistance,
                d = unit.rotation - 180f;

        float rX = rootX * Mathf.cosDeg(unit.rotation - 90f);
        float rY = rootY * Mathf.sinDeg(unit.rotation - 90f);

        aimDistance = getDistance(unit.aimX, unit.aimY, unit.x + rX, unit.y + rY);
        c = Mathf.atan2(unit.aimX - unit.x - rX, unit.aimY - unit.y - rY);

        if(!aimControlling){
            sinA = Mathf.sinDeg(wristAngle) * armDistance / aimDistance;
            sinB = Mathf.sinDeg(wristAngle + Mathf.radiansToDegrees * (float)Math.asin(sinA));
            tryDistance = aimDistance * sinB / Mathf.sinDeg(wristAngle);
            d = Mathf.radiansToDegrees * (float)Math.asin(sinA) + c - 180f;
            tx = unit.aimX + Mathf.cosDeg(d) * (tryDistance - wristLength) - unit.x - rX;
            ty = unit.aimY + Mathf.sinDeg(d) * (tryDistance - wristLength) - unit.y - rY;
            if(tryDistance >= wristLength * 2f){
                armDistanceTarget = armLength * armDefaultScale;
            }else{
                armDistanceTarget = armLength - armLength * (1 - armDefaultScale) * (wristLength - tryDistance) / wristLength;
            }
            armRot = c - Mathf.radiansToDegrees * (float)Math.asin(sinB);
        }else{
            if(aimDistance <= armLength + wristLength && aimDistance >= clawDistanceMin){
                tx = unit.aimX - unit.x - rX;
                ty = unit.aimY - unit.y - rY;
            }else{
                if (aimDistance > (armLength + wristLength)) {
                    tx = (armLength + wristLength) * ((unit.aimX - unit.x - rX) / aimDistance);
                    ty = (armLength + wristLength) * ((unit.aimY - unit.y - rY) / aimDistance);
                } else {
                    tx = clawDistanceMin * ((unit.aimX - unit.x - rX) / aimDistance);
                    ty = clawDistanceMin * ((unit.aimY - unit.y - rY) / aimDistance);
                }
            }
            float a = 90f * (1 - (aimDistance - clawDistanceMin) / (armLength + wristLength - clawDistanceMin));
            armDistance = (float)Math.sqrt(aimDistance * aimDistance + wristLength * wristLength - 2 * aimDistance * wristLength * Mathf.cosDeg(a));
            d = a + c - 180f;
            sinB = Mathf.sinDeg(a) * wristLength / armDistance;
            armRot = c - Mathf.radiansToDegrees * (float)Math.asin(sinB);
        }

        x = Mathf.approachDelta(x, tx + rX, 0.05f + clawSpeedMultiplier * Math.min(absorbRadius, Math.abs(x - tx - rX)));
        y = Mathf.approachDelta(y, ty + rY, 0.05f + clawSpeedMultiplier * Math.min(absorbRadius, Math.abs(y - ty - rY)));
        rotZ = Mathf.approachDelta(rotZ, d + 180f, 0.05f + clawSpeedMultiplier * 2f * Math.min(8f, Math.abs(d + 180f - rotZ)));
        wristAngle = Mathf.approachDelta(wristAngle, wristAngleTarget, 0.05f + clawSpeedMultiplier * 2f * Math.min(8f, Math.abs(wristAngle - wristAngleTarget)));
        armDistance = Mathf.approachDelta(armDistance, armDistanceTarget, 0.05f + clawSpeedMultiplier * 0.5f * Math.min(absorbRadius, Math.abs(armDistance - armDistanceTarget)));

        armNodeOffset = Mathf.sqrt(armLength * armLength - armDistance * armDistance) / 2;
    }

    @Override
    public void update(Unit unit){

        clawSpeedMultiplier = 0.5f;
        if(hasBullet){
            clawSpeedMultiplier = 0.3f;
        }
        if(!stereoMode) {
            clawMove(unit);
        }else{
            clawMoveStereo(unit);
        }

        paramUnit = unit;
        paramField = this;
        paramPos.set(x, y).rotate(unit.rotation - 90f).add(unit);
        totalDamage = 0f;
        hasBullet = false;
        releaseSoundPlay = false;

        Groups.bullet.intersect(unit.x + x - absorbRadius, unit.y + y - absorbRadius, absorbRadius * 2f, absorbRadius * 2f, clawConsumer);
        Units.nearby(unit.x + x - absorbRadius, unit.y + y - absorbRadius, absorbRadius * 2f, absorbRadius * 2f, missileConsumer);

        if(releaseSoundPlay){
            releaseEffect.at(unit.x + x, unit.y + y, paramUnit.team.color);
            releaseSound.at(unit.x + x, unit.y + y);
            if(releaseShake > 0){
                Effect.shake(releaseShake, releaseShake * 2f, unit.x + x, unit.y + y);
            }
        }

        rotMultiplier = 1f;
        if(hasBullet){
            rotMultiplier = Math.min(10f, 2f * ((totalDamage / 500f) + 1f));
        }
        rot = rot + clawRotateSpeed * rotMultiplier;
        if(rot > 360f) rot -= 360f;
        if(rot < -360f) rot += 360f;

        if(activated){
            if(clawLaserColor == null){
                clawLaserColor = paramUnit.team.color;
            }
            clawLaserEffect.at(unit.x + x, unit.y + y, clawLaserColor);
            clawLaserCenterEffect.at(unit.x + x, unit.y + y, clawLaserCenterColor);
        }
    }

    @Override
    public void draw(Unit unit){
        if(!stereoMode) {
            drawClawDefault(unit);
        }else{
            float rotAbs = Math.abs(rot);
            float drawRot = rotAbs >= 360 ? rotAbs - 360 : (rotAbs >= 180 ? rotAbs - 180 : rotAbs);
            if(clawRotateSpeed > 0) {
                drawClawStereo(unit, drawRot);
            }else{
                drawClawStereo(unit, 180 - drawRot);
            }
        }
    }

    public void drawClawDefault(Unit unit){
        Draw.color(Color.white, 1f);
        Draw.z(Layer.flyingUnitLow);
        Draw.rect(clawRegion, unit.x + x, unit.y + y, rot);
        Draw.z(Draw.z() - 0.1f);
        Draw.rect(clawBottomRegion, unit.x + x, unit.y + y, rot);
        Draw.z(Math.min(Layer.darkness, Layer.flyingUnitLow - 0.2f));
        if(clawShadow > 0) {
            Drawf.shadow(unit.x + x, unit.y + y, clawShadow);
        }
        Draw.z(Layer.flyingUnitLow - 0.3f);
        Draw.rect(clawArmRegion, unit.x + x / 2, unit.y + y / 2, getDistance(unit.x, unit.y, unit.x + x, unit.y + y), 8f, Mathf.radiansToDegrees * Mathf.atan2(x, y));
        if(clawShadowElevation > 0){
            Draw.z(Math.min(Layer.darkness, Layer.flyingUnitLow - 1f));
            drawShadow(unit);
        }
    }

    public void drawClawStereo(Unit unit, float rot){
        Draw.color(Color.white, 1f);
        //claw inner
        Draw.z(Layer.flyingUnitLow - 4f);
        Draw.rect(clawSideInnerRegion, unit.x + x + 0.5f * clawLength * Mathf.cosDeg(rot) * Mathf.cosDeg(rotZ - 90f) + 0.5f * (clawSpriteHeight - clawSpriteBeamOffset) * Mathf.cosDeg(rotZ - 180f),
                unit.y + y + 0.5f * clawLength * Mathf.sinDeg(rotZ - 90f) + 0.5f * (clawSpriteHeight - clawSpriteBeamOffset) * Mathf.sinDeg(rotZ - 180f),
                clawSpriteWidth * Mathf.sinDeg(rot), clawSpriteHeight, rotZ - 90f);
        //claw back sides
        Draw.z(Layer.flyingUnitLow - 4.5f);
        Draw.rect(clawSideSideRegion, unit.x + x + 0.5f * (clawLength + clawSpriteThickness + clawSpriteWidth * Mathf.cosDeg(rot - 90f)) * Mathf.cosDeg(rot) * Mathf.cosDeg(rotZ - 90f) + 0.5f * (clawSpriteHeight - clawSpriteBeamOffset) * Mathf.cosDeg(rotZ - 180f),
                unit.y + y + 0.5f * (clawLength + clawSpriteThickness + clawSpriteWidth * Mathf.cosDeg(rot - 90f)) * Mathf.sinDeg(rotZ - 90f) + 0.5f * (clawSpriteHeight - clawSpriteBeamOffset) * Mathf.sinDeg(rotZ - 180f),
                clawSpriteThickness * Mathf.clamp(Mathf.cosDeg(rot)), clawSpriteHeight, rotZ - 90f);
        Draw.rect(clawSideSideRegion, unit.x + x + 0.5f * (clawLength + clawSpriteThickness - clawSpriteWidth * Mathf.cosDeg(rot - 90f)) * Mathf.cosDeg(rot) * Mathf.cosDeg(rotZ - 90f) + 0.5f * (clawSpriteHeight - clawSpriteBeamOffset) * Mathf.cosDeg(rotZ - 180f),
                unit.y + y + 0.5f * (clawLength + clawSpriteThickness - clawSpriteWidth * Mathf.cosDeg(rot - 90f)) * Mathf.sinDeg(rotZ - 90f) + 0.5f * (clawSpriteHeight - clawSpriteBeamOffset) * Mathf.sinDeg(rotZ - 180f),
                clawSpriteThickness * Mathf.clamp(-Mathf.cosDeg(rot)), clawSpriteHeight, rotZ - 90f);
        //claw outer
        Draw.z(Layer.flyingUnit - 0.1f);
        Draw.rect(clawSideOuterRegion, unit.x + x + 0.5f * (-clawLength + clawSpriteThickness * 2) * Mathf.cosDeg(rot) * Mathf.cosDeg(rotZ - 90f) + 0.5f * (clawSpriteHeight - clawSpriteBeamOffset) * Mathf.cosDeg(rotZ - 180f),
                unit.y + y + 0.5f * (-clawLength + clawSpriteThickness * 2) * Mathf.sinDeg(rotZ - 90f) + 0.5f * (clawSpriteHeight - clawSpriteBeamOffset) * Mathf.sinDeg(rotZ - 180f),
                clawSpriteWidth * Mathf.sinDeg(rot), clawSpriteHeight, rotZ - 90f);
        //claw front sides
        Draw.z(Layer.flyingUnit - 0.2f);
        Draw.rect(clawSideSideRegion, unit.x + x + 0.5f * (-clawLength - clawSpriteThickness + clawSpriteWidth * Mathf.cosDeg(rot - 90f)) * Mathf.cosDeg(rot) * Mathf.cosDeg(rotZ - 90f) + 0.5f * (clawSpriteHeight - clawSpriteBeamOffset) * Mathf.cosDeg(rotZ - 180f),
                unit.y + y + 0.5f * (-clawLength - clawSpriteThickness + clawSpriteWidth * Mathf.cosDeg(rot - 90f)) * Mathf.sinDeg(rotZ - 90f) + 0.5f * (clawSpriteHeight - clawSpriteBeamOffset) * Mathf.sinDeg(rotZ - 180f),
                clawSpriteThickness * Mathf.clamp(Mathf.cosDeg(rot)), clawSpriteHeight, rotZ - 90f);
        Draw.rect(clawSideSideRegion, unit.x + x + 0.5f * (-clawLength - clawSpriteThickness - clawSpriteWidth * Mathf.cosDeg(rot - 90f)) * Mathf.cosDeg(rot) * Mathf.cosDeg(rotZ - 90f) + 0.5f * (clawSpriteHeight - clawSpriteBeamOffset) * Mathf.cosDeg(rotZ - 180f),
                unit.y + y + 0.5f * (-clawLength - clawSpriteThickness - clawSpriteWidth * Mathf.cosDeg(rot - 90f)) * Mathf.sinDeg(rotZ - 90f) + 0.5f * (clawSpriteHeight - clawSpriteBeamOffset) * Mathf.sinDeg(rotZ - 180f),
                clawSpriteThickness * Mathf.clamp(-Mathf.cosDeg(rot)), clawSpriteHeight, rotZ - 90f);
        //claw bottom side
        Draw.z(Layer.flyingUnit - 0.3f);
        Draw.rect(clawBottomSideRegion, unit.x + x + 0.5f * (-clawLength + clawSpriteThickness * 2) * Mathf.cosDeg(rot) * Mathf.cosDeg(rotZ - 90f) + 0.5f * (clawSpriteBeamOffset * 2 + clawSpriteThickness) * Mathf.cosDeg(rotZ - 180f),
                unit.y + y + 0.5f * (-clawLength + clawSpriteThickness * 2) * Mathf.sinDeg(rotZ - 90f) + 0.5f * (clawSpriteBeamOffset * 2 + clawSpriteThickness) * Mathf.sinDeg(rotZ - 180f),
                clawSpriteWidth * Mathf.sinDeg(rot), clawSpriteHeight, rotZ - 90f);
        //claw bottom main
        Draw.z(Layer.flyingUnit - 0.4f);
        Draw.rect(clawBottomMainRegion, unit.x + x + 0.5f * (clawSpriteWidth * Mathf.cosDeg(rot - 90f)) * Mathf.cosDeg(rot) * Mathf.cosDeg(rotZ - 90f) + 0.5f * (clawSpriteBeamOffset * 2 + clawSpriteThickness) * Mathf.cosDeg(rotZ - 180f),
                unit.y + y + 0.5f * (clawSpriteWidth * Mathf.cosDeg(rot - 90f)) * Mathf.sinDeg(rotZ - 90f) + 0.5f * (clawSpriteBeamOffset * 2 + clawSpriteThickness) * Mathf.sinDeg(rotZ - 180f),
                (clawLength + 2 * clawSpriteThickness) * Mathf.clamp(Mathf.cosDeg(rot)), clawSpriteThickness, rotZ - 90f);
        Draw.rect(clawBottomMainRegion, unit.x + x + 0.5f * (-clawSpriteWidth * Mathf.cosDeg(rot - 90f)) * Mathf.cosDeg(rot) * Mathf.cosDeg(rotZ - 90f) + 0.5f * (clawSpriteBeamOffset * 2 + clawSpriteThickness) * Mathf.cosDeg(rotZ - 180f),
                unit.y + y + 0.5f * (-clawSpriteWidth * Mathf.cosDeg(rot - 90f)) * Mathf.sinDeg(rotZ - 90f) + 0.5f * (clawSpriteBeamOffset * 2 + clawSpriteThickness) * Mathf.sinDeg(rotZ - 180f),
                (clawLength + 2 * clawSpriteThickness) * Mathf.clamp(-Mathf.cosDeg(rot)), clawSpriteThickness, rotZ - 90f);
        if(armSide == 0) {
            //wrist
            Draw.z(Layer.flyingUnitLow - 0.3f);
            Draw.rect(wristRegion, unit.x + x - 0.5f * wristLength * Mathf.cosDeg(rotZ - 180f),
                    unit.y + y - 0.5f * wristLength * Mathf.sinDeg(rotZ - 180f),
                    wristLength, wristSpriteWidth, rotZ);
            //wrist joint
            Draw.z(Layer.flyingUnitLow - 0.1f);
            Draw.rect(wristJointRegion, unit.x + x - wristLength * Mathf.cosDeg(rotZ - 180f),
                    unit.y + y - wristLength * Mathf.sinDeg(rotZ - 180f),
                    wristJointSpriteSize, wristJointSpriteSize, 0f);
            //arm base
            Draw.z(Layer.flyingUnitLow - 0.2f);
            Draw.rect(armBaseRegion, unit.x + x - wristLength * Mathf.cosDeg(rotZ - 180f) - 0.25f * armLength * Mathf.cosDeg(rotZ - 360f + wristAngle + Mathf.radiansToDegrees * (float) Math.asin(armNodeOffset / (armLength / 2))),
                    unit.y + y - wristLength * Mathf.sinDeg(rotZ - 180f) - 0.25f * armLength * Mathf.sinDeg(rotZ - 360f + wristAngle + Mathf.radiansToDegrees * (float) Math.asin(armNodeOffset / (armLength / 2))),
                    armLength / 2, armBaseSpriteWidth, rotZ - 180f + wristAngle + Mathf.radiansToDegrees * (float) Math.asin(armNodeOffset / (armLength / 2)));
            //arm joint
            Draw.z(Layer.flyingUnitLow);
            Draw.rect(armJointRegion, unit.x + x - wristLength * Mathf.cosDeg(rotZ - 180f) - 0.5f * armLength * Mathf.cosDeg(rotZ - 360f + wristAngle + Mathf.radiansToDegrees * (float) Math.asin(armNodeOffset / (armLength / 2))),
                    unit.y + y - wristLength * Mathf.sinDeg(rotZ - 180f) - 0.5f * armLength * Mathf.sinDeg(rotZ - 360f + wristAngle + Mathf.radiansToDegrees * (float) Math.asin(armNodeOffset / (armLength / 2))),
                    armJointSpriteSize, armJointSpriteSize, 0f);
            //arm
            Draw.z(Layer.flyingUnitLow - 0.1f);
            Draw.rect(armRegion, unit.x + x - wristLength * Mathf.cosDeg(rotZ - 180f) - 0.25f * armLength * Mathf.cosDeg(rotZ - 360f + wristAngle + Mathf.radiansToDegrees * (float) Math.asin(armNodeOffset / (armLength / 2))) + 0.5f * armDistance * Mathf.cosDeg(rotZ - 360f + wristAngle),
                    unit.x + x - wristLength * Mathf.sinDeg(rotZ - 180f) - 0.25f * armLength * Mathf.sinDeg(rotZ - 360f + wristAngle + Mathf.radiansToDegrees * (float) Math.asin(armNodeOffset / (armLength / 2))) + 0.5f * armDistance * Mathf.sinDeg(rotZ - 360f + wristAngle),
                    armLength / 2, armSpriteWidth, rotZ - 180f + wristAngle - Mathf.radiansToDegrees * (float) Math.asin(armNodeOffset / (armLength / 2)));
            //root
            Draw.z(Layer.flyingUnit + 1f);
            Draw.rect(rootRegion, unit.x + rootX * Mathf.cosDeg(unit.rotation - 90f), unit.y + rootY * Mathf.sinDeg(unit.rotation - 90f),
                    rootSpriteSize, rootSpriteSize, unit.rotation - 90f);
        }
    }

    public void drawClawParts(Unit unit){
        if(clawParts.size > 0){
            DrawPart.params.set(activated? 1f : 0f, hasBullet? 1f : 0f, 0f, 0f, (Angles.angleDist(rot, 0f) + 180f) / 360f, 0f, unit.x + x, unit.y + y, rot);

            for(int i = 0; i < clawParts.size; i++){
                clawParts.get(i).draw(DrawPart.params);
            }
        }
    }

    public void drawShadow(Unit unit){
        float e = clawShadowElevation;
        float sx = unit.x + x + shadowTX * e, sy = unit.y + y + shadowTY * e;
        Floor floor = world.floorWorld(sx, sy);

        float dest = floor.canShadow ? 1f : 0f;
        unit.shadowAlpha = unit.shadowAlpha < 0 ? dest : Mathf.approachDelta(unit.shadowAlpha, dest, 0.11f);
        Draw.color(Pal.shadow, Pal.shadow.a * unit.shadowAlpha);

        Draw.rect(clawShadowRegion, sx, sy, rot);
        Draw.color();
    }








    private static final Cons<Bullet> clawConsumer = b -> {
        if(paramField.activated && b.within(paramUnit.x + paramField.x, paramUnit.y + paramField.y, paramField.absorbRadius) && b.type.absorbable
                && (b.team != paramUnit.team || ((paramField.absorbSameTeam || b.owner == paramUnit) && !paramUnit.isShooting()))
                && !b.type.killShooter && !b.type.underwater && (b.type instanceof BasicBulletType || b.type instanceof LiquidBulletType)) {

            if(b.owner != paramUnit) {
                paramField.absorbEffect.at(b.x, b.y, paramUnit.team.color);
                paramField.absorbSound.at(b.x, b.y, Math.abs(Mathf.range(0.8f, 1.1f)), 1f);
            }
            b.team = paramUnit.team;
            b.time = 0f;
            b.owner = paramUnit;
            b.x = paramUnit.x + paramField.x;
            b.y = paramUnit.y + paramField.y;
            b.rotation(b.rotation() + paramField.clawRotateSpeed + paramField.rotMultiplier);
            b.vel.x = 0f;
            b.vel.y = 0f;
            paramField.hasBullet = true;
            paramField.totalDamage += b.type.damage;
            paramField.totalDamage += b.type.splashDamage;
        }
        if(paramField.activated && b.within(paramUnit.x + paramField.x, paramUnit.y + paramField.y, paramField.absorbRadius / 2f)
                && (b.team == paramUnit.team && b.owner == paramUnit && paramUnit.isShooting())) {

            b.time = 0f;
            float outSpeed = Math.min(Math.max((b.type instanceof BombBulletType)? paramField.releaseSpeed : 3f, 2f * b.type.speed) ,Math.max(b.type.speed, paramField.releaseSpeed));
            if(!paramField.stereoMode) {
                b.vel.x = outSpeed * paramField.x / paramField.getDistance(paramField.x, paramField.y, 0, 0);
                b.vel.y = outSpeed * paramField.y / paramField.getDistance(paramField.x, paramField.y, 0, 0);
            }else{
                b.vel.x = outSpeed * Mathf.cosDeg(paramField.rotZ);
                b.vel.y = outSpeed * Mathf.sinDeg(paramField.rotZ);
            }
            if(b.type instanceof LiquidBulletType || b.type instanceof BombBulletType) {
                b.vel.x += Mathf.range(1f);
                b.vel.y += Mathf.range(1f);
            }
            paramField.releaseSoundPlay = true;

            if(paramField.checkBulletRecreate(b)) {
                paramField.tempBulletType[paramField.tempBulletNumber] = b.type.copy();
                if (!b.type.collides && b.type.hitEffect != b.type.despawnEffect && !b.type.despawnHit) {
                    paramField.tempBulletType[paramField.tempBulletNumber].hitEffect = b.type.despawnEffect;
                }
                paramField.tempBulletType[paramField.tempBulletNumber].collides = true;
                paramField.tempBulletType[paramField.tempBulletNumber].collidesAir = true;
                paramField.tempBulletType[paramField.tempBulletNumber].collidesGround = true;
                paramField.tempBulletType[paramField.tempBulletNumber].collidesTiles = true;
                paramField.tempBulletType[paramField.tempBulletNumber].collideTerrain = false;
                paramField.tempBulletType[paramField.tempBulletNumber].scaleLife = false;
                paramField.tempBulletType[paramField.tempBulletNumber].accel = Math.min(b.type.accel, 0f);
                paramField.tempBulletType[paramField.tempBulletNumber].drag = Math.max(b.type.drag, 0f);
                paramField.tempBulletType[paramField.tempBulletNumber].speed = outSpeed + ((b.type instanceof LiquidBulletType || b.type instanceof BombBulletType)? Mathf.range(1f) : 0f);
                paramField.tempBulletType[paramField.tempBulletNumber].create(paramUnit, paramUnit.team, b.x, b.y, paramField.stereoMode? paramField.rotZ : Mathf.atan2(paramField.x, paramField.y), 1f, 1f);
                b.set(-800, -800);
                b.remove();
                if (paramField.tempBulletNumber < paramField.tempBulletType.length - 1) {
                    paramField.tempBulletNumber++;
                } else {
                    paramField.tempBulletNumber = 1;
                }
            }
        }
    };

    private static final Cons<Unit> missileConsumer = m -> {
        if(paramField.activated && m.within(paramUnit.x + paramField.x, paramUnit.y + paramField.y, paramField.absorbRadius)
                && (m.team != paramUnit.team || ((paramField.absorbSameTeam || m.flag == 1f) && !paramUnit.isShooting()))
                && m.isMissile() && m.flag != 2f) {

            if(m.flag != 1f) {
                paramField.absorbEffect.at(m.x, m.y, paramUnit.team.color);
                paramField.absorbSound.at(m.x, m.y, Math.abs(Mathf.range(0.8f, 1.1f)), 1f);
            }
            m.flag = 1f;
            if(m instanceof TimedKillc tk){
                tk.time(0f);
            }
            m.team = paramUnit.team;
            m.x = paramUnit.x + paramField.x;
            m.y = paramUnit.y + paramField.y;
            m.rotation = paramField.stereoMode? paramField.rotZ : Mathf.radiansToDegrees * Mathf.atan2(paramField.x, paramField.y);
            paramField.totalDamage += m.type.estimateDps();
            paramField.hasBullet = true;
        }
        if(paramField.activated && m.within(paramUnit.x + paramField.x, paramUnit.y + paramField.y, paramField.absorbRadius)
                && m.team == paramUnit.team && m.isMissile() && paramUnit.isShooting() && m.flag == 1f) {

            if(m instanceof TimedKillc tk){
                tk.time(0f);
            }
            m.flag = 2f;
            m.rotation = paramField.stereoMode? paramField.rotZ : Mathf.radiansToDegrees * Mathf.atan2(paramField.x, paramField.y);
            paramField.releaseSoundPlay = true;
        }

    };
}
