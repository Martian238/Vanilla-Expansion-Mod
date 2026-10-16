package VanillaExpansion.expand.bullets;

import VanillaExpansion.content.CustomFx;
import arc.graphics.Color;
import arc.math.Interp;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Vec2;
import arc.util.Log;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.content.Liquids;
import mindustry.entities.Damage;
import mindustry.entities.Effect;
import mindustry.entities.Lightning;
import mindustry.entities.Units;
import mindustry.entities.bullet.PointBulletType;
import mindustry.entities.effect.MultiEffect;
import mindustry.entities.effect.ParticleEffect;
import mindustry.gen.Building;
import mindustry.gen.Bullet;
import mindustry.gen.Healthc;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;

public class StabPointBulletType extends PointBulletType {
    private static float cdist = 0f;
    private static Unit result;
    public StabPointBulletType() {}
    public boolean withHydrogen = false;
    public boolean overwriteHit = true;
    public boolean overwrite = true;
    public boolean withLightning = false;
    public Effect lightningEffect = CustomFx.chainLightningBig;

    @Override
    public void init() {
        super.init();
        if(overwrite) {
            if (overwriteHit) hitEffect = CustomFx.instHitColor;
            if (overwriteHit) despawnEffect = CustomFx.instBombColor;
            if (withHydrogen) {
                trailEffect = new MultiEffect(CustomFx.instTrailColor, new ParticleEffect() {{
                    particles = 1;
                    length = 20;
                    interp = Interp.circleOut;
                    sizeInterp = Interp.circleIn;
                    colorFrom = Liquids.hydrogen.color.cpy().a(0.5f);
                    colorTo = Liquids.hydrogen.color.cpy();
                    sizeFrom = 4;
                    lifetime = 60;
                    sizeTo = 0;
                    layer = Layer.bullet;
                }});
            } else {
                trailEffect = CustomFx.instTrailColor;
            }
            shootEffect = CustomFx.instShootColor;
        }
    }

    @Override
    public void init(Bullet b){
        if(killShooter && b.owner() instanceof Healthc h && !h.dead()){
            h.kill();
        }

        if(instantDisappear){
            b.time = lifetime + 1f;
        }

        if(spawnBullets.size > 0){
            for(var bullet : spawnBullets){
                bullet.create(b, b.x, b.y, b.rotation() + Mathf.range(spawnBulletRandomSpread));
            }
        }

        float px = b.x + b.lifetime * b.vel.x,
                py = b.y + b.lifetime * b.vel.y,
                rot = b.rotation();

        Geometry.iterateLine(0f, b.x, b.y, px, py, trailSpacing, (x, y) -> {
            trailEffect.at(x, y, rot, trailColor);
        });


        if(withLightning){
            float sx = b.x, sy = b.y;
            lightningEffect.at(sx, sy, rot, Pal.surge.cpy(), new Vec2(px, py));
            //Log.info(sx + ", " + sy + ", " + px + ", " + py);
        }

        b.time = b.lifetime;
        b.set(px, py);

        //calculate hit entity

        cdist = 0f;
        result = null;
        float range = 1f;

        Units.nearbyEnemies(b.team, px - range, py - range, range*2f, range*2f, e -> {
            if(e.dead() || !e.checkTarget(collidesAir, collidesGround) || !e.hittable()) return;

            e.hitbox(Tmp.r1);
            if(!Tmp.r1.contains(px, py)) return;

            float dst = e.dst(px, py) - e.hitSize;
            if((result == null || dst < cdist)){
                result = e;
                cdist = dst;
            }
        });

        if(result != null){
            b.collision(result, px, py);
        }else if(collidesTiles){
            Building build = Vars.world.buildWorld(px, py);
            if(build != null && build.team != b.team){
                build.collision(b);
                hit(b, px, py);
                b.hit = true;
            }
        }

        b.remove();

        b.vel.setZero();
    }
}
