package VanillaExpansion.expand.world.block.production;

import VanillaExpansion.annotations.Annotations;
import arc.Core;
import arc.audio.Sound;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import mindustry.graphics.Layer;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.production.Drill;
import mindustry.world.consumers.ConsumeLiquid;
import mindustry.world.consumers.ConsumeLiquidBase;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import mindustry.world.meta.StatValues;
import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;

import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import mindustry.world.Block;

import static mindustry.Vars.*;

public class CoolantDrill extends Drill {

    public float coolantMultiplier = 0.5f;
    public float baseHeatCapacity = 0.4f;
    public float consumeSpeed = 0.1f;
    public float lightningChance = 0.02f;
    public int lightningLength = 7;
    public int lightningLengthRand = 2;
    public Color lightningColor = Pal.surge;
    public float lightningDamage = 30f;
    public Sound lightningSound = Sounds.none;
    public float lightningVolume = 0.2f;
    public Liquid[] coolants = {};

    public CoolantDrill(String name){
        super(name);
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.remove(Stat.drillTier);
        stats.remove(Stat.drillSpeed);

        stats.add(Stat.drillTier, StatValues.drillables(drillTime, hardnessDrillMultiplier, size * size, drillMultipliers, b -> b instanceof Floor f && !f.wallOre && f.itemDrop != null &&
                f.itemDrop.hardness <= tier && (blockedItems == null || !blockedItems.contains(f.itemDrop)) && (indexer.isBlockPresent(f) || state.isMenu())));

        stats.add(Stat.drillSpeed, 60f / drillTime * size * size, StatUnit.itemsSecond);


        if(liquidBoostIntensity != 1 && findConsumer(f -> f instanceof ConsumeLiquidBase && f.booster) instanceof ConsumeLiquidBase consBase) {
            stats.remove(Stat.booster);
            for (Liquid l : coolants) {
                l.init();
                stats.add(Stat.booster, StatValues.liquid(l,consumeSpeed * 60f, true));
                stats.add(Stat.booster, StatValues.fixValue((liquidBoostIntensity * liquidBoostIntensity * (1f + coolantMultiplier * (l.heatCapacity - baseHeatCapacity)) * (1f + coolantMultiplier * (l.heatCapacity - baseHeatCapacity)))).toString()+StatUnit.timesSpeed.localized());

                }
        }
    }



    public class CoolantDrillBuild extends DrillBuild {

        @Override
        public void updateTile(){
            if(timer(timerDump, dumpTime / timeScale)){
                dump(dominantItem != null && items.has(dominantItem) ? dominantItem : null);
            }

            if(dominantItem == null){
                return;
            }

            timeDrilled += warmup * delta();

            float delay = getDrillTime(dominantItem);

            Liquid coolantLiquid = liquids.current(); // read the liquid's coolant power and use it

            if(items.total() < itemCapacity && dominantItems > 0 && efficiency > 0){
                float speed = Mathf.lerp(1f, liquidBoostIntensity * (1f + coolantMultiplier * (coolantLiquid.heatCapacity - baseHeatCapacity)), optionalEfficiency) * efficiency;

                lastDrillSpeed = (speed * dominantItems * warmup) / delay;
                warmup = Mathf.approachDelta(warmup, speed, warmupSpeed);
                progress += delta() * dominantItems * speed * warmup;

                if(Mathf.chanceDelta(updateEffectChance * warmup))
                    updateEffect.at(x + Mathf.range(size * 2f), y + Mathf.range(size * 2f));
            }else{
                lastDrillSpeed = 0f;
                warmup = Mathf.approachDelta(warmup, 0f, warmupSpeed);
                return;
            }

            if(dominantItems > 0 && progress >= delay && items.total() < itemCapacity){
                int amount = (int)(progress / delay);
                for(int i = 0; i < amount; i++){
                    offload(dominantItem);
                }

                progress %= delay;

                if(wasVisible && Mathf.chanceDelta(drillEffectChance * warmup)) drillEffect.at(x + Mathf.range(drillEffectRnd), y + Mathf.range(drillEffectRnd), dominantItem.color);
                if(wasVisible && Mathf.chanceDelta(lightningChance * warmup)){

                        Lightning.create(team, lightningColor, lightningDamage, x + Mathf.range(drillEffectRnd), y + Mathf.range(drillEffectRnd), Mathf.range(0,360), lightningLength + Mathf.range(lightningLengthRand));
                        lightningSound.at(tile, Mathf.random(0.9f, 1.1f), lightningVolume);

                }
            }
        }

    }




}
