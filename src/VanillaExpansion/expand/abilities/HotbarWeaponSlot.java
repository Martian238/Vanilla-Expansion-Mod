package VanillaExpansion.expand.abilities;
import arc.audio.Sound;
import arc.math.Mathf;
import mindustry.entities.Effect;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.content.Fx;
public class HotbarWeaponSlot {
    public BulletType bullet;
    public float reload = 30f;
    public float shootCone = 10f;       //允许发射的角度差
    public float shootX = 0f, shootY = 0f;  //相对单位的发射偏移
    public float recoil = 0f;
    public float shake = 0f;
    public Effect shootEffect = Fx.none;
    public Sound shootSound = Sounds.none;
    public float volume = 1f, minPitch = 0.9f, maxPitch = 1.1f;
    public float reloadTimer = 0f;
    public float heatTimer = 0f;
    public float warmupTimer = 0f;
    public float warmup = 0f;           //预热帧数
    public float heat = 0f;             //发射后过热帧数
    public boolean fromCenter = true;
    public boolean alongAim = false;
    public boolean tryShoot(Unit unit, float aimX, float aimY) {
        if (reloadTimer > 0f) return false;
        float bx, by;
        if (fromCenter) {
            bx = unit.x;
            by = unit.y;
        } else if (alongAim) {
            float dirAng = Mathf.angle(aimX - unit.x, aimY - unit.y);
            bx = unit.x + Mathf.cosDeg(dirAng) * shootX;
            by = unit.y + Mathf.sinDeg(dirAng) * shootX;
        } else {
            bx = unit.x + offsetX(unit.rotation, shootX, shootY);
            by = unit.y + offsetY(unit.rotation, shootX, shootY);
        }
        float targetAng = Mathf.angle(aimX - bx, aimY - by);
        float lifeScl = bullet.scaleLife
                ? Mathf.dst(bx, by, aimX, aimY) / (bullet.lifetime * Math.max(1f, bullet.speed))
                : 1f;
        bullet.create(unit, unit.team, bx, by, targetAng, 1f, lifeScl);
        shootEffect.at(bx, by, targetAng);
        bullet.smokeEffect.at(bx, by, targetAng);
        if (shake > 0f) Effect.shake(shake, shake * 2f, bx, by);
        shootSound.at(bx, by, Mathf.random(minPitch, maxPitch), volume);
        if (recoil > 0f) {
            unit.vel.x += recoil * Mathf.cosDeg(targetAng + 180f);
            unit.vel.y += recoil * Mathf.sinDeg(targetAng + 180f);
        }
        reloadTimer = reload;
        heatTimer = heat;
        return true;
    }
    public void updateTimers() {
        if (reloadTimer > 0f) reloadTimer--;
        if (heatTimer > 0f) heatTimer--;
        if (warmupTimer > 0f && warmupTimer < warmup) warmupTimer++;
    }
    public boolean readyToShoot(Unit unit) {
        if (warmup > 0f) {
            if (warmupTimer < warmup) {
                warmupTimer++;
                return false;
            }
        }
        return reloadTimer <= 0f;
    }
    static float offsetX(float unitRot, float ox, float oy) {
        return ox * Mathf.cosDeg(unitRot - 90f) + oy * Mathf.cosDeg(unitRot);
    }
    static float offsetY(float unitRot, float ox, float oy) {
        return ox * Mathf.sinDeg(unitRot - 90f) + oy * Mathf.sinDeg(unitRot);
    }
}