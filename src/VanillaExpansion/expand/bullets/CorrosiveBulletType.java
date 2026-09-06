package VanillaExpansion.expand.bullets;

import VanillaExpansion.entities.CorrosiveDamage;
import VanillaExpansion.expand.world.block.defense.TantalumWall;
import mindustry.content.StatusEffects;
import mindustry.entities.Damage;
import mindustry.entities.Fires;
import mindustry.entities.bullet.BasicBulletType;
import mindustry.gen.Building;
import mindustry.gen.Bullet;

import static mindustry.Vars.indexer;

public class CorrosiveBulletType extends BasicBulletType {
    public CorrosiveBulletType() {}

    @Override
    public void createSplashDamage(Bullet b, float x, float y){
        if(splashDamageRadius > 0 && !b.absorbed){
            CorrosiveDamage.damage(b.team, x, y, splashDamageRadius, splashDamage * b.damageMultiplier(), splashDamagePierce, collidesAir, collidesGround, scaledSplashDamage, b, armorMultiplier);

            if(status != StatusEffects.none){
                Damage.status(b.team, x, y, splashDamageRadius, status, statusDuration, collidesAir, collidesGround);
            }

            if(heals()){
                indexer.eachBlock(b.team, x, y, splashDamageRadius, Building::damaged, other -> {
                    healEffect.at(other.x, other.y, 0f, healColor, other.block);
                    other.heal(healPercent / 100f * other.maxHealth() + healAmount);
                });
            }

            if(makeFire){
                indexer.eachBlock(null, x, y, splashDamageRadius, other -> other.team != b.team && !(other instanceof TantalumWall.TantalumWallBuild), other -> Fires.create(other.tile));
            }
        }
    }
}
