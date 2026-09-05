package VanillaExpansion.expand.world.block.defense;

import arc.Events;
import mindustry.content.StatusEffects;
import mindustry.entities.Damage;
import mindustry.entities.bullet.BulletType;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Bullet;
import mindustry.world.blocks.defense.Wall;

public class TantalumWall extends Wall {
    public TantalumWall(String name){
        super(name);
    }

    public float corrosiveDamageMultiplier = 0f;

    public class TantalumWallBuild extends WallBuild{
        @Override
        public boolean collide(Bullet other) {
            return true;
        }

        @Override
        public boolean collision(Bullet other) {
            boolean wasDead = health <= 0;
            BulletType t = other.type;
            float damage = other.type.buildingDamage(other);
            if(t.status == StatusEffects.corroded){
                damage *= corrosiveDamageMultiplier;
            }
            if (!t.pierceArmor) {
                damage = Damage.applyArmor(damage, block.armor * t.armorMultiplier * t.blockArmorMultiplier);
            }
            damage(other, other.team, damage);
            if (health <= 0 && !wasDead) {
                Events.fire(new EventType.BuildingBulletDestroyEvent(this, other));
            }
            return true;
        }

        @Override
        public void damage(Bullet bullet, Team source, float damage) {
            if(bullet != null && bullet.type.status == StatusEffects.corroded){
                damage(source, damage * corrosiveDamageMultiplier);
                Events.fire(bulletDamageEvent.set(this, bullet));
                return;
            }
            damage(source, damage);
            Events.fire(bulletDamageEvent.set(this, bullet));
        }
    }
}
