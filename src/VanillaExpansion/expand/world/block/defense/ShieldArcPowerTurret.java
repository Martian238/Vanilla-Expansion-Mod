package VanillaExpansion.expand.world.block.defense;

import arc.Core;
import mindustry.entities.abilities.ShieldArcAbility;
import mindustry.world.blocks.defense.turrets.PowerTurret;
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
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;


public class ShieldArcPowerTurret extends PowerTurret {


    public float shieldData;
    public ShieldArcPowerTurret(String name) {
        super(name);
    }

    public class ShieldArcTurretBuild extends PowerTurretBuild {
        public float shieldData = shieldMax;
        public float shieldAlpha = 0f;
        public float widthScale = 0f;
        public final Vec2 shieldPos = new Vec2();

        @Override
        public void updateTile(){
            // 1. 先调用父类更新（处理射击等）
            super.updateTile();

            // 2. 更新护盾数据
            if(shieldData < shieldMax){
                shieldData += Time.delta * shieldRegen;
            }

            boolean active = shieldData > 0 && (isActive() || !whenShooting);
            shieldAlpha = Math.max(shieldAlpha - Time.delta / 10f, 0f);

            if(active){
                widthScale = Mathf.lerpDelta(widthScale, 1f, 0.06f);
                shieldPos.set(0, 0).rotate(rotation - 90f).add(this);

                // 拦截子弹和单位
                float reach = shieldRadius + shieldWidth;
                Groups.bullet.intersect(
                        shieldPos.x - reach, shieldPos.y - reach,
                        reach * 2f, reach * 2f,
                        b -> {
                            if(b.team != team && b.type.absorbable && shieldData > 0){
                                // ... 你的拦截逻辑
                                b.absorb();
                                Fx.absorb.at(b);
                                shieldData -= b.damage();
                            }
                        }
                );
            }else{
                widthScale = Mathf.lerpDelta(widthScale, 0f, 0.11f);
            }

            // 护盾破碎恢复
            if(shieldData < 0) shieldData = 0;
        }

        @Override
        public void draw(){
            // 1. 先绘制炮台本身
            super.draw();

            // 2. 绘制护盾
            if(widthScale > 0.001f){
                Draw.z(Layer.shields);
                Draw.color(shieldColor == null ? team.color : shieldColor);
                Draw.alpha(Mathf.clamp(shieldAlpha));

                if(drawArc){
                    Lines.stroke(shieldWidth * widthScale);
                    Lines.arc(
                            x + shieldPos.x, y + shieldPos.y,
                            shieldRadius,
                            shieldAngle / 360f,
                            rotation + shieldAngleOffset - shieldAngle / 2f
                    );
                }
                Draw.reset();
            }
        }

        @Override
        public void displayBars(Table table){
            super.displayBars(table);
            table.add(new Bar(
                    () -> Core.bundle.format("stat.shieldhealth", (int)shieldData, (int)shieldMax),
                    () -> Pal.accent,
                    () -> shieldData / shieldMax
            )).row();
        }
    }

    // ========== 让炮台使用自定义 Build 类 ==========

    public TurretBuild createTurretBuild(){
        return new ShieldArcTurretBuild();
    }


    private static ShieldArcTurretBuild paramUnit;
    private static ShieldArcPowerTurret paramField;
    private static final Vec2 paramPos = new Vec2();
    private static final Cons<Bullet> shieldConsumer = b -> {
        if(b.team != paramUnit.team && b.type.absorbable && paramField.shieldData > 0 &&
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

                paramField.shieldHitSound.at(b.x, b.y, 1f + Mathf.range(0.1f), paramField.shieldHitSoundVolume);
            }

            // break shield
            if(paramField.shieldData <= b.damage()){
                paramField.shieldData -= paramField.shieldCooldown * paramField.shieldRegen;

                Fx.arcShieldBreak.at(paramPos.x, paramPos.y, 0, paramField.shieldColor == null ? paramUnit.team.color : paramField.shieldColor, paramUnit);

                paramField.breakSound.at(paramPos.x, paramPos.y);
            }

            // shieldDamage for consistency
            paramField.shieldData -= b.type.shieldDamage(b);
            paramField.shieldAlpha = 1f;
        }
    };

    protected static final Cons<Unit> unitConsumer = unit -> {
        // ignore core units
        if(paramField.shieldData > 0 && unit.targetable(paramUnit.team) &&
                !(unit.within(paramPos, paramField.shieldRadius - paramField.shieldWidth) && paramPos.within(unit.x - unit.deltaX, unit.y - unit.deltaY, paramField.shieldRadius - paramField.shieldWidth)) &&
                (Tmp.v1.set(unit).add(unit.deltaX, unit.deltaY).within(paramPos, paramField.shieldRadius + paramField.shieldWidth) || unit.within(paramPos, paramField.shieldRadius + paramField.shieldWidth)) &&
                (Angles.within(paramPos.angleTo(unit), paramUnit.rotation + paramField.shieldAngleOffset, paramField.shieldAngle / 2f) || Angles.within(paramPos.angleTo(unit.x + unit.deltaX, unit.y + unit.deltaY), paramUnit.rotation + paramField.shieldAngleOffset, paramField.shieldAngle / 2f))){

            if(unit.isMissile() && paramField.missileUnitMultiplier >= 0f){
                Call.unitSafeDeath(unit);
                Fx.absorb.at(unit);
                paramField.pushEffect.at(unit.x, unit.y,paramUnit.team.color);

                // consider missile hp and gamerule to damage the shield
                paramField.shieldData -= unit.health() * paramField.missileUnitMultiplier * Vars.state.rules.unitDamage(unit.team);
                paramField.shieldAlpha = 1f;

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
    public Sound shieldHitSound = Sounds.shieldHit;
    public float shieldHitSoundVolume = 0.12f;
    /** Multiplier for shield damage taken from missile units. */
    public float missileUnitMultiplier = 2f;

    /** Whether to draw the arc line. */
    public boolean drawArc = true;
    /** If not null, will be drawn on top. */
    public @Nullable String region;
    /** Color override of the shield. Uses unit shield colour by default. */
    public @Nullable Color shieldColor;
    /** If true, sprite position will be influenced by x/y. */
    public boolean offsetRegion = false;
    /** If true, enemy units are pushed out. */
    public boolean pushUnits = true;
    public Effect pushEffect = Fx.circleColorSpark;

    /** State. */
    protected float widthScale, shieldAlpha;


    /*public void addStats(){
        super.setStats();
        stats.add("shield", Strings.autoFixed(shieldMax, 2));
        stats.add("repairspeed", Strings.autoFixed(shieldRegen * 60f, 2));
        stats.add("cooldown", Strings.autoFixed(shieldCooldown / 60f, 2));
        if(chanceDeflect > 0f){
            stats.add("deflectchance", Strings.autoFixed(chanceDeflect *100f, 2));
        }
    }*/


    public void update(ShieldArcTurretBuild turret){


        if(shieldData < shieldMax){
            shieldData += Time.delta * shieldRegen;
        }

        boolean active = shieldData > 0 && (turret.isActive() || !whenShooting);
        shieldAlpha = Math.max(shieldAlpha - Time.delta/10f, 0f);

        if(active){
            widthScale = Mathf.lerpDelta(widthScale, 1f, 0.06f);
            paramUnit = turret;
            paramField = this;
            paramPos.set(shieldX, shieldY).rotate(turret.rotation - 90f).add(turret);

            float reach = shieldRadius + shieldWidth;
            Groups.bullet.intersect(paramPos.x - reach, paramPos.y - reach, reach * 2f, reach * 2f, shieldConsumer);
            Units.nearbyEnemies(paramUnit.team, paramPos.x - reach, paramPos.y - reach, reach * 2f, reach * 2f, unitConsumer);
        }else{
            widthScale = Mathf.lerpDelta(widthScale, 0f, 0.11f);
        }
    }


    public void created(ShieldArcTurretBuild turret){
        shieldData = shieldMax;
    }



    public void draw(ShieldArcTurretBuild turret){


        if(widthScale > 0.001f){
            Draw.z(Layer.shields);

            Draw.color(shieldColor == null ? turret.team.color : shieldColor, Color.white, Mathf.clamp(shieldAlpha));
            var pos = paramPos.set(shieldX, shieldY).rotate(turret.rotation - 90f).add(turret);

            if(!Vars.renderer.animateShields){
                Draw.alpha(0.4f);
            }

            if(region != null){
                Vec2 rp = offsetRegion ? pos : Tmp.v1.set(turret);
                Draw.yscl = widthScale;
                Draw.rect(region, rp.x, rp.y, turret.rotation - 90);
                Draw.yscl = 1f;
            }

            if(drawArc){
                Lines.stroke(shieldWidth * widthScale);
                Lines.arc(pos.x, pos.y, shieldRadius, shieldAngle / 360f, turret.rotation + shieldAngleOffset - shieldAngle / 2f);
            }
            Draw.reset();
        }
    }


    public void displayBars(Unit unit, Table bars){
        bars.add(new Bar("stat.shieldhealth", Pal.accent, () -> shieldData / shieldMax)).row();
    }
}
