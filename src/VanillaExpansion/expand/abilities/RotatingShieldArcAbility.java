package VanillaExpansion.expand.abilities;

import arc.Core;
import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.abilities.Ability;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;

import java.util.concurrent.atomic.AtomicReference;

import static mindustry.Vars.tilesize;

public class RotatingShieldArcAbility extends Ability {

    private static Unit paramUnit;
    private static RotatingShieldArcAbility paramField;
    private static Vec2 paramPos = new Vec2();
    private static final Cons<Bullet> shieldConsumer = b -> {
        float rotateOffset = paramUnit.rotation();
        if(paramField.rotateSpeed != 0f) {
            rotateOffset = (Time.time * paramField.rotateSpeed) % 360;
        }
        if(b.team != paramUnit.team && b.type.absorbable && paramField.data > 0 &&
                !(b.within(paramPos, paramField.actualRadius - paramField.width) && paramPos.within(b.x - b.deltaX, b.y - b.deltaY, paramField.actualRadius - paramField.width)) &&
                (Tmp.v1.set(b).add(b.deltaX, b.deltaY).within(paramPos, paramField.actualRadius + paramField.width) || b.within(paramPos, paramField.actualRadius + paramField.width)) &&
                (Angles.within(paramPos.angleTo(b), rotateOffset + paramField.angleOffset, paramField.angle / 2f) || Angles.within(paramPos.angleTo(b.x + b.deltaX, b.y + b.deltaY), rotateOffset + paramField.angleOffset, paramField.angle / 2f))){

            if(paramField.chanceDeflect > 0f && b.vel.len() >= 0.1f && (b.type.reflectable || paramField.perfectDeflect) && Mathf.chance(paramField.chanceDeflect)){

                //make sound
                paramField.deflectSound.at(paramPos, Mathf.random(0.9f, 1.1f));

                //translate bullet back to where it was upon collision
                b.trns(-b.vel.x, -b.vel.y);

                float penX = Math.abs(paramPos.x - b.x), penY = Math.abs(paramPos.y - b.y);

                if(penX > penY){
                    b.vel.x *= -1;
                    b.vel.y *= paramField.reflectVel;
                }else{
                    b.vel.y *= -1;
                    b.vel.x *= paramField.reflectVel;
                }
                if(!b.type.killShooter) {
                    b.owner = paramUnit;
                }
                b.team = paramUnit.team;
                b.time = b.lifetime * paramField.reflectTime;
                if(paramField.reflectBuildingDamage > 0f){
                    b.buildingDamageMultiplier = paramField.reflectBuildingDamage;
                }

            }else{
                b.absorb();
                Fx.absorb.at(b);

                paramField.hitSound.at(b.x, b.y, 1f + Mathf.range(0.1f), paramField.hitSoundVolume);
            }

            // break shield
            if(paramField.data <= b.damage() || (paramField.pushSelfDamage > 0 && paramField.pushBreak)){
                paramField.pushBreak = false;

                if(!paramField.sameUnitBoost || paramField.sameUnitCountOut < 0) {
                    paramField.data -= paramField.cooldown * paramField.regen;
                }else{
                    paramField.data -= (paramField.cooldown / (paramField.sameUnitCountOut * paramField.cooldownBoostMultiplier + 1)) * (paramField.regen * ((paramField.sameUnitCountOut * paramField.regenBoostMultiplier) + 1));
                }

                Fx.arcShieldBreak.at(paramPos.x, paramPos.y, 0, paramField.color == null ? paramUnit.type.shieldColor(paramUnit) : paramField.color, paramUnit);

                paramField.breakSound.at(paramPos.x, paramPos.y);
            }

            // shieldDamage for consistency
            paramField.data -= b.type.shieldDamage(b);
            paramField.alpha = 1f;
        }
    };

    protected static final Cons<Unit> unitConsumer = unit -> {
        // ignore core units
        float rotateOffset = paramUnit.rotation();
        if(paramField.rotateSpeed != 0f) {
            rotateOffset = (Time.time * paramField.rotateSpeed) % 360;
        }
        if(paramField.data > 0 && unit.targetable(paramUnit.team) &&
                !(unit.within(paramPos, paramField.actualRadius - paramField.width) && paramPos.within(unit.x - unit.deltaX, unit.y - unit.deltaY, paramField.actualRadius - paramField.width)) &&
                (Tmp.v1.set(unit).add(unit.deltaX, unit.deltaY).within(paramPos, paramField.actualRadius + paramField.width) || unit.within(paramPos, paramField.actualRadius + paramField.width)) &&
                (Angles.within(paramPos.angleTo(unit), rotateOffset + paramField.angleOffset, paramField.angle / 2f) || Angles.within(paramPos.angleTo(unit.x + unit.deltaX, unit.y + unit.deltaY), rotateOffset + paramField.angleOffset, paramField.angle / 2f))){

            if(unit.isMissile() && paramField.missileUnitMultiplier >= 0f){
                Call.unitSafeDeath(unit);
                Fx.absorb.at(unit);
                paramField.pushEffect.at(unit.x, unit.y,paramUnit.team.color);

                // consider missile hp and gamerule to damage the shield
                paramField.data -= unit.health() * paramField.missileUnitMultiplier * Vars.state.rules.unitDamage(unit.team);
                paramField.alpha = 1f;

            }else if(paramField.pushUnits && !(!unit.isFlying() && paramUnit.isFlying())){

                float reach = paramField.actualRadius + paramField.width;
                float overlapDst = reach - unit.dst(paramPos.x, paramPos.y);

                if(overlapDst > 0){
                    //only nullify velocity if it's heading towards the shield
                    if(Angles.angleDist(unit.angleTo(paramPos), unit.vel.angle()) < 90f){
                        unit.vel.setZero();
                    }
                    // get out
                    unit.move(Tmp.v1.set(unit).sub(paramUnit).setLength(overlapDst + 0.01f));

                    if(Mathf.chanceDelta(0.3f * Time.delta)){
                        paramField.pushEffect.at(unit.x, unit.y, paramUnit.team.color);
                    }
                    if(paramField.pushDamage > 0){
                        if(paramField.pushDamagePierce) {
                            if(!paramUnit.moving()) {
                                unit.damagePierce(paramField.pushDamage);
                            }else{
                                unit.damagePierce(paramField.pushDamage * (1 - paramField.pushDamageMoveWeakMultiplier));
                            }
                        }else{
                            if(!paramUnit.moving()) {
                                unit.damage(paramField.pushDamage);
                            }else{
                                unit.damage(paramField.pushDamage * (1 - paramField.pushDamageMoveWeakMultiplier));
                            }
                        }
                    }
                    if(paramField.pushSelfDamage > 0) {
                        if (paramField.data >= paramField.pushSelfDamage) {
                            paramField.data -= paramField.pushSelfDamage;
                        } else {
                            paramField.data = 0;
                            paramField.pushBreak = true;
                        }
                        paramField.alpha = 1f;
                    }
                }
            }
        }
    };

    /** Shield radius. */
    public float radius = 60f;
    /** Shield regen speed in damage/tick. */
    public float regen = 0.1f;
    /** Maximum shield. */
    public float max = 200f;
    /** Cooldown after the shield is broken, in ticks. */
    public float cooldown = 60f * 5;
    /** Angle of shield arc. */
    public float angle = 80f;
    /** Offset parameters for shield. */
    public float angleOffset = 0f, x = 0f, y = 0f;
    /** If true, only activates when shooting. */
    public boolean whenShooting = true;
    /** Width of shield line. */
    public float width = 6f;
    /** Bullet deflection chance. -1 to disable */
    public float chanceDeflect = -1f;
    /** Multiplier for reflected bullet building damage. -1 to disable */
    public float reflectBuildingDamage = 1f;
    /** Velocity multiplier for reflected bullets on the opposite axis. Negative values = concave, positive values = convex */
    public float reflectVel = 3f;
    /** Time multiplier for reflected bullets. */
    public float reflectTime = 1f - 0.5f;
    /** Deflection sound. */
    public Sound deflectSound = Sounds.none;
    public Sound breakSound = Sounds.shieldBreakSmall;
    public Sound hitSound = Sounds.shieldHit;
    public float hitSoundVolume = 0.12f;
    /** Multiplier for shield damage taken from missile units. */
    public float missileUnitMultiplier = 2f;

    public float rotateSpeed = 2f;
    public boolean showBars = true;
    public int shieldCount = 1;
    public boolean perfectDeflect = false;

    public boolean radiusChange = false;
    public float maxRadius = 180f;
    public float detectRadius = 180f;
    public float extraDetectRadius = 30f;
    public boolean detectAir = true;
    public boolean detectGround = true;
    public boolean detectSame = false;
    public boolean angleRelated = true;

    public boolean sameUnitBoost = false;
    public float regenBoostMultiplier = 0.75f;
    public float maxBoostMultiplier = 1f;
    public float cooldownBoostMultiplier = 0.5f;
    public int maxSameUnit = 4;
    public float sameUnitRadius = 180f;

    public float pushDamage = 0f;
    public boolean pushDamagePierce = false;
    public float pushDamageMoveWeakMultiplier = 0.9f;
    public float pushSelfDamage = 0f;

    /** Whether to draw the arc line. */
    public boolean drawArc = true;
    /** If not null, will be drawn on top. */
    public @Nullable String region;
    /** Color override of the shield. Uses unit shield colour by default. */
    public @Nullable Color color;
    /** If true, sprite position will be influenced by x/y. */
    public boolean offsetRegion = false;
    /** If true, enemy units are pushed out. */
    public boolean pushUnits = true;
    public Effect pushEffect = Fx.circleColorSpark;

    /** State. */
    protected float widthScale, alpha, actualRadius;
    protected int sameUnitCountOut;
    protected boolean pushBreak;



    @Override
    public void addStats(Table t){
        super.addStats(t);
        t.add(abilityStat("shield", Strings.autoFixed(max, 2)));
        t.row();
        t.add(abilityStat("repairspeed", Strings.autoFixed(regen * 60f, 2)));
        t.row();
        t.add(abilityStat("cooldown", Strings.autoFixed(cooldown / 60f, 2)));
        if(chanceDeflect > 0f){
            t.row();
            t.add(abilityStat("deflectchance", Strings.autoFixed(chanceDeflect *100f, 2)));
        }
        if(rotateSpeed != 0f){
            t.row();
            t.add(abilityStat("shieldrotatespeed", Math.abs(rotateSpeed) * 60f));
        }
        if(shieldCount > 1) {
            t.row();
            t.add(abilityStat("shieldcount", shieldCount));
        }
        if(perfectDeflect && chanceDeflect > 0){
            t.row();
            t.add(abilityStat("shieldperfectdeflect"));
        }
        if(pushDamage > 0f){
            t.row();
            t.add(abilityStat("shieldpushdamage", pushDamage));
            if(pushSelfDamage > 0f) {
                t.row();
                t.add(abilityStat("shieldpushselfdamage", pushSelfDamage));
            }
            if(pushDamageMoveWeakMultiplier > 0f) {
                t.row();
                t.add(abilityStat("shieldpushdamageweak", 100f - pushDamageMoveWeakMultiplier * 100f));
            }
        }
        if(radiusChange){
            t.row();
            t.add(abilityStat("shieldprotect"));
            t.row();
            t.add(abilityStat("shieldprotectradius", detectRadius / tilesize));
        }
        if(sameUnitBoost){
            t.row();
            t.add(abilityStat("shieldsameunitboost"));
            t.row();
            t.add(abilityStat("shieldsameunitradius", sameUnitRadius / tilesize));
            t.row();
            t.add(abilityStat("shieldsameunitboostmaxcount", maxSameUnit));
            t.row();
            t.add(abilityStat("shieldsameunitboostmax", maxBoostMultiplier * 100));
            t.row();
            t.add(abilityStat("shieldsameunitboostregen", regenBoostMultiplier * 100));
            t.row();
            t.add(abilityStat("shieldsameunitboostcooldown", cooldownBoostMultiplier * 100));
        }
    }

    public float getDistance(float ax, float ay, float bx, float by){
        return Mathf.sqrt(Math.abs((ax - bx) * (ax - bx) + (ay - by) * (ay - by)));
    }
    /*public float getAngleDiff(float a, float b){
        float r = a - b;
        boolean y = false;
        for(int i = 0; !y; i++){
            if(r < 0){
                r = r + 360f;
            }else if(r >= 360f){
                r = r - 360f;
            }else{
                y = true;
            }
        }
        return Math.abs(r - 180f);
    }*/

    @Override
    public void update(Unit unit){


        AtomicReference<Float> distanceTest = new AtomicReference<>(0f);
        AtomicReference<Float> distanceTestTotal = new AtomicReference<>(0f);
        float additionalRadius = 0f;
        final int[] sameUnitCount = {0};

        distanceTest.set(0f);
        Units.nearby(unit.team, unit.x, unit.y, detectRadius + extraDetectRadius, other -> {
            if((other != unit) && ((other.type != unit.type) || detectSame) && !other.isMissile() && ((other.isFlying() && detectAir) || (!other.isFlying() && detectGround))){
                distanceTest.set(12f + other.hitSize * 1.25f + getDistance(other.x, other.y, unit.x, unit.y));
                if(getDistance(other.x, other.y, unit.x, unit.y) > detectRadius) {
                    distanceTest.set(12f + other.hitSize * 1.25f + detectRadius);
                }
                if(angleRelated){
                    float shieldRot = unit.rotation + angleOffset;
                    if(rotateSpeed != 0f) {
                        shieldRot = (Time.time * rotateSpeed) % 360 + angleOffset;
                    }
                    float otherAngle = Mathf.radiansToDegrees * Mathf.atan2(other.x - unit.x, other.y - unit.y);
                    float angleWeight = Mathf.sqrt(Mathf.clamp((180f - Angles.angleDist(otherAngle, shieldRot)) / 180f));
                    //Log.info(other.type.localizedName + " " + otherAngle + " " + angleWeight);
                    distanceTest.set(angleWeight * distanceTest.get());
                }
                if(distanceTest.get() > distanceTestTotal.get()){
                    distanceTestTotal.set(distanceTest.get());
                }
            }
        });
        Units.nearby(unit.team, unit.x, unit.y, sameUnitRadius, other -> {
            if(other.type == unit.type && other != unit){
                sameUnitCount[0] = sameUnitCount[0] + 1;
            }
        });


        if(distanceTestTotal.get() - radius > 0){
            additionalRadius = distanceTestTotal.get() - radius;
        }
        float sameUnitBoostMultiplier = 0f;
        if(sameUnitCount[0] > 0) {
            if(sameUnitCount[0] < maxSameUnit) {
                sameUnitBoostMultiplier = sameUnitCount[0];
                sameUnitCountOut = sameUnitCount[0];
            }else{
                sameUnitBoostMultiplier = maxSameUnit;
                sameUnitCountOut = maxSameUnit;
            }
        }else{
            sameUnitCountOut = 0;
        }

        if(radiusChange) {
            actualRadius = Mathf.approachDelta(actualRadius, radius + additionalRadius, 0.05f + 0.2f * Math.abs(actualRadius - radius - additionalRadius));
        }else{
            actualRadius = radius;
        }

        if(!sameUnitBoost) {
            if (data < max) {
                data += Time.delta * regen;
            }
            if (data > max) {
                data = max;
            }
        }else{
            if (data < max * (sameUnitBoostMultiplier * maxBoostMultiplier + 1)) {
                data += Time.delta * regen * (sameUnitBoostMultiplier * regenBoostMultiplier + 1);
            }
            if (data > max * (sameUnitBoostMultiplier * maxBoostMultiplier + 1)) {
                data = max * (sameUnitBoostMultiplier * maxBoostMultiplier + 1);
            }
        }

        boolean active = data > 0 && (unit.isShooting || !whenShooting);
        alpha = Math.max(alpha - Time.delta/10f, 0f);

        if(active){
            widthScale = Mathf.lerpDelta(widthScale, 1f, 0.06f);
            paramUnit = unit;
            paramField = this;
            paramPos.set(x, y).rotate(unit.rotation - 90f).add(unit);

            float reach = actualRadius + width;
            Groups.bullet.intersect(paramPos.x - reach, paramPos.y - reach, reach * 2f, reach * 2f, shieldConsumer);
            Units.nearbyEnemies(paramUnit.team, paramPos.x - reach, paramPos.y - reach, reach * 2f, reach * 2f, unitConsumer);
        }else{
            widthScale = Mathf.lerpDelta(widthScale, 0f, 0.11f);
        }
    }

    @Override
    public void created(Unit unit){
        pushBreak = false;
        data = max;
    }

    @Override
    public void draw(Unit unit){
        if(widthScale > 0.001f){
            Draw.z(Layer.shields);

            Draw.color(color == null ? unit.type.shieldColor(unit) : color, Color.white, Mathf.clamp(alpha));
            var pos = paramPos.set(x, y).rotate(unit.rotation - 90f).add(unit);

            if(!Vars.renderer.animateShields){
                Draw.alpha(0.4f);
            }

            if(region != null){
                Vec2 rp = offsetRegion ? pos : Tmp.v1.set(unit);
                Draw.yscl = widthScale;
                Draw.rect(region, rp.x, rp.y, unit.rotation - 90);
                Draw.yscl = 1f;
            }

            if(drawArc){
                float rotateOffset = (Time.time * rotateSpeed) % 360;
                if(rotateSpeed == 0) rotateOffset = unit.rotation();
                Lines.stroke(width * widthScale);
                Lines.arc(pos.x, pos.y, actualRadius, angle / 360f, rotateOffset + angleOffset - angle / 2f);
            }
            Draw.reset();
        }
    }

    @Override
    public void displayBars(Unit unit, Table bars) {
        if (showBars) {
            if(shieldCount <= 1) {
                bars.add(new Bar("stat.shieldhealth", Pal.accent, () -> data / max)).row();
            }
            if(sameUnitBoost){
                bars.add(new Bar(() -> Core.bundle.format("bar.sameunitcount",
                        sameUnitCountOut, maxSameUnit), () -> Pal.techBlue,
                        () -> (float) sameUnitCountOut / maxSameUnit)).row();
            }
        }
    }
}
