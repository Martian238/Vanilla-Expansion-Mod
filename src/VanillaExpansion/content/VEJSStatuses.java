package VanillaExpansion.content;

import arc.graphics.Color;
import mindustry.content.StatusEffects;
import mindustry.type.StatusEffect;

public class VEJSStatuses {

    public static StatusEffect caught, fakeBoss;

    public static void load(){
        caught = new StatusEffect("caught"){{
            color = Color.valueOf("ff835e");
            hideDatabase = true;
            generateIcons = false;
        }};
        fakeBoss = new StatusEffect("boss-fake"){{
            healthMultiplier = StatusEffects.boss.healthMultiplier;
            damageMultiplier = StatusEffects.boss.damageMultiplier;
            color = StatusEffects.boss.color;
            permanent = true;
            hideDatabase = true;
            generateIcons = false;
        }};
    }
}
