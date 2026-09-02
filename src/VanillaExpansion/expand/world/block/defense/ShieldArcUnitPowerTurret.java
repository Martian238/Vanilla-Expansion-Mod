package VanillaExpansion.expand.world.block.defense;

import VanillaExpansion.expand.world.block.production.ConfigurableHeatProducer;
import arc.Core;
import arc.Events;
import arc.audio.Sound;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.struct.Seq;
import arc.util.Structs;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.content.Fx;
import mindustry.content.StatusEffects;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.entities.abilities.ShieldArcAbility;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.game.EventType;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.graphics.Pal;
import mindustry.type.UnitType;
import mindustry.type.Weapon;
import mindustry.type.unit.MissileUnitType;
import mindustry.ui.Bar;
import mindustry.world.blocks.defense.turrets.PowerTurret;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

public class ShieldArcUnitPowerTurret extends PowerTurret {
    public ShieldArcUnitPowerTurret(String name){
        super(name);
    }

    public float shieldMax = 4000f;
    public float shieldRadius = 80f;
    public float shieldWidth = 12f;
    public float shieldAngle = 120f;
    public float shieldX = 0f, shieldY = 0f;
    public float shieldMissileUnitMultiplier = 0.25f;
    public float shieldRegen = 1f;
    public float shieldCooldown = 300f;
    public Sound shieldCreateSound = Sounds.none;

    public Effect hotEffect = Fx.reactorsmoke;
    public TextureRegion sbcnm;

    @Override
    public void load(){
        super.load();
        sbcnm = Core.atlas.find("temporary-shield-unit");
        if(sbcnm == null){
            sbcnm = Core.atlas.find("error");
        }
        shieldUnitType.region = sbcnm;
        shieldUnitType.abilities = Seq.with(
                new ShieldArcAbility(){{
                    max = shieldMax;
                    radius = shieldRadius;
                    angle = shieldAngle;
                    x = shieldX;
                    y = shieldY;
                    missileUnitMultiplier = shieldMissileUnitMultiplier;
                    width = shieldWidth;
                    regen = shieldRegen;
                    cooldown = shieldCooldown;
                    pushUnits = true;
                    whenShooting = false;
                }}
        );
    }
    private UnitType shieldUnitType = new UnitType("temporary-shield-unit"){{
        hittable = drawBody = drawShields = drawCell = drawSoftShadow = wobble = targetable
                = createScorch = createWreck = physics = useUnitCap = false;
        region = sbcnm;
        flying = true;
        health = 9999999;
        engineSize = wreckSoundVolume = deathShake = 0;
        deathExplosionEffect = fallEffect = fallEngineEffect = Fx.none;
        deathSound = wreckSound = Sounds.none;
        shadowElevation = 9999;
        hitSize = 0.01f;
        weapons.add(
                new Weapon(){{
                    shootSound = Sounds.none;
                    mirror = false;
                    x = y = 0;
                    reload = 60f;
                    alwaysShooting = true;
                    minWarmup = 0.9f;
                    bullet = new BasicBulletType(){{
                        killShooter = instantDisappear = true;
                        shootEffect = smokeEffect = hitEffect = despawnEffect = Fx.none;
                        collides = absorbable = reflectable = hittable = false;
                        damage = 0;
                    }};
                }}
        );
    }};

    @Override
    public void setBars(){
        super.setBars();
        addBar("shield", (ShieldArcUnitPowerTurretBuild entity) ->
                new Bar("stat.shieldhealth", Pal.accent, () -> Mathf.clamp(entity.currentShield / shieldMax)));
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.shieldHealth, shieldMax);
        stats.add(Stat.regenerationRate, shieldRegen * 60f, StatUnit.perSecond);
        stats.add(Stat.cooldownTime, shieldCooldown * 60f, StatUnit.seconds);
    }

    public class ShieldArcUnitPowerTurretBuild extends PowerTurretBuild{

        private float currentShield = shieldMax;
        private Unit shieldUnit;
        private float currentRadius = 0f;
        private float lastRadius = 0f;



        @Override
        public void updateTile(){
            super.updateTile();
            if(isShooting() && shieldUnit == null){
                shieldUnit = shieldUnitType.create(team);
                shieldUnit.set(x, y);
                Events.fire(new EventType.UnitCreateEvent(shieldUnit, this, null));
                if (!Vars.net.client()) {
                    shieldUnit.add();
                    Units.notifyUnitSpawn(shieldUnit);
                }
                shieldCreateSound.at(x, y);
                if(currentShield < shieldMax && shieldUnit.abilities[0] != null){
                    shieldUnit.abilities[0].data = currentShield;
                }
            }
            if(shieldUnit != null && shieldUnit.abilities[0] != null){
                shieldUnit.rotation(rotation);
                shieldUnit.set(x, y);
                shieldUnit.apply(StatusEffects.disarmed, 30f);
                currentShield = shieldUnit.abilities[0].data;
                //currentRadius = Mathf.lerp(0f, shieldRadius, warmup());
                currentRadius = shieldRadius;
                lastRadius = currentRadius;
                if((warmup() <= 0.1f && !isShooting() && !isControlled()) || (!isShooting() && isControlled())){
                    Effect be = new Effect(40, e -> {
                        Lines.stroke(3 * e.fout(), e.color);
                                Lines.arc(x, y, lastRadius + shieldWidth/2, shieldAngle / 360f, rotation - shieldAngle / 2f);
                                Lines.arc(x, y, lastRadius - shieldWidth/2, shieldAngle / 360f, rotation - shieldAngle / 2f);
                                for(int i : Mathf.signs){
                                    float
                                            px = x + Angles.trnsx(rotation - shieldAngle / 2f * i, lastRadius + shieldWidth / 2),
                                            py = y + Angles.trnsy(rotation - shieldAngle / 2f * i, lastRadius + shieldWidth / 2),
                                            px1 = x + Angles.trnsx(rotation - shieldAngle / 2f * i, lastRadius - shieldWidth / 2),
                                            py1 = y + Angles.trnsy(rotation - shieldAngle / 2f * i, lastRadius - shieldWidth / 2);
                                    Lines.line(px, py, px1, py1);
                                }
                    }).followParent(true);
                    be.at(x, y, rotation, team.color);
                    shieldUnit.kill();
                }
            }
            if (shieldUnit != null && shieldUnit.dead) {
                shieldUnit = null;
            }
            if(currentShield < shieldMax){
                if(shieldUnit == null) {
                    currentShield += shieldRegen;
                    if (currentShield > shieldMax) {
                        currentShield = shieldMax;
                    }
                }
                currentRadius = 0f;
            }
            if(shieldUnit == null){
                currentRadius = 0f;
            }
        }

        @Override
        public void kill(){
            if(shieldUnit != null) shieldUnit.kill();
            super.kill();
        }

        @Override
        public void remove(){
            if(shieldUnit != null) shieldUnit.kill();
            super.remove();
        }
    }
}
