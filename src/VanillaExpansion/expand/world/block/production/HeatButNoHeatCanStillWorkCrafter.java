package VanillaExpansion.expand.world.block.production;

import arc.Core;
import arc.math.Mathf;
import mindustry.graphics.Pal;
import mindustry.logic.LAccess;
import mindustry.ui.Bar;
import mindustry.world.blocks.heat.HeatConsumer;
import mindustry.world.blocks.production.HeatCrafter;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

public class HeatButNoHeatCanStillWorkCrafter extends HeatCrafter {

    public float basicEfficiency = 1f;

    public HeatButNoHeatCanStillWorkCrafter(String name) {
        super(name);
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.remove(Stat.input);
        stats.remove(Stat.maxEfficiency);
        for(var c : consumers){
            c.display(stats);
        }
        stats.add(Stat.booster, heatRequirement, StatUnit.heatUnits);
        stats.add(Stat.affinities, (int)(basicEfficiency * 100f), StatUnit.percent);
        stats.add(Stat.maxEfficiency, (int)((maxEfficiency + basicEfficiency) * 100f), StatUnit.percent);
    }

    @Override
    public void setBars(){
        super.setBars();

        removeBar("heat");

        addBar("heat", (HBNHCSWCrafterBuild entity) ->
                new Bar(() ->
                        Core.bundle.format("bar.heatpercent", (int)(entity.heat + 0.01f), (int)(entity.efficiencyScale() * 100 + 0.01f)),
                        () -> Pal.lightOrange,
                        () -> entity.heat / heatRequirement));
    }

    public class HBNHCSWCrafterBuild extends GenericCrafterBuild implements HeatConsumer {

        public float[] sideHeat = new float[4];
        public float heat = 0f;

        @Override
        public void updateTile(){
            heat = calculateHeat(sideHeat);

            super.updateTile();
        }

        @Override
        public boolean shouldConsume(){
            return ((heatRequirement <= 0f || heat > 0) || (basicEfficiency > 0)) && super.shouldConsume();
        }
        @Override
        public float warmupTarget(){
            return basicEfficiency + Mathf.clamp((heat / heatRequirement));
        }


        @Override
        public float heatRequirement(){
            return heatRequirement;
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.heat) return heat;
            return super.sense(sensor);
        }

        @Override
        public float[] sideHeat(){
            return sideHeat;
        }

        @Override
        public float efficiencyScale(){
            float over = Math.max(heat - heatRequirement, 0f);
            return Math.min(basicEfficiency + Mathf.clamp(heat / heatRequirement) + over / heatRequirement * overheatScale, basicEfficiency + maxEfficiency);
        }
    }
}
