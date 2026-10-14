package VanillaExpansion.content;

import mindustry.content.StatusEffects;
import mindustry.type.StatusEffect;

public class VEJSStatuses {

    public static StatusEffect fakeBoss;

    public static void load(){
        fakeBoss = new StatusEffect("boss-fake"){{
            healthMultiplier = StatusEffects.boss.healthMultiplier;
            damageMultiplier = StatusEffects.boss.damageMultiplier;
            color = StatusEffects.boss.color;
            permanent = true;
            hideDatabase = true;
        }};
    }
}
