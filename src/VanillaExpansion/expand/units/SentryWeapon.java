package VanillaExpansion.expand.units;
import arc.math.Angles;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.audio.SoundLoop;
import mindustry.entities.Predict;
import mindustry.entities.Sized;
import mindustry.entities.Units;
import mindustry.entities.units.WeaponMount;
import mindustry.gen.Building;
import mindustry.gen.Sounds;
import mindustry.gen.Teamc;
import mindustry.gen.Unit;
import mindustry.type.Weapon;

import static mindustry.Vars.headless;
public class SentryWeapon extends Weapon {
    public float cone = 90f;
    public boolean targetBuildings = false;
    public SentryWeapon(String name){
        super(name);
    }
    public SentryWeapon(){
        this("");
        predictTarget = false;
    }
    @Override
    public void update(Unit unit, WeaponMount mount){
        boolean can = unit.canShoot();
        float mountX = unit.x + Angles.trnsx(unit.rotation - 90, x, y);
        float mountY = unit.y + Angles.trnsy(unit.rotation - 90, x, y);
        if(!controllable && autoTarget){
            boolean needNewTarget = (mount.retarget -= Time.delta) <= 0f || 
                                    (mount.target != null && !isTargetValid(unit, mount.target)) || !Angles.within(rotate ? mount.rotation : unit.rotation + baseRotation, mount.targetRotation, cone);
            if(needNewTarget){
                mount.target = findValidTarget(unit, mountX, mountY, bullet.range + Math.abs(shootY));
                mount.retarget = mount.target == null ? targetInterval : targetSwitchInterval;
            }
            if(mount.target != null){
                if(predictTarget){
                    Vec2 to = Predict.intercept(unit, mount.target, bullet);
                    mount.aimX = to.x;
                    mount.aimY = to.y;
                }else{
                    mount.aimX = mount.target.x();
                    mount.aimY = mount.target.y();
                }
                boolean inRange = mount.target.within(mountX, mountY, bullet.range + Math.abs(shootY) + 
                    (mount.target instanceof Sized s ? s.hitSize()/2f : 0f)) && Angles.within(rotate ? mount.rotation : unit.rotation + baseRotation, mount.targetRotation, cone);
                mount.shoot = mount.rotate = inRange && can;
            }else{
                mount.shoot = false;
                mount.rotate = false;
            }
        }
        float lastReload = mount.reload;
        mount.reload = Math.max(mount.reload - Time.delta * unit.reloadMultiplier, 0);
        mount.recoil = Mathf.approachDelta(mount.recoil, 0, unit.reloadMultiplier / recoilTime);
        if(recoils > 0){
            if(mount.recoils == null) mount.recoils = new float[recoils];
            for(int i = 0; i < recoils; i++){
                mount.recoils[i] = Mathf.approachDelta(mount.recoils[i], 0, unit.reloadMultiplier / recoilTime);
            }
        }
        mount.smoothReload = Mathf.lerpDelta(mount.smoothReload, mount.reload / reload, smoothReloadSpeed);
        mount.charge = mount.charging && shoot.firstShotDelay > 0 ? Mathf.approachDelta(mount.charge, 1, 1 / shoot.firstShotDelay) : 0;
        float warmupTarget = (can && mount.shoot) || (continuous && mount.bullet != null) || mount.charging ? 1f : 0f;
        if(linearWarmup){
            mount.warmup = Mathf.approachDelta(mount.warmup, warmupTarget, shootWarmupSpeed);
        }else{
            mount.warmup = Mathf.lerpDelta(mount.warmup, warmupTarget, shootWarmupSpeed);
        }
        if(rotate && (mount.rotate || mount.shoot) && can && Angles.within(mount.rotation, mount.targetRotation, cone)){
            float axisX = unit.x + Angles.trnsx(unit.rotation - 90, x, y);
            float axisY = unit.y + Angles.trnsy(unit.rotation - 90, x, y);
            mount.targetRotation = Angles.angle(axisX, axisY, mount.aimX, mount.aimY) - unit.rotation;
            mount.rotation = Angles.moveToward(mount.rotation, mount.targetRotation, rotateSpeed * Time.delta);
            if(rotationLimit < 360){
                float dst = Angles.angleDist(mount.rotation, baseRotation);
                if(dst > rotationLimit/2f){
                    mount.rotation = Angles.moveToward(mount.rotation, baseRotation, dst - rotationLimit/2f);
                }
            }
        }else if(!rotate && Angles.within(unit.rotation + baseRotation, mount.targetRotation, cone)){
            mount.rotation = baseRotation;
            mount.targetRotation = unit.angleTo(mount.aimX, mount.aimY);
        }
        float weaponRotation = unit.rotation - 90 + (rotate ? mount.rotation : baseRotation);
        float bulletX = mountX + Angles.trnsx(weaponRotation, this.shootX, this.shootY);
        float bulletY = mountY + Angles.trnsy(weaponRotation, this.shootX, this.shootY);
        float shootAngle = bulletRotation(unit, mount, bulletX, bulletY);
        if(alwaysShooting) mount.shoot = true;
        if(continuous && mount.bullet != null){
            if(!mount.bullet.isAdded() || mount.bullet.time >= mount.bullet.lifetime || mount.bullet.type != bullet){
                mount.bullet = null;
            }else{
                mount.bullet.rotation(weaponRotation + 90);
                mount.bullet.set(bulletX, bulletY);
                mount.reload = reload;
                mount.recoil = 1f;
                unit.vel.add(Tmp.v1.trns(mount.bullet.rotation() + 180f, mount.bullet.type.recoil * Time.delta));
                if(shootSound != Sounds.none && !headless){
                    if(mount.sound == null) mount.sound = new SoundLoop(shootSound, 1f);
                    mount.sound.update(bulletX, bulletY, true);
                }
                float shootLength = Math.min(Mathf.dst(bulletX, bulletY, mount.aimX, mount.aimY), range());
                float curLength = Mathf.dst(bulletX, bulletY, mount.bullet.aimX, mount.bullet.aimY);
                float resultLength = Mathf.approachDelta(curLength, shootLength, aimChangeSpeed);
                Tmp.v1.trns(shootAngle, mount.lastLength = resultLength).add(bulletX, bulletY);
                mount.bullet.aimX = Tmp.v1.x;
                mount.bullet.aimY = Tmp.v1.y;
                if(alwaysContinuous && mount.shoot){
                    mount.bullet.time = mount.bullet.lifetime * mount.bullet.type.optimalLifeFract * mount.warmup;
                    mount.bullet.keepAlive = true;
                    unit.apply(shootStatus, shootStatusDuration);
                }
            }
        }else{
            mount.heat = Math.max(mount.heat - Time.delta * unit.reloadMultiplier / cooldownTime, 0);
            if(mount.sound != null){
                mount.sound.update(bulletX, bulletY, false);
            }
        }
        boolean wasFlipped = mount.side;
        if(otherSide >= 0 && alternate && mount.side == flipSprite && otherSide < unit.mounts.length && mount.reload <= reload / 2f && lastReload > reload / 2f){
            unit.mounts[otherSide].side = !unit.mounts[otherSide].side;
            mount.side = !mount.side;
        }
        if(!headless && activeSound != Sounds.none && mount.shoot && can && mount.warmup >= minWarmup){
            Vars.control.sound.loop(activeSound, unit, activeSoundVolume);
        }
        float velLen = unit.isRemote() ? unit.vel.len() : unit.deltaLen() / Time.delta;
        if(mount.shoot && can && !(bullet.killShooter && mount.totalShots > 0) &&
                (!alternate || wasFlipped == flipSprite) &&
                mount.warmup >= minWarmup && velLen >= minShootVelocity &&
                (mount.reload <= 0.0001f || (alwaysContinuous && mount.bullet == null)) &&
                (alwaysShooting || Angles.within(rotate ? mount.rotation : unit.rotation + baseRotation, mount.targetRotation, shootCone))){
            if(mount.target != null && isTargetValid(unit, mount.target)){
                shoot(unit, mount, bulletX, bulletY, shootAngle);
                mount.reload = reload;
            }else{
                mount.shoot = false;
                mount.target = null;
                mount.retarget = 0f;
            }
        }
    }
    private Teamc findValidTarget(Unit unit, float x, float y, float range){
        return Units.closestTarget(
            unit.team,
            x,
            y,
            range,
            u -> {
                if(!u.checkTarget(bullet.collidesAir, bullet.collidesGround)) return false;
                return true;
            },
            t -> {
                if(!bullet.collidesGround) return false;
                if(!targetBuildings)  return false;
                return false;
            }
        );
    }
    private boolean isTargetValid(Unit unit, Teamc target){
        if(target instanceof Unit u){
            return u.isAdded() && u.team != unit.team ;
        }
        if(target instanceof Building b){
            return targetBuildings && b.isAdded() && b.team != unit.team ;
        }
        return false;
    }
}