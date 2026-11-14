package VanillaExpansion.expand.type.unit.internal.chargeskill;

import VanillaExpansion.expand.type.unit.annihilate.AnnihilatePartUnitType;
import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.content.StatusEffects;
import mindustry.entities.Effect;
import mindustry.entities.Units;

import static mindustry.Vars.tilesize;

public class ChargeSkillReign extends ChargeSkillMechUnit{

    public boolean newspaper = true;
    public float timer = 60;

    @Override
    public void update(){
        super.update();

        if(charges > 0){
            if(chargeDelay > 0){
                chargeDelay -= Time.delta;
            }else {
                chargeTime -= Time.delta;
                apply(StatusEffects.unmoving, Time.delta + 2f);
                apply(StatusEffects.disarmed, Time.delta + 2f);
            }
        }

        if(timer <= 0){
            timer = Mathf.random(60f * 5f, 60f * 10f);
        }else{
            timer -= Time.delta;
            if(timer <= 0){
                chargeRingRadius = 56f;
                chargeRingStroke = 20f;

                charges = 1;
                chargeDelay = chargeDelayMax = 60f;
                chargeTime = chargeTimeMax = 180f;
            }
        }
    }

    @Override
    public void rawDamage(float d){
        dealInterrupt(d);
        super.rawDamage(d);
    }

    public void interruptEffect(){
        if(type instanceof ChargeSkillUnitType t){
            t.interruptEffect.at(x, y, Mathf.range(5f), chargeRingRadius / 50f);
            if(interrupts >= charges){
                t.chargeStopEffect.at(x, y, 0f, chargeRingColor1, chargeRingRadius * (chargeTime / chargeTimeMax));
            }
            t.interruptSound.at(x, y, 1f, 0.7f);
            Effect.shake(t.interruptShake, t.interruptShake * 2f, x, y);
        }
    }

    public void dealInterrupt(float damage){
        if(damage >= 1000f && charges > 0 && chargeDelay <= 0){
            interrupts++;
            interruptEffect();
        }
    }

    @Override
    public void draw(){
        super.draw();
        if(newspaper && type instanceof ChargeSkillUnitType t){
            Draw.rect(t.newspaperRegion, x + 15f * Mathf.cosDeg(rotation), y + 15f * Mathf.sinDeg(rotation),
                    t.newspaperRegion.width * 4.8f, t.newspaperRegion.height * 4, rotation + 180f);
        }
    }
}
