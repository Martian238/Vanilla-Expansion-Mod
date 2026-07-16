package VanillaExpansion.expand.abilities;

import arc.util.Time;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;
import mindustry.graphics.Pal;
import mindustry.type.StatusEffect;

public class AttackSlowAbility extends Ability {
    public float normalSpeed = 3f;
    public float attackSpeed = 0.1f;
    public float recoveryTime = 180f;

    private float lastShotTime = -Float.MAX_VALUE;
    private StatusEffect slowEffect;

    public AttackSlowAbility() {
        slowEffect = createSlowEffect();
    }

    public AttackSlowAbility(float normalSpeed, float attackSpeed, float recoveryTime) {
        this.normalSpeed = normalSpeed;
        this.attackSpeed = attackSpeed;
        this.recoveryTime = recoveryTime;
        slowEffect = createSlowEffect();
    }

    private StatusEffect createSlowEffect() {
        return new StatusEffect("proxima-attack-slow") {{
            speedMultiplier = attackSpeed / normalSpeed;
            color = Pal.lancerLaser;
        }};
    }

    @Override
    public void update(Unit unit) {
        boolean isAttacking = unit.isShooting();

        if (isAttacking) {
            lastShotTime = Time.time;
            unit.apply(slowEffect, recoveryTime);
        }
    }
}