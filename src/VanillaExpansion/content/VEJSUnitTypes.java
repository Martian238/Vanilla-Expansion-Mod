package VanillaExpansion.content;

import VanillaExpansion.expand.type.unit.HyperUnit;
import mindustry.content.Fx;
import mindustry.gen.Sounds;
import mindustry.gen.TimedKillUnit;
import mindustry.gen.UnitEntity;
import mindustry.type.UnitType;

public class VEJSUnitTypes {

    //Boss
    public static UnitType hyper;

    //Internal
    public static UnitType textTrigger;

    public static void load(){


        hyper = new UnitType("hyper"){{
            constructor = HyperUnit::create;
        }};

        textTrigger = new UnitType("text-trigger"){{
            constructor = TimedKillUnit::create;
            lifetime = 30f;
            isEnemy = drawMinimap = false;
            hidden = true;
            targetable = hittable = false;
            drawBody = drawCell = drawSoftShadow = false;
            health = 9999999f;
            deathShake = deathSoundVolume = 0f;
            createWreck = createScorch = false;
            deathExplosionEffect = fallEffect = fallEngineEffect = Fx.none;
            wreckSound = Sounds.none;
        }};
    }
}
