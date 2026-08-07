package VanillaExpansion.expand.world.block.power;

import arc.*;
import arc.audio.Sound;
import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.Sounds;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.blocks.power.ConsumeGenerator;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

public class ShakeGenerator extends ConsumeGenerator {

    public float shake = 2f;
    public float shakeDuration = 4f;
    public float minShakeStartWarmup = 0.65f;

    public float lightningChance = 0.03f;
    public int lightningLength = 9;
    public int lightningLengthRand = 4;
    public Color lightningColor = Pal.surge;
    public float lightningDamage = 40f;
    public Sound lightningSound = Sounds.none;
    public float lightningVolume = 0.15f;

    public ShakeGenerator(String name){
        super(name);
    }



    public class ShakeGeneratorBuild extends GeneratorBuild{
        public float warmup, totalTime, efficiencyMultiplier = 1f, itemDurationMultiplier = 1;

        @Override
        public void updateEfficiencyMultiplier(){
            if(filterItem != null){
                float m = filterItem.efficiencyMultiplier(this);
                if(m > 0) efficiencyMultiplier = m;
            }else if(filterLiquid != null){
                float m = filterLiquid.efficiencyMultiplier(this);
                if(m > 0) efficiencyMultiplier = m;
            }
        }

        @Override
        public void updateTile(){
            boolean valid = efficiency > 0;

            warmup = Mathf.lerpDelta(warmup, valid ? 1f : 0f, warmupSpeed);

            productionEfficiency = efficiency * efficiencyMultiplier;
            totalTime += warmup * Time.delta;

            //randomly produce the effect
            if(valid && Mathf.chanceDelta(effectChance)){
                generateEffect.at(x + Mathf.range(generateEffectRange), y + Mathf.range(generateEffectRange));
            }

            //make sure the multiplier doesn't change when there is nothing to consume while it's still running
            if(filterItem != null && valid && itemDurationMultipliers.size > 0 && filterItem.getConsumed(this) != null){
                itemDurationMultiplier = itemDurationMultipliers.get(filterItem.getConsumed(this), 1);
            }

            //take in items periodically
            if(hasItems && valid && generateTime <= 0f){
                consume();
                consumeEffect.at(x + Mathf.range(generateEffectRange), y + Mathf.range(generateEffectRange));
                generateTime = 1f;
            }

            if(wasVisible && shake > 0f && warmup >= minShakeStartWarmup){
                Effect.shake(shake * ((warmup - minShakeStartWarmup) / (1f - minShakeStartWarmup)), shakeDuration, x, y);
            }

            if(outputLiquid != null){
                float added = Math.min(productionEfficiency * delta() * outputLiquid.amount, liquidCapacity - liquids.get(outputLiquid.liquid));
                liquids.add(outputLiquid.liquid, added);
                dumpLiquid(outputLiquid.liquid);

                if(explodeOnFull && liquids.get(outputLiquid.liquid) >= liquidCapacity - 0.01f){
                    kill();
                    Events.fire(new GeneratorPressureExplodeEvent(this));
                }
            }

            if(wasVisible && warmup >= minShakeStartWarmup){

            if(Mathf.chanceDelta(lightningChance * ((warmup - minShakeStartWarmup) / (1f - minShakeStartWarmup)))){

                Lightning.create(team, lightningColor, lightningDamage, x + Mathf.range(size), y + Mathf.range(size), Mathf.range(0,360), lightningLength + Mathf.range(lightningLengthRand));
                lightningSound.at(tile, Mathf.random(0.9f, 1.1f), lightningVolume);

            }}

            //generation time always goes down, but only at the end so consumeTriggerValid doesn't assume fake items
            generateTime -= delta() / (itemDuration * itemDurationMultiplier);
        }

        @Override
        public boolean consumeTriggerValid(){
            return generateTime > 0;
        }

        @Override
        public float warmup(){
            return warmup;
        }

        @Override
        public float totalProgress(){
            return totalTime;
        }

        @Override
        public void drawLight(){
            //???
            drawer.drawLight(this);
            //TODO hard coded
            Drawf.light(x, y, (60f + Mathf.absin(10f, 5f)) * size, Color.orange, 0.5f * warmup);
        }
    }

}
