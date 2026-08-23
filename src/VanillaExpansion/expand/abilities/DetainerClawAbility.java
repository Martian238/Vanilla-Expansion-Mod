package VanillaExpansion.expand.abilities;

import arc.Core;
import arc.audio.Sound;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.input.KeyCode;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.entities.abilities.Ability;
import mindustry.entities.bullet.*;
import mindustry.entities.effect.ExplosionEffect;
import mindustry.entities.effect.MultiEffect;
import mindustry.entities.effect.ParticleEffect;
import mindustry.entities.effect.WrapEffect;
import mindustry.entities.part.DrawPart;
import mindustry.gen.*;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.EmptyFloor;
import mindustry.world.blocks.environment.Floor;

import static java.lang.Float.NaN;
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
    public float clawDistanceDefault = 80f;
    public float clawDistanceMax = 160f;
    public float clawDistanceMin = 40f;

    public float absorbRadius = 24f;
    public boolean absorbSameTeam = true;
    public float releaseSpeed = 13f;
    public Effect absorbEffect = Fx.circleColorSpark;
    public Sound absorbSound = Sounds.shieldHit;
    public Effect releaseEffect = Fx.shockwave;
    public Sound releaseSound = Sounds.none;
    public float releaseShake = 6f;

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
    public String wristJointRegion = "reinforced-surge-wall";
    public String armBaseRegion = "plastanium-wall";
    public String armJointRegion = "scrap-wall1";
    public String armRegion = "surge-wall";
    public String rootRegion = "phase-wall";

    public float clawSpriteWidth = 12f;
    public float clawSpriteHeight = 24f;
    public float clawSpriteBeamOffset = 18f;
    public float clawSpriteThickness = 3f;
    public float wristSpriteWidth = 8f;
    public float armBaseSpriteWidth = 12f;
    public float armSpriteWidth = 14f;
    public float wristJointSpriteSize = 16f;
    public float armJointSpriteSize = 16f;
    public float rootSpriteSize = 20f;

    public Seq<Integer> otherControlKeys = new Seq<>();
    public Sound controlKeySound = Sounds.click;
    public Effect controlKeyEffect = Fx.spawn;
    public Sound switchModeSound = Sounds.click;
    public boolean switchModeSoundPlayer = true;

    public boolean faceControlling = true;
    public boolean outputLaser = false;
    public Effect energyEffect = Fx.none;
    public Sound energyOrbSound = Sounds.shootNavanax;
    public Sound laserShootSound = Sounds.none;
    public Sound laserLoopSound = Sounds.none;






    private float x, y, rot, rotMultiplier = 1f, totalDamage, clawSpeedMultiplier = 0.2f,
            rotZ, armDistance, armDistanceTarget, wristAngle, wristAngleTarget, armNodeOffset, armRot, dvx, dvy, lastX, lastY;
    private final boolean activated = true;
    private boolean hasBullet = false, releaseSoundPlay = false;
    private final BulletType[] tempBulletType = new BulletType[99];
    private int tempBulletNumber = 1;
    private KeyCode controlKey = KeyCode.num1;
    private KeyCode deflectModeKey = KeyCode.num4; //mode0
    private KeyCode outputModeKey = KeyCode.num5; //mode1
    private KeyCode payloadModeKey = KeyCode.num6; //mode2
    private boolean flip = (rootX < 0);
    private int mode;
    private float totalEnergy;







    private static Unit paramUnit;
    private static DetainerClawAbility paramField;
    private static Vec2 paramPos = new Vec2();




    @Override
    public void created(Unit unit){
        rot = 0f;
        x = clawDistanceDefault * Mathf.cosDeg(clawDefaultAngle + unit.rotation);
        y = clawDistanceDefault * Mathf.sinDeg(clawDefaultAngle + unit.rotation);
        if(stereoMode){
            x = rootX;
            y = rootY;
        }
        tempBulletNumber = 1;
        armDistance = armDistanceTarget = armLength * armDefaultScale;
        rotZ = unit.rotation;
        wristAngle = wristAngleTarget = wristAngleDefault;
        unit.flag(0);

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
                d = unit.rotation - 180f, a = 0f;

        float rX = rootX * Mathf.cosDeg(unit.rotation - 90f) + rootY * Mathf.cosDeg(unit.rotation);
        float rY = rootX * Mathf.sinDeg(unit.rotation - 90f) + rootY * Mathf.sinDeg(unit.rotation);

        aimDistance = getDistance(unit.aimX, unit.aimY, unit.x + rX, unit.y + rY);
        c = Mathf.radiansToDegrees * Mathf.atan2(unit.aimX - unit.x - rX, unit.aimY - unit.y - rY);

        if(!aimControlling){
            /*
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
            */

                tx = armLength * armDefaultScale * Mathf.cosDeg(unit.rotation + (armSide == 0? -90f : 90f)) + wristLength * Mathf.cosDeg(unit.rotation);
                ty = armLength * armDefaultScale * Mathf.sinDeg(unit.rotation + (armSide == 0? -90f : 90f)) + wristLength * Mathf.sinDeg(unit.rotation);

            float tempDistance = getDistance(x, y, rX, rY);

                a = (float) (Mathf.radiansToDegrees * Math.asin(armLength * armDefaultScale / clawDistanceDefault));
            armDistanceTarget = (float) Math.sqrt(tempDistance * tempDistance - wristLength * wristLength);
            sinB = Mathf.sinDeg(a) * wristLength / armDistanceTarget;
            wristAngleTarget = wristAngleDefault;
            d = a + c - 180f;

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
            a = 90f * Mathf.clamp(1 - (aimDistance - clawDistanceMin) / (armLength + wristLength - clawDistanceMin));
            armDistanceTarget = Math.min(armLength, (float)Math.sqrt(aimDistance * aimDistance + wristLength * wristLength - 2 * aimDistance * wristLength * Mathf.cosDeg(a)));
            sinB = Mathf.sinDeg(a) * wristLength / armDistanceTarget;
            wristAngleTarget = (float) (Mathf.radiansToDegrees * (Math.asin(Mathf.sinDeg(a) * aimDistance / armDistanceTarget)));
            wristAngleTarget = a + Mathf.radiansToDegrees * Math.acos(Math.sqrt(1 - sinB * sinB)) < 90f ? 180f - wristAngleTarget : wristAngleTarget;
            if(!flip) {
                d = a + c - 180f;
            }else{
                d = -a + c - 180f;
            }

            armRot = c - Mathf.radiansToDegrees * (float)Math.asin(sinB);
        }

        //Log.info("wA:" + wristAngleTarget + ", armRot:" + armRot + ", armD:" + armDistanceTarget + ", a:" + a);

        x = Mathf.approachDelta(x, tx + rX, 0.05f + clawSpeedMultiplier * Math.min(absorbRadius, Math.abs(x - tx - rX)) * (!aimControlling? 0.5f : 1f));
        y = Mathf.approachDelta(y, ty + rY, 0.05f + clawSpeedMultiplier * Math.min(absorbRadius, Math.abs(y - ty - rY)) * (!aimControlling? 0.5f : 1f));
        if(Math.abs(x - tx - rX) > 800f) x = tx + rX;
        if(Math.abs(y - ty - rY) > 800f) y = ty + rY;
        if(aimControlling) {
            if(!flip) {
                rotZ = Angles.angleDist(rotZ, d + 180f) <= 30f ? d + 180f : (Mathf.sinDeg(rotZ - d - 180f) > 0 ? rotZ - 30f : rotZ + 30f);
            }else{
                rotZ = (Angles.angleDist(rotZ, d + 180f) <= 30f ? d + 180f : (Mathf.sinDeg(rotZ - d - 180f) > 0 ? rotZ - 30f : rotZ + 30f));
            }
        }else{
            //rotZ = (float) (unit.rotation - (Mathf.radiansToDegrees * (Mathf.atan2(x - rX, y - rY) - Math.asin(sinB))) - unit.rotation + 90f);
            //Log.info(armDistanceTarget);
            if(!flip) {
                rotZ = (float) (Mathf.radiansToDegrees * (Mathf.atan2(x - 2 * rX, y - 2 * rY)) + a - 20f);
            }else{
                rotZ = -90f + (float) (Mathf.radiansToDegrees * (Mathf.atan2(x - 2 * rX, y - 2 * rY)) + a - 20f);
            }
        }
        wristAngle = Mathf.approachDelta(wristAngle, flip? -wristAngleTarget : wristAngleTarget, 50f + clawSpeedMultiplier * 2f * Math.min(8f, Math.abs(wristAngle - (flip? -wristAngleTarget : wristAngleTarget))));
        armDistance = Mathf.approachDelta(armDistance, armDistanceTarget, 50f + clawSpeedMultiplier * 0.5f * Math.min(absorbRadius, Math.abs(armDistance - armDistanceTarget)));
        if(Float.isNaN(armDistance)) {
            armDistance = armDistanceTarget;
        }
        if (Math.abs(armDistance - armDistanceTarget) > 800f) y = armDistance = armDistanceTarget;
        armNodeOffset = armDistance >= armLength ? 0 : (armDistance <= 0 ? armLength / 2 : (Mathf.sqrt(Math.abs(armLength * armLength - armDistance * armDistance)) / 2));
        if(Float.isNaN(armNodeOffset)){
            armNodeOffset = 0;
        }
    }



    public void unitFaceClaw(Unit unit){
        float angle = Mathf.radiansToDegrees * Mathf.atan2(unit.aimX - unit.x, unit.aimY - unit.y);
        float sin = Mathf.sinDeg(angle - unit.rotation);
        if(Angles.angleDist(unit.rotation, angle) > 45) {
            if (sin > 0) {
                unit.rotation(unit.rotation + unit.type.rotateSpeed);
            } else if (sin < 0) {
                unit.rotation(unit.rotation - unit.type.rotateSpeed);
            }
            if (Angles.angleDist(unit.rotation, angle) <= unit.type.rotateSpeed) {
                unit.rotation(angle);
            }
        }
    }

    @Override
    public void update(Unit unit){
        if(rootX > 0){
            armSide = 0;
            flip = false;
        }else if(rootX < 0){
            armSide = 1;
            flip = true;
        }

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
        if(mode != 1) totalEnergy = 0f;

        Groups.bullet.intersect(unit.x + x - absorbRadius, unit.y + y - absorbRadius, absorbRadius * 2f, absorbRadius * 2f, clawConsumer);
        Units.nearby(unit.x + x - absorbRadius, unit.y + y - absorbRadius, absorbRadius * 2f, absorbRadius * 2f, missileConsumer);
        Units.nearby(unit.x + x - absorbRadius, unit.y + y - absorbRadius, absorbRadius * 2f, absorbRadius * 2f, unitConsumer);

        if(releaseSoundPlay){
            releaseEffect.at(unit.x + x, unit.y + y, paramUnit.team.color);
            releaseSound.at(unit.x + x, unit.y + y);
            if(releaseShake > 0){
                Effect.shake(releaseShake, releaseShake * 2f, unit.x + x, unit.y + y);
            }
        }
        if(mode == 1 && totalEnergy > 0){
            float d = Math.min(totalEnergy, Math.max(totalEnergy * 0.01f, 10f));
            unit.flag += d;
            totalEnergy -= d;
            Fx.circleColorSpark.at(unit.x + x, unit.y + y, 0f, clawLaserColor == null? unit.team.color : clawLaserColor);
        }

        rotMultiplier = 1f;
        if(hasBullet){
            rotMultiplier = Mathf.approachDelta(rotMultiplier, Math.min(10f, 2f * ((totalDamage / 500f) + 1f)), 5f);
        }
        if(unit.flag > 0 || totalEnergy > 0){
            rotMultiplier = Mathf.approachDelta(rotMultiplier, (float) Math.min(10f, 2f * (((unit.flag + totalEnergy) / 1000f) + 1f)), 5f);
        }
        if(laserShooting){
            rotMultiplier = Mathf.approachDelta(rotMultiplier, (float) Math.min(10f, 1f * (((unit.flag + totalEnergy) / 1000f) + 5.5f)), 5f);
        }
        if(rotMultiplier > 10f) rotMultiplier = 10f;
        if(rotMultiplier < 1f) rotMultiplier = 1f;
        if(Float.isNaN(rotMultiplier)) rotMultiplier = 1f;
        rot = rot + clawRotateSpeed * rotMultiplier;
        if(rot > 360f) rot -= 360f;
        if(rot < -360f) rot += 360f;





        controlling(unit);

        if(faceControlling && aimControlling && unit.isPlayer()){
            unitFaceClaw(unit);
        }

        if(delayShootingTimer > 0) delayShootingTimer -= 1;

        //Log.info(x +", "+ y);

        dvx = x - lastX;
        dvy = y - lastY;
        lastX = x;
        lastY = y;

        laserShooting = false;
        if(unit.flag + totalEnergy > 0 && mode == 1){
            outputModeAttack(unit);
        }else{
            if(unit.flag + totalEnergy > 0 && mode == 2){
                unit.flag(0);
                totalEnergy = 0;
            }else{
                energyCharging = false;
            }
        }
        Vec2 laserSoundPos = new Vec2();
        laserSoundPos.x = unit.x + x;
        laserSoundPos.y = unit.y + y;
        if(laserShooting) Vars.control.sound.loop(laserLoopSound, laserSoundPos, 1f);
        if(orbTimer > 0){
            orbTimer -= 1;
        }
    }//end update()




    public void controlling(Unit unit){
        controlKey = KeyCode.byOrdinal(28 + controlKeyJson);

        if(Core.input.keyTap(controlKey) && unit.isPlayer()){
            aimControlling = !aimControlling;
            controlKeySound.at(unit.x + x, unit.y + y);
            if(aimControlling) {
                controlKeyEffect.at(unit.x + x, unit.y + y);
            }else{
                controlInit(unit);
            }
        }
        if(!otherControlKeys.isEmpty() && unit.isPlayer()) {
            for (Integer i : otherControlKeys) {
                if (Core.input.keyTap(KeyCode.byOrdinal(28 + i)) && unit.isPlayer()) {
                    aimControlling = false;
                    controlInit(unit);
                }
            }
        }
        if(unit.isPlayer()){
            if(Core.input.keyTap(deflectModeKey)){
                mode = 0;
                if(switchModeSoundPlayer){
                    switchModeSound.at(unit.x, unit.y, 1, 0.3f);
                    Vars.ui.announce(Core.bundle.format("detainerclaw.mode0"), 1f);
                }
            }
            if(Core.input.keyTap(outputModeKey)){
                mode = 1;
                if(switchModeSoundPlayer){
                    switchModeSound.at(unit.x, unit.y, 1, 0.3f);
                    Vars.ui.announce(Core.bundle.format("detainerclaw.mode1"), 1f);
                }
            }
            if(Core.input.keyTap(payloadModeKey)){
                mode = 2;
                if(switchModeSoundPlayer){
                    switchModeSound.at(unit.x, unit.y, 1, 0.3f);
                    Vars.ui.announce(Core.bundle.format("detainerclaw.mode2"), 1f);
                }
            }
        }
    }

    public void controlInit(Unit unit){
        armDistanceTarget = armLength * armDefaultScale;
        rotZ = unit.rotation;
        wristAngleTarget = wristAngleDefault;
    }

    private float holdTime, max = 60f, orbTimer;
    private boolean energyCharging, laserShooting;

    public void outputModeAttack(Unit unit){
        float limit = 65f;
        if(unit.isShooting() && aimControlling && (holdTime < limit || outputLaser)){
            energyCharging = true;
            holdTime += 1;
            if(outputLaser && holdTime > max){
                if(holdTime == max + 1){
                    laserShootSound.at(unit.x + x, unit.y + y);
                    Effect.shake(4f, 16, unit.x + x, unit.y + y);
                }
                laserShooting = true;
                orbTimer = 20f;
                RailBulletType laser = new RailBulletType(){{
                    length = 600f;
                    pierceEffect = hitEffect = new WrapEffect(Fx.hitLaserColor, clawLaserColor == null? unit.team.color : clawLaserColor);
                    damage = 10;
                    pierceDamageFactor = 0.5f;
                    pierceArmor = true;
                    despawnEffect = Fx.none;
                    pierceCap = 5;
                    Effect b = Fx.pointBeam;
                    b.lifetime = 2;
                    lineEffect = b;
                    pointEffect = new MultiEffect(new ParticleEffect(){{
                        particles = 1;
                        colorFrom = colorTo = clawLaserColor == null? unit.team.color : clawLaserColor;
                        layer = Layer.bullet;
                        sizeFrom = 6;
                        sizeTo = 0;
                        lifetime = 1;
                        length = baseLength = 0;
                    }},
                            new ParticleEffect(){{
                                particles = 1;
                                colorFrom = colorTo = clawLaserColor == null? unit.team.color : clawLaserColor;
                                layer = Layer.bullet - 0.01f;
                                sizeFrom = 5;
                                sizeTo = 0;
                                lifetime = 10;
                                length = 10;
                                baseLength = 0;
                            }}
                            );
                    pointEffectSpace = 1;
                }};
                laser.create(unit, unit.team, unit.x + x, unit.y + y, stereoMode? rotZ : Mathf.radiansToDegrees * Mathf.atan2(x, y));
                unit.flag -= 10;
                if(unit.flag < 0){
                    unit.flag = 0;
                }
                Effect.shake(1.5f, 4, unit.x + x, unit.y + y);

            }
        }else{
            energyCharging = false;
            if(holdTime > 0){
                if(!outputLaser){
                    float h = holdTime;
                    float d = (float) (Mathf.clamp(h / max) * (unit.flag + totalEnergy));
                    float r = Math.max(6f, 24f * Mathf.clamp((float) ((totalEnergy + unit.flag) / 4500)));
                    BasicBulletType orb = new BasicBulletType(){{
                        //sprite = "circle-bullet";
                        frontRegion = Core.atlas.find("circle-bullet");
                        backRegion = Core.atlas.find("circle-bullet-back");
                        damage = 0;
                        splashDamage = d;
                        splashDamageRadius = r * 3;
                        despawnShake = hitShake = r;
                        knockback = r / 2;
                        width = height = r * 1.25f;
                        speed = releaseSpeed;
                        trailLength = (int) (r / 4);
                        trailWidth = r / 8;
                        backColor = trailColor = clawLaserColor == null? unit.team.color: clawLaserColor;
                        frontColor = clawLaserCenterColor;
                        hittable = false;
                        lifetime = 600f;
                        hitEffect = despawnEffect = new ExplosionEffect(){{
                            waveRad = r * 3;
                            waveLife = 10f;
                            smokeRad = r * 2;
                            smokes = (int) (r * 2);
                            sparks = (int) r;
                            sparkRad = r * 2;
                            smokeColor = sparkColor = waveColor = clawLaserColor == null? unit.team.color: clawLaserColor;
                            lifetime = 60f;
                            smokeSize = r * 0.25f + 1f;
                            sparkLen = r * 0.5f;
                            sparkStroke = (int) (Math.sqrt(r) / 2);
                            waveStroke = 2;
                        }};
                        hitSound = despawnSound = Sounds.explosionAfflict;
                        hitSoundVolume = Mathf.clamp(r / 9);
                        shootEffect = Fx.shockwave;
                    }};
                    orbTimer = 20f;
                    orb.create(unit, unit.team, unit.x + x, unit.y + y, stereoMode? rotZ : Mathf.radiansToDegrees * Mathf.atan2(x, y));
                    unit.flag -= d;
                    if(unit.flag < 0){
                        unit.flag = 0;
                    }
                    energyOrbSound.at(unit.x + x, unit.y + y, 1, 1);
                    orb.shootEffect.at(unit.x + x, unit.y + y, stereoMode? rotZ : Mathf.radiansToDegrees * Mathf.atan2(x, y));
                    Effect.shake(r / 3, (float) (r * 1.5), unit.x + x, unit.y + y);
                }
                holdTime = 0;
            }
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

        if(activated){
            if(clawLaserColor == null){
                clawLaserColor = unit.team.color;
            }
            drawClawLaser(unit, clawLaserColor, clawLaserCenterColor);
        }
    }

    public void drawClawStereo(Unit unit, float rot){
        //basic pos
        float
                cx = unit.x + x,
                cy = unit.y + y,
                rx = unit.x + rootX * Mathf.cosDeg(unit.rotation - 90f) + rootY * Mathf.cosDeg(unit.rotation),
                ry = unit.y + rootX * Mathf.sinDeg(unit.rotation - 90f) + rootY * Mathf.sinDeg(unit.rotation);
        //claw pos
        float
                dxInner = (Mathf.cosDeg(rot) * clawLength / 2) * Mathf.cosDeg(rotZ - 90f) + (-0.5f * clawSpriteHeight + clawSpriteBeamOffset) * Mathf.cosDeg(rotZ - 180f),
                dyInner = (Mathf.cosDeg(rot) * clawLength / 2) * Mathf.sinDeg(rotZ - 90f) + (-0.5f * clawSpriteHeight + clawSpriteBeamOffset) * Mathf.sinDeg(rotZ - 180f),
                dxOuter = (-Mathf.cosDeg(rot) * (clawLength / 2 + clawSpriteThickness)) * Mathf.cosDeg(rotZ - 90f) + (-0.5f * clawSpriteHeight + clawSpriteBeamOffset) * Mathf.cosDeg(rotZ - 180f),
                dyOuter = (-Mathf.cosDeg(rot) * (clawLength / 2 + clawSpriteThickness)) * Mathf.sinDeg(rotZ - 90f) + (-0.5f * clawSpriteHeight + clawSpriteBeamOffset) * Mathf.sinDeg(rotZ - 180f),
                dxBackSide1 = (Mathf.cosDeg(rot) * (clawLength / 2 + 0.5f * clawSpriteThickness)) * Mathf.cosDeg(rotZ - 90f) + (-0.5f * clawSpriteHeight + clawSpriteBeamOffset) * Mathf.cosDeg(rotZ - 180f)
                        + (Mathf.sinDeg(rot) * 0.5f * clawSpriteWidth) * Mathf.cosDeg(rotZ - 90f),
                dyBackSide1 = (Mathf.cosDeg(rot) * (clawLength / 2 + 0.5f * clawSpriteThickness)) * Mathf.sinDeg(rotZ - 90f) + (-0.5f * clawSpriteHeight + clawSpriteBeamOffset) * Mathf.sinDeg(rotZ - 180f)
                        + (Mathf.sinDeg(rot) * 0.5f * clawSpriteWidth) * Mathf.sinDeg(rotZ - 90f),
                dxBackSide2 = (Mathf.cosDeg(rot) * (clawLength / 2 + 0.5f * clawSpriteThickness)) * Mathf.cosDeg(rotZ - 90f) + (-0.5f * clawSpriteHeight + clawSpriteBeamOffset) * Mathf.cosDeg(rotZ - 180f)
                        - (Mathf.sinDeg(rot) * 0.5f * clawSpriteWidth) * Mathf.cosDeg(rotZ - 90f),
                dyBackSide2 = (Mathf.cosDeg(rot) * (clawLength / 2 + 0.5f * clawSpriteThickness)) * Mathf.sinDeg(rotZ - 90f) + (-0.5f * clawSpriteHeight + clawSpriteBeamOffset) * Mathf.sinDeg(rotZ - 180f)
                        - (Mathf.sinDeg(rot) * 0.5f * clawSpriteWidth) * Mathf.sinDeg(rotZ - 90f),
                dxFrontSide1 = (Mathf.cosDeg(rot) * (-clawLength / 2 - 0.5f * clawSpriteThickness)) * Mathf.cosDeg(rotZ - 90f) + (-0.5f * clawSpriteHeight + clawSpriteBeamOffset) * Mathf.cosDeg(rotZ - 180f)
                        + (Mathf.sinDeg(rot) * 0.5f * clawSpriteWidth) * Mathf.cosDeg(rotZ - 90f),
                dyFrontSide1 = (Mathf.cosDeg(rot) * (-clawLength / 2 - 0.5f * clawSpriteThickness)) * Mathf.sinDeg(rotZ - 90f) + (-0.5f * clawSpriteHeight + clawSpriteBeamOffset) * Mathf.sinDeg(rotZ - 180f)
                        + (Mathf.sinDeg(rot) * 0.5f * clawSpriteWidth) * Mathf.sinDeg(rotZ - 90f),
                dxFrontSide2 = (Mathf.cosDeg(rot) * (-clawLength / 2 - 0.5f * clawSpriteThickness)) * Mathf.cosDeg(rotZ - 90f) + (-0.5f * clawSpriteHeight + clawSpriteBeamOffset) * Mathf.cosDeg(rotZ - 180f)
                        - (Mathf.sinDeg(rot) * 0.5f * clawSpriteWidth) * Mathf.cosDeg(rotZ - 90f),
                dyFrontSide2 = (Mathf.cosDeg(rot) * (-clawLength / 2 - 0.5f * clawSpriteThickness)) * Mathf.sinDeg(rotZ - 90f) + (-0.5f * clawSpriteHeight + clawSpriteBeamOffset) * Mathf.sinDeg(rotZ - 180f)
                        - (Mathf.sinDeg(rot) * 0.5f * clawSpriteWidth) * Mathf.sinDeg(rotZ - 90f),
                dxBottomSide = (-Mathf.cosDeg(rot) * (clawLength / 2 + clawSpriteThickness)) * Mathf.cosDeg(rotZ - 90f) + (clawSpriteBeamOffset + 0.5f * clawSpriteThickness) * Mathf.cosDeg(rotZ - 180f),
                dyBottomSide = (-Mathf.cosDeg(rot) * (clawLength / 2 + clawSpriteThickness)) * Mathf.sinDeg(rotZ - 90f) + (clawSpriteBeamOffset + 0.5f * clawSpriteThickness) * Mathf.sinDeg(rotZ - 180f),
                dxBottomMain1 = (clawSpriteBeamOffset + 0.5f * clawSpriteThickness) * Mathf.cosDeg(rotZ - 180f)
                        + (Mathf.sinDeg(rot) * 0.5f * clawSpriteWidth) * Mathf.cosDeg(rotZ - 90f),
                dyBottomMain1 = (clawSpriteBeamOffset + 0.5f * clawSpriteThickness) * Mathf.sinDeg(rotZ - 180f)
                        + (Mathf.sinDeg(rot) * 0.5f * clawSpriteWidth) * Mathf.sinDeg(rotZ - 90f),
                dxBottomMain2 = (clawSpriteBeamOffset + 0.5f * clawSpriteThickness) * Mathf.cosDeg(rotZ - 180f)
                        - (Mathf.sinDeg(rot) * 0.5f * clawSpriteWidth) * Mathf.cosDeg(rotZ - 90f),
                dyBottomMain2 = (clawSpriteBeamOffset + 0.5f * clawSpriteThickness) * Mathf.sinDeg(rotZ - 180f)
                        - (Mathf.sinDeg(rot) * 0.5f * clawSpriteWidth) * Mathf.sinDeg(rotZ - 90f);
        //arm pos
        float
                dxWrist = (clawSpriteBeamOffset + clawSpriteThickness + 0.5f * (wristLength - clawSpriteBeamOffset - clawSpriteThickness)) * Mathf.cosDeg(rotZ - 180f),
                dyWrist = (clawSpriteBeamOffset + clawSpriteThickness + 0.5f * (wristLength - clawSpriteBeamOffset - clawSpriteThickness)) * Mathf.sinDeg(rotZ - 180f),
                dxWristJoint = (wristLength) * Mathf.cosDeg(rotZ - 180f),
                dyWristJoint = (wristLength) * Mathf.sinDeg(rotZ - 180f),
                armAngle = (float) (Mathf.radiansToDegrees * Math.asin(Math.min(1, armNodeOffset / (armLength / 2)))),
                dxArmJoint = dxWristJoint + (armDistance / 2) * Mathf.cosDeg(rotZ + wristAngle) + ((armSide == 0? 1 : -1) * armNodeOffset) * Mathf.cosDeg(rotZ + wristAngle + 90f),
                dyArmJoint = dyWristJoint + (armDistance / 2) * Mathf.sinDeg(rotZ + wristAngle) + ((armSide == 0? 1 : -1) * armNodeOffset) * Mathf.sinDeg(rotZ + wristAngle + 90f),
                dxArmBase = (dxArmJoint + dxWristJoint) / 2,
                dyArmBase = (dyArmJoint + dyWristJoint) / 2,
                dxArm = (dxArmJoint + cx + rx) / 2 - cx,
                dyArm = (dyArmJoint + cy + ry) / 2 - cy;

        Draw.color(Color.white, 1f);
        //claw inner
        Draw.z(Layer.flyingUnitLow - 4f);
        drawRectShadow(clawSideInnerRegion, cx + dxInner, cy + dyInner,
                clawSpriteWidth * Mathf.sinDeg(rot), clawSpriteHeight, rotZ - 90f, unit);
        //claw back sides
        Draw.z(Layer.flyingUnitLow - 4.5f);
        drawRectShadow(clawSideSideRegion, cx + dxBackSide1, cy + dyBackSide1,
                clawSpriteThickness * Mathf.clamp(Mathf.cosDeg(rot)), clawSpriteHeight, rotZ - 90f, unit);
        drawRectShadow(clawSideSideRegion, cx + dxBackSide2, cy + dyBackSide2,
                clawSpriteThickness * Mathf.clamp(-Mathf.cosDeg(rot)), clawSpriteHeight, rotZ - 90f, unit);
        //claw outer
        Draw.z(Layer.flyingUnit - 0.1f);
        drawRectShadow(clawSideOuterRegion, cx + dxOuter, cy + dyOuter,
                clawSpriteWidth * Mathf.sinDeg(rot), clawSpriteHeight, rotZ - 90f, unit);
        //claw front sides
        Draw.z(Layer.flyingUnit - 0.2f);
        drawRectShadow(clawSideSideRegion, cx + dxFrontSide1, cy + dyFrontSide1,
                clawSpriteThickness * Mathf.clamp(Mathf.cosDeg(rot)), clawSpriteHeight, rotZ - 90f, unit);
        drawRectShadow(clawSideSideRegion, cx + dxFrontSide2, cy + dyFrontSide2,
                clawSpriteThickness * Mathf.clamp(-Mathf.cosDeg(rot)), clawSpriteHeight, rotZ - 90f, unit);
        //claw bottom side
        Draw.z(Layer.flyingUnit - 0.3f);
        drawRectShadow(clawBottomSideRegion, cx + dxBottomSide, cy + dyBottomSide,
                clawSpriteWidth * Mathf.sinDeg(rot), clawSpriteThickness, rotZ - 90f, unit);
        //claw bottom main
        Draw.z(Layer.flyingUnit - 0.4f);
        drawRectShadow(clawBottomMainRegion, cx + dxBottomMain1, cy + dyBottomMain1,
                (clawLength + 2 * clawSpriteThickness) * Mathf.clamp(Mathf.cosDeg(rot)), clawSpriteThickness, rotZ - 90f, unit);
        drawRectShadow(clawBottomMainRegion, cx + dxBottomMain2, cy + dyBottomMain2,
                (clawLength + 2 * clawSpriteThickness) * Mathf.clamp(-Mathf.cosDeg(rot)), clawSpriteThickness, rotZ - 90f, unit);

            //wrist
            Draw.z(Layer.flyingUnitLow - 0.3f);
        drawRectShadow(wristRegion, cx + dxWrist, cy + dyWrist,
                    wristLength - clawSpriteBeamOffset - clawSpriteThickness, wristSpriteWidth, rotZ, unit);
            //wrist joint
            Draw.z(Layer.flyingUnitLow - 0.1f);
        drawRectShadow(wristJointRegion, cx + dxWristJoint, cy + dyWristJoint,
                    wristJointSpriteSize, wristJointSpriteSize, 0f, unit);


            //arm base
            Draw.z(Layer.flyingUnitLow - 0.2f);
        drawRectShadow(armBaseRegion, cx + dxArmBase, cy + dyArmBase,
                armLength / 2, armBaseSpriteWidth, Mathf.radiansToDegrees * Mathf.atan2(dxWristJoint - dxArmJoint, dyWristJoint - dyArmJoint), unit);

            //arm joint
            Draw.z(Layer.flyingUnitLow);
        drawRectShadow(armJointRegion, cx + dxArmJoint, cy + dyArmJoint,
                    armJointSpriteSize, armJointSpriteSize, 0f, unit);

            //arm
            Draw.z(Layer.flyingUnitLow - 0.1f);
        drawRectShadow(armRegion, cx + dxArm, cy + dyArm,
                getDistance(cx + dxArmJoint,cy + dyArmJoint, rx, ry), armBaseSpriteWidth, Mathf.radiansToDegrees * Mathf.atan2(dxArmJoint + cx - rx, dyArmJoint + cy - ry), unit);

            //root
            Draw.z(Layer.flyingUnit + 1f);
        drawRectShadow(rootRegion, rx, ry,
                    rootSpriteSize, rootSpriteSize, unit.rotation - 90f, unit);

        if(activated){
            if(clawLaserColor == null){
                clawLaserColor = unit.team.color;
            }
            drawClawLaser(unit, clawLaserColor, clawLaserCenterColor);
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

    public void drawRectShadow(String region, float x, float y, float w, float h ,float r, Unit unit){
        Draw.rect(region, x, y, w, h, r);

        if(clawShadowElevation > 0) {

            float normZ = Draw.z();
            Color normColor = Draw.getColor();

            float e = clawShadowElevation;
            float sx = x + shadowTX * e, sy = y + shadowTY * e;
            Floor floor = world.floorWorld(sx, sy);

            float dest = floor.canShadow ? 1f : 0f;
            unit.shadowAlpha = unit.shadowAlpha < 0 ? dest : Mathf.approachDelta(unit.shadowAlpha, dest, 0.11f);
            Draw.color(Pal.shadow, Pal.shadow.a * unit.shadowAlpha);

            Draw.z(Math.min(Layer.darkness, Layer.flyingUnitLow - 1f));
            Draw.rect(region, sx, sy, w, h, r);
            Draw.color();

            Draw.z(normZ);
            Draw.color(normColor);

        }
    }

    public void drawClawLaser(Unit unit, Color color1, Color color2){
        Draw.z(99.99f);
        Draw.color(color1);
        Lines.stroke(clawWidth);
        if(!stereoMode) {
            float lx = x + unit.x;
            float ly = y + unit.y;
            float lr = 0.5f * clawLength - 0.5f * clawWidth;
            Lines.line(lx - lr * Mathf.cosDeg(rot), ly - lr * Mathf.sinDeg(rot),
                    lx + lr * Mathf.cosDeg(rot), ly + lr * Mathf.sinDeg(rot));
            Drawf.light(lx - lr * Mathf.cosDeg(rot), ly - lr * Mathf.sinDeg(rot),
                    lx + lr * Mathf.cosDeg(rot), ly + lr * Mathf.sinDeg(rot),
                    clawWidth, color1, 0.5f);
        }else{
            float lx = x + unit.x;
            float ly = y + unit.y;
            float lr = 0.5f * (clawLength - clawWidth) * Mathf.cosDeg(rot);
            Lines.line(lx - lr * Mathf.cosDeg(rotZ - 90f), ly - lr * Mathf.sinDeg(rotZ - 90f),
                    lx + lr * Mathf.cosDeg(rotZ - 90f), ly + lr * Mathf.sinDeg(rotZ - 90f));
            Drawf.light(lx - lr * Mathf.cosDeg(rotZ - 90f), ly - lr * Mathf.sinDeg(rotZ - 90f),
                    lx + lr * Mathf.cosDeg(rotZ - 90f), ly + lr * Mathf.sinDeg(rotZ - 90f),
                    clawWidth, color1, 0.5f);
        }
        Draw.z(110f);
        Draw.color(color2);
        Lines.stroke(clawWidth * 0.3f);
        if(!stereoMode) {
            float lx = x + unit.x;
            float ly = y + unit.y;
            float lr = 0.5f * clawLength - 0.5f * clawWidth * 0.3f;
            Lines.line(lx - lr * Mathf.cosDeg(rot), ly - lr * Mathf.sinDeg(rot),
                    lx + lr * Mathf.cosDeg(rot), ly + lr * Mathf.sinDeg(rot));
            Drawf.light(lx - lr * Mathf.cosDeg(rot), ly - lr * Mathf.sinDeg(rot),
                    lx + lr * Mathf.cosDeg(rot), ly + lr * Mathf.sinDeg(rot),
                    clawWidth, color2, 0.5f);
        }else{
            float lx = x + unit.x;
            float ly = y + unit.y;
            float lr = 0.5f * (clawLength - clawWidth * 0.3f) * Mathf.cosDeg(rot);
            Lines.line(lx - lr * Mathf.cosDeg(rotZ - 90f), ly - lr * Mathf.sinDeg(rotZ - 90f),
                    lx + lr * Mathf.cosDeg(rotZ - 90f), ly + lr * Mathf.sinDeg(rotZ - 90f));
            Drawf.light(lx - lr * Mathf.cosDeg(rotZ - 90f), ly - lr * Mathf.sinDeg(rotZ - 90f),
                    lx + lr * Mathf.cosDeg(rotZ - 90f), ly + lr * Mathf.sinDeg(rotZ - 90f),
                    clawWidth * 0.3f, color2, 0.25f);
        }

        Draw.alpha(1.0f);
        if(mode != 2 && totalEnergy + unit.flag > 0){
            drawClawEnergy(unit);
        }
    }

    public void drawClawEnergy(Unit unit){
        float e = (float) (totalEnergy + unit.flag);
        Draw.z(99.99f);
        Draw.color(clawLaserColor);
        float r = Math.max(6f, 24f * Mathf.clamp((float) ((totalEnergy + unit.flag) / 4500)));
        Fill.circle(unit.x + x, unit.y + y, r * 0.75f);
        Draw.z(101f);
        Draw.color(clawLaserCenterColor);
        Fill.circle(unit.x + x, unit.y + y, r * 0.5f);
        Drawf.light(unit.x + x, unit.y + y, r * 2, clawLaserColor, 1f);
        if(Mathf.chanceDelta(r / 48f)){
            energyEffect.at(unit.x + x + Mathf.range(absorbRadius / 2), unit.y + y + Mathf.range(absorbRadius / 2));
        }

        if(energyCharging){
            float maxr = absorbRadius;
            float curr = Mathf.clamp(holdTime / max) * maxr;
            Draw.z(Layer.flyingUnitLow - 1f);
            Draw.color(clawLaserColor);
            Draw.alpha(0.5f);
            Lines.circle(unit.x + x, unit.y + y, maxr);
            Fill.circle(unit.x + x, unit.y + y, curr);
        }
    }








    private static final Cons<Bullet> clawConsumer = b -> {
        if(paramField.orbTimer <= 0 && paramField.activated && paramField.mode != 2 && b.within(paramUnit.x + paramField.x, paramUnit.y + paramField.y, paramField.absorbRadius) && b.type.absorbable
                && (b.team != paramUnit.team || ((paramField.absorbSameTeam || b.owner == paramUnit) && !paramField.delayedShooting(paramUnit)))
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
            if(paramField.mode == 1){
                paramField.totalEnergy += b.type.damage;
                paramField.totalEnergy += b.type.splashDamage;
                paramField.absorbSound.at(b.x, b.y, 2, 0.6f);
                b.remove();
            }
            paramField.totalDamage += b.type.damage;
            paramField.totalDamage += b.type.splashDamage;
            paramField.hasBullet = true;
        }
        if(paramField.orbTimer <= 0 && paramField.activated && b.within(paramUnit.x + paramField.x, paramUnit.y + paramField.y, paramField.absorbRadius / 2f)
                && (b.team == paramUnit.team && b.owner == paramUnit && (paramField.delayedShooting(paramUnit) || paramField.mode == 2))) {

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
        if(paramField.activated && paramField.mode != 2 && m.within(paramUnit.x + paramField.x, paramUnit.y + paramField.y, paramField.absorbRadius)
                && (m.team != paramUnit.team || ((paramField.absorbSameTeam || m.flag == 1f) && !paramField.delayedShooting(paramUnit)))
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
            if(paramField.mode == 1){
                paramField.totalDamage += m.type.estimateDps();
                paramField.absorbSound.at(m.x, m.y, 2, 0.6f);
                m.remove();
            }
            paramField.totalDamage += m.type.estimateDps();
            paramField.hasBullet = true;

        }
        if(paramField.activated && m.within(paramUnit.x + paramField.x, paramUnit.y + paramField.y, paramField.absorbRadius)
                && m.team == paramUnit.team && m.isMissile() && (paramField.delayedShooting(paramUnit) || paramField.mode == 2) && m.flag == 1f) {

            if(m instanceof TimedKillc tk){
                tk.time(0f);
            }
            m.flag = 2f;
            m.apply(StatusEffects.fast, 600f);
            m.rotation = paramField.stereoMode? paramField.rotZ : Mathf.radiansToDegrees * Mathf.atan2(paramField.x, paramField.y);
            paramField.releaseSoundPlay = true;
        }

    };

    private static final Cons<Unit> unitConsumer = u -> {
        if(((paramField.activated && paramField.mode == 2)
                && (!paramField.canDropUnit(u, paramField.x + paramUnit.x, paramField.y + paramUnit.y) || (paramUnit.isShooting()) && paramField.mode == 2))
                && !u.type.hidden && u.type.hitSize <= paramField.clawLength
                && !u.spawnedByCore() && u.type != paramUnit.type
                 && paramField.aimControlling && u.within(paramUnit.x + paramField.x, paramUnit.y + paramField.y, paramField.absorbRadius * 0.75f)){
            if(paramField.canDropUnit(u, paramField.x + paramUnit.x, paramField.y + paramUnit.y)) {
                u.x = paramUnit.x + paramField.x;
                u.y = paramUnit.y + paramField.y;
                u.vel.x = paramField.dvx;
                u.vel.y = paramField.dvy;
                if(paramField.stereoMode){
                    u.rotation(paramField.rotZ);
                }else{
                    u.rotation(Mathf.radiansToDegrees * Mathf.atan2(paramField.x, paramField.y));
                }
            }else{
                u.apply(StatusEffects.unmoving, 2f);
            }
            paramField.hasBullet = true;
            paramField.totalDamage += u.type.hitSize * 5;

        }
    };

    private float delayShootingTimer;

    public boolean delayedShooting(Unit unit) {
        if(unit.isShooting()){
            delayShootingTimer = 15f;
            return true;
        }else{
            if(delayShootingTimer > 0){
                return true;
            }
        }
        return false;
    }

    public boolean canDropUnit(Unit u, float x, float y) {
        Floor floor = world.floorWorld(x, y);
        Tile tile = world.tileWorld(x, y);
        Building build = world.buildWorld(x, y);
        if(u.type.flying || u.type.canBoost) {
            return true;
        }else if(u.type.naval){
            if(floor != null){
                if(floor.isLiquid){
                    if(tile != null){
                        if(!tile.block().solid){
                            if(build != null){
                                return !build.block.solid;
                            }else return true;
                        }else return false;
                    }else{
                        if(build != null){
                            return !build.block.solid;
                        }else return true;
                    }
                }else return false;
            }else return false;
        }else{
            if((u instanceof LegsUnit && u.type.allowLegStep) || (u instanceof CrawlUnit)){
                if(floor != null){
                    if(floor.placeableOn && !(floor instanceof EmptyFloor)){
                        if(tile != null){
                            return !tile.block().solid;
                        }else return true;
                    }else return false;
                }else return false;
            }else{
                if(floor != null){
                    if(floor.placeableOn && !(floor instanceof EmptyFloor)){
                        if(tile != null){
                            if(!tile.block().solid){
                                if(build != null){
                                    return !build.block.solid;
                                }else return true;
                            }else return false;
                        }else{
                            if(build != null){
                                return !build.block.solid;
                            }else return true;
                        }
                    }else return false;
                }else return false;
            }
        }
    }
}
