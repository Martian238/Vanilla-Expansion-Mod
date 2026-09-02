package VanillaExpansion.expand.world.block.defense;

import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.LAccess;
import mindustry.world.blocks.defense.turrets.PowerTurret;


public class ShieldArcPowerTurret extends PowerTurret {


    //THIS IS SB

    /** Shield radius. */
    public float shieldRadius = 60f;
    /** Shield regen speed in damage/tick. */
    public float shieldRegen = 0.1f;
    /** Maximum shield. */
    public float shieldMax = 200f;
    /** Cooldown after the shield is broken, in ticks. */
    public float shieldCooldown = 60f * 5;
    /** Angle of shield arc. */
    public float shieldAngle = 80f;
    /** Offset parameters for shield. */
    public float shieldAngleOffset = 0f, shieldX = 0f, shieldY = 0f;
    /** If true, only activates when shooting. */
    public boolean whenShooting = true;
    /** Width of shield line. */
    public float shieldWidth = 6f;
    /** Bullet deflection chance. -1 to disable */
    public float chanceDeflect = -1f;
    /** Multiplier for reflected bullet building damage. -1 to disable */
    public float reflectBuildingDamage = 1f;
    /** Velocity multiplier for reflected bullets on the opposite axis. Negative values = concave, positive values = convex */
    public float reflectVel = 1f;
    /** Time multiplier for reflected bullets. */
    public float reflectTime = 1f - 0.5f;
    /** Deflection sound. */
    public Sound deflectSound = Sounds.none;
    public Sound shieldBreakSound = Sounds.shieldBreakSmall;
    public Sound hitSound = Sounds.shieldHit;
    public float hitSoundVolume = 0.12f;
    /** Multiplier for shield damage taken from missile units. */
    public float missileUnitMultiplier = 2f;

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
    protected float widthScale, alpha;


    private float data;





    public ShieldArcPowerTurret(String name) {
        super(name);
    }







    public class ShieldArcPowerTurretBuild extends TurretBuild{

        @Override
        public float getAmmoFraction(){
            return power == null ? 0f : power.status;
        }

        @Override
        public double sense(LAccess sensor){
            return switch(sensor){
                case ammo -> power == null ? 0f : power.status;
                case ammoCapacity -> 1;
                case heat -> heatRequirement > 0 ? heat : Float.NaN;
                default -> super.sense(sensor);
            };
        }

        @Override
        public BulletType useAmmo(){
            //nothing used directly
            return shootType;
        }

        @Override
        public boolean hasAmmo(){
            //you can always rotate, but never shoot if there's no power
            return true;
        }

        @Override
        public BulletType peekAmmo(){
            return shootType;
        }


        @Override
        public boolean shouldAmbientSound(){
            return data > 0.01f && warmup() > 1f;
        }

        private static ShieldArcPowerTurretBuild paramUnit;
        private static ShieldArcPowerTurret paramField;
        private static Vec2 paramPos = new Vec2();
        private static final Cons<Bullet> shieldConsumer = b -> {
            if(b.team != paramUnit.team && b.type.absorbable && paramField.data > 0 &&
                    !(b.within(paramPos, paramField.shieldRadius - paramField.shieldWidth) && paramPos.within(b.x - b.deltaX, b.y - b.deltaY, paramField.shieldRadius - paramField.shieldWidth)) &&
                    (Tmp.v1.set(b).add(b.deltaX, b.deltaY).within(paramPos, paramField.shieldRadius + paramField.shieldWidth) || b.within(paramPos, paramField.shieldRadius + paramField.shieldWidth)) &&
                    (Angles.within(paramPos.angleTo(b), paramUnit.rotation + paramField.shieldAngleOffset, paramField.shieldAngle / 2f) || Angles.within(paramPos.angleTo(b.x + b.deltaX, b.y + b.deltaY), paramUnit.rotation + paramField.shieldAngleOffset, paramField.shieldAngle / 2f))){

                if(paramField.chanceDeflect > 0f && b.vel.len() >= 0.1f && b.type.reflectable && Mathf.chance(paramField.chanceDeflect)){

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

                    b.owner = paramUnit;
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
                if(paramField.data <= b.damage()){
                    paramField.data -= paramField.shieldCooldown * paramField.shieldRegen;

                    Fx.arcShieldBreak.at(paramPos.x, paramPos.y, 0, paramField.color == null ? paramUnit.team.color : paramField.color, paramUnit);

                    paramField.shieldBreakSound.at(paramPos.x, paramPos.y);
                }

                // shieldDamage for consistency
                paramField.data -= b.type.shieldDamage(b);
                paramField.alpha = 1f;
            }
        };

        protected static final Cons<Unit> unitConsumer = unit -> {
            // ignore core units
            if(paramField.data > 0 && unit.targetable(paramUnit.team) &&
                    !(unit.within(paramPos, paramField.shieldRadius - paramField.shieldWidth) && paramPos.within(unit.x - unit.deltaX, unit.y - unit.deltaY, paramField.shieldRadius - paramField.shieldWidth)) &&
                    (Tmp.v1.set(unit).add(unit.deltaX, unit.deltaY).within(paramPos, paramField.shieldRadius + paramField.shieldWidth) || unit.within(paramPos, paramField.shieldRadius + paramField.shieldWidth)) &&
                    (Angles.within(paramPos.angleTo(unit), paramUnit.rotation + paramField.shieldAngleOffset, paramField.shieldAngle / 2f) || Angles.within(paramPos.angleTo(unit.x + unit.deltaX, unit.y + unit.deltaY), paramUnit.rotation + paramField.shieldAngleOffset, paramField.shieldAngle / 2f))){

                if(unit.isMissile() && paramField.missileUnitMultiplier >= 0f){
                    Call.unitSafeDeath(unit);
                    Fx.absorb.at(unit);
                    paramField.pushEffect.at(unit.x, unit.y,paramUnit.team.color);

                    // consider missile hp and gamerule to damage the shield
                    paramField.data -= unit.health() * paramField.missileUnitMultiplier * Vars.state.rules.unitDamage(unit.team);
                    paramField.alpha = 1f;

                }else {
                    if(paramField.pushUnits) {
                        unit.isFlying();
                        float reach = paramField.shieldRadius + paramField.shieldWidth;
                        float overlapDst = reach - unit.dst(paramPos.x, paramPos.y);

                        if (overlapDst > 0) {
                            //only nullify velocity if it's heading towards the shield
                            if (Angles.angleDist(unit.angleTo(paramPos), unit.vel.angle()) < 90f) {
                                unit.vel.setZero();
                            }
                            // get out
                            unit.move(Tmp.v1.set(unit).sub(paramUnit).setLength(overlapDst + 0.01f));

                            if (Mathf.chanceDelta(0.3f * Time.delta)) {
                                paramField.pushEffect.at(unit.x, unit.y, paramUnit.team.color);
                            }
                        }
                    }
                }
            }
        };


        @Override
        public void updateTile(){

            if(data < shieldMax){
                data += Time.delta * shieldRegen;
            }

            boolean active = data > 0 && (warmup() > 0.01f || !whenShooting);
            alpha = Math.max(alpha - Time.delta/10f, 0f);

            if(active){
                widthScale = Mathf.lerpDelta(widthScale, 1f, 0.06f);
                paramUnit = this;
                paramField = (ShieldArcPowerTurret) this.block;
                paramPos.set(x, y).rotate(rotation - 90f).add(unit);

                float reach = shieldRadius + shieldWidth;
                Groups.bullet.intersect(paramPos.x - reach, paramPos.y - reach, reach * 2f, reach * 2f, shieldConsumer);
                Units.nearbyEnemies(paramUnit.team, paramPos.x - reach, paramPos.y - reach, reach * 2f, reach * 2f, unitConsumer);
            }else{
                widthScale = Mathf.lerpDelta(widthScale, 0f, 0.11f);
            }
        }


        @Override
        public void draw(){
            super.draw();
            drawShield();
        }


        public void drawShield(){
            if(widthScale > 0.001f){
                Draw.z(Layer.shields);

                Draw.color(color == null ? team.color : color, Color.white, Mathf.clamp(alpha));
                var pos = paramPos.set(x, y).rotate(rotation - 90f).add(unit);

                if(!Vars.renderer.animateShields){
                    Draw.alpha(0.4f);
                }

                if(region != null){
                    Vec2 rp = offsetRegion ? pos : Tmp.v1.set(unit);
                    Draw.yscl = widthScale;
                    Draw.rect(region, rp.x, rp.y, rotation - 90);
                    Draw.yscl = 1f;
                }

                if(drawArc){
                    Lines.stroke(shieldWidth * widthScale);
                    Lines.arc(pos.x, pos.y, shieldRadius, shieldAngle / 360f, rotation + shieldAngleOffset - shieldAngle / 2f);
                }
                Draw.reset();
            }
        }




    }

}
