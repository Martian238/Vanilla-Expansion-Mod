package VanillaExpansion.expand.world.block.production;

import arc.math.Mathf;
import mindustry.type.Liquid;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.production.Drill;
import mindustry.world.consumers.ConsumeLiquid;
import mindustry.world.consumers.ConsumeLiquidBase;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import mindustry.world.meta.StatValues;

import static mindustry.Vars.indexer;
import static mindustry.Vars.state;

public class CoolantDrill extends Drill {

    public float coolantMultiplier = 0.5f;
    public float baseHeatCapacity = 0.4f;
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

        /*if(liquidBoostIntensity != 1 && findConsumer(f -> f instanceof ConsumeLiquid && f.booster) instanceof ConsumeLiquid consBase){
            stats.remove(Stat.booster);
            stats.add(Stat.booster,
                    StatValues.speedBoosters("{0}" + StatUnit.timesSpeed.localized(),
                            consBase.amount,
                            (liquidBoostIntensity * liquidBoostIntensity * ( 1f + coolantMultiplier * (consBase.liquid.heatCapacity - baseHeatCapacity)) * ( 1f + coolantMultiplier * (consBase.liquid.heatCapacity - baseHeatCapacity))), false, consBase::consumes)
            );
        }*/
        if(liquidBoostIntensity != 1 && findConsumer(f -> f instanceof ConsumeLiquidBase && f.booster) instanceof ConsumeLiquidBase consBase) {
            stats.remove(Stat.booster);
            for (Liquid l : coolants) {
                l.init();
                stats.add(Stat.booster,
                        StatValues.speedBoosters("{0}" + StatUnit.timesSpeed.localized(),
                                0.1f,
                                (liquidBoostIntensity * liquidBoostIntensity * (1f + coolantMultiplier * (l.heatCapacity - baseHeatCapacity)) * (1f + coolantMultiplier * (l.heatCapacity - baseHeatCapacity))), false, consBase::consumes));
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
            }
        }

    }

}
