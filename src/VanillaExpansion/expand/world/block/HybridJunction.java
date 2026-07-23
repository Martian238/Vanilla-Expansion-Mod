package VanillaExpansion.expand.world.block;

import mindustry.gen.Building;
import mindustry.type.Liquid;
import mindustry.world.blocks.distribution.Junction;
import mindustry.world.blocks.liquid.LiquidJunction;
import mindustry.world.meta.Stat;



public class HybridJunction extends Junction {
    public HybridJunction(String name) {
        super(name);
        floating = true;
        hasLiquids = true;
        outputsLiquid = true;


    }

    @Override
    public void setStats(){
        super.setStats();
        stats.remove(Stat.liquidCapacity);
    }

    @Override
    public void setBars(){
        super.setBars();
        removeBar("liquid");
    }
    public class HybridJunctionBuild extends JunctionBuild {


        @Override
        public Building getLiquidDestination(Building source, Liquid liquid){
            if(!enabled) return this;

            int dir = (source.relativeTo(tile.x, tile.y) + 4) % 4;
            Building next = nearby(dir);
            if(next == null || (!next.acceptLiquid(this, liquid) && !(next.block instanceof LiquidJunction) && !(next.block instanceof HybridJunction))){
                return this;
            }
            return next.getLiquidDestination(this, liquid);
        }
    }

}
