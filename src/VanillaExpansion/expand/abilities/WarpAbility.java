package VanillaExpansion.expand.abilities;

import arc.Core;
import arc.audio.Sound;
import arc.graphics.Color;
import arc.input.KeyCode;
import arc.math.Angles;
import arc.math.Mathf;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.entities.abilities.Ability;
import mindustry.entities.bullet.LaserBulletType;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;

public class WarpAbility extends Ability {
    public WarpAbility() {

    }

    //TODO

    public Sound warpSound = Sounds.none;
    public float warpSoundVolume = 1f;
    public Effect warpStartEffect = Fx.shockwave;
    public Effect warpEndEffect = Fx.shockwave;
    public float warpShake = 16f;
    public float warpShakeDuration = 48f;

    public float warpTrailWidth = 20f;
    public float warpTrailLife = 30f;
    public Color warpTrailColor = Color.valueOf("00ffce");

    public float warpWarmup = 60f;
    public float warpCooldown = 30f;
    public float warpDamage = 10000f;

    public KeyCode warpKey = KeyCode.q;

    private float timer;
    private boolean ready;
    private float cooldown;
    private float warpLength;
    private float wx, wy, angle;

    private final LaserBulletType warpBeamBullet = new LaserBulletType(){{
        damage = warpDamage / 2f;
        absorbable = reflectable = hittable = false;
        collidesGround = false;
        collidesAir = false;
        length = warpLength;
        width = warpTrailWidth;
        laserEffect = Fx.chainLightning;
        sideLength = 0f;
        colors[0].set(warpTrailColor.a(0.5f));
        colors[1].set(warpTrailColor);
        colors[2].set(Color.white);
        hitColor = warpTrailColor;
    }};


    @Override
    public void created(Unit unit){
        cooldown = timer = 0;
        ready = false;
    }

    @Override
    public void update(Unit unit) {
        if(cooldown > 0 && !Core.input.keyDown(warpKey)){
            cooldown -= 1;
        }
        if(Core.input.keyDown(warpKey) && unit.isPlayer() && !unit.dead() && unit.isFlying() && cooldown <= 0){
            if(timer < 0){
                timer = 0;
            }
            timer++;
            wx = unit.aimX;
            wy = unit.aimY;
            angle = Mathf.radiansToDegrees * Mathf.atan2(wx - unit.x, wy - unit.y);
            float sin = Mathf.sinDeg(angle - unit.rotation);
            if(sin > 0){
                unit.rotation(unit.rotation + unit.type.rotateSpeed);

            }else if(sin < 0){
                unit.rotation(unit.rotation - unit.type.rotateSpeed);
            }
            if(Angles.angleDist(unit.rotation, angle) <= unit.type.rotateSpeed){
                unit.rotation(angle);
                if(cooldown <= 0 && timer >= warpWarmup){
                    ready = true;
                }
            }else{
                ready = false;
            }

            if(ready){
                warp(unit);
            }

        }else if (timer > 0){
            timer -= 1;
        }
    }

    public void warp(Unit unit){
        cooldown = warpCooldown;
        timer = 0;
        warpBeamBullet.create(unit, unit.team, unit.x, unit.y, angle);
        warpBeamBullet.create(unit, unit.team, wx, wy, angle + 180f);
        warpStartEffect.at(unit.x, unit.y, angle);
        warpEndEffect.at(wx, wy, angle + 180f);
        Effect.shake(warpShake, warpShakeDuration, unit.x, unit.y);
        Effect.shake(warpShake, warpShakeDuration, wx, wy);
        warpSound.at(unit.x, unit.y, 1f, warpSoundVolume);
        warpSound.at(wx, wy, 1f, warpSoundVolume);
        unit.x(wx);
        unit.y(wy);
    }

}
