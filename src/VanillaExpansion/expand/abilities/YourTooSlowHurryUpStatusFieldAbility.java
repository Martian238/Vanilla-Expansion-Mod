package VanillaExpansion.expand.abilities;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.*;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.UnitController;
import mindustry.game.Team;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.LAccess;
import mindustry.mod.Mods;
import mindustry.type.*;

import static mindustry.Vars.*;
import mindustry.entities.abilities.StatusFieldAbility;
import mindustry.ui.Styles;

import java.util.Objects;

public class YourTooSlowHurryUpStatusFieldAbility extends StatusFieldAbility {
    public float targetHealth = 1000000f;
    public String targetName = "new-horizon-nucleoid";
    public float speedTo = 11.25f / 1.312f;//1.5f
    public float accelTo = 0.06f;
    public float dragTo = 1f;//0.025f
    public float strafePenaltyTo = 0.5f;
    public Effect speedApplyEffect = new Effect();
    public float regenHealth = 100000f;
    public float regenSpeed = 10000f;
    public float regenDuration = 600f;
    public Effect regenApplyEffect = new Effect();
    public StatusEffect speedEffect = new StatusEffect("hyper-speed-status"){{
        speedMultiplier = speedTo;
        dragMultiplier = dragTo;
        applyExtend = false;
        applyEffect = speedApplyEffect;
    }};
    public StatusEffect regenEffect = new StatusEffect("hyper-regen-status"){{
        damage = -1f * regenSpeed;
        applyExtend = false;
        applyEffect = regenApplyEffect;
    }};

    public YourTooSlowHurryUpStatusFieldAbility() {
        super(StatusEffects.none, 60f, 60f, 60f);
    }
    public YourTooSlowHurryUpStatusFieldAbility(StatusEffect effect, float duration, float reload, float range){
        super(effect, duration, reload, range);
        this.duration = duration;
        this.reload = reload;
        this.range = range;
        this.effect = effect;
    }






    @Override
    public void update(Unit unit){
        timer += Time.delta;



        if(timer >= reload && (!onShoot || unit.isShooting)){
            Units.nearby(unit.team, unit.x, unit.y, range, other -> {
                other.apply(effect, duration);
                applyEffect.at(other, parentizeEffects);
                if( Objects.equals(other.type.name, targetName)){
                    other.apply(speedEffect, duration);
                    if(other.health <= regenHealth){
                        other.apply(regenEffect, regenDuration);
                    }
                }

            });
//other.maxHealth == targetHealth &&

            float x = unit.x + Angles.trnsx(unit.rotation, effectY, effectX), y = unit.y + Angles.trnsy(unit.rotation, effectY, effectX);
            activeEffect.at(x, y, effectSizeParam ? range : unit.rotation, color, parentizeEffects ? unit : null);

            timer = 0f;
        }
    }

    @Override
    public void addStats(Table t){
        super.addStats(t);
        t.row();

        //t.add(Core.bundle.format("bullet.range", Strings.autoFixed(range / tilesize, 2)));
        //t.row();
        t.add(abilityStat("hyperspeed.multiplier", Strings.autoFixed(Math.round(speedTo), 2)));
        t.row();
        t.add(abilityStat("hyperspeed.firingrate", Strings.autoFixed(60f / reload, 2)));
        t.row();
        t.add(abilityStat("hyperspeed.duration", Strings.autoFixed(duration / 60f, 2)));
        t.row();
        t.add(abilityStat("hyperregen.health", Strings.autoFixed(regenHealth, 2)));
        t.row();
        t.add(abilityStat("hyperregen.speed", Strings.autoFixed(regenSpeed * 60f, 2)));
        t.row();
        t.add(abilityStat("hyperregen.duration", Strings.autoFixed(regenDuration / 60f, 2)));
        t.row();
    }
}
