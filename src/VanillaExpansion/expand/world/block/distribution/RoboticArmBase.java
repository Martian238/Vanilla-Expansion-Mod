package VanillaExpansion.expand.world.block.distribution;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class RoboticArmBase extends GenericCrafter{
    public boolean coreMerge = true;

    public RoboticArmBase(String name){
        super(name);
        hasItems = false;
        solid = true;
        update = false;
        sync = true;
        destructible = true;
        group = BlockGroup.transportation;
        envEnabled = Env.any;
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    public static void incinerateEffect(Building self, Building source){
        if(Mathf.chance(0.3)){
            Tile edge = Edges.getFacingEdge(source, self);
            Tile edge2 = Edges.getFacingEdge(self, source);
            if(edge != null && edge2 != null && self.wasVisible){
                Fx.coreBurn.at((edge.worldx() + edge2.worldx())/2f, (edge.worldy() + edge2.worldy())/2f);
            }
        }
    }

    public class RoboticArmBuild extends Building{
        public @Nullable Building linkedCore;

        @Override
        public boolean acceptItem(Building source, Item item){
            return linkedCore != null && linkedCore.acceptItem(source, item);
        }

        @Override
        public void handleItem(Building source, Item item){
            if(linkedCore != null){
                if(linkedCore.items.get(item) >= ((CoreBuild)linkedCore).storageCapacity){
                    incinerateEffect(this, source);
                }
                ((CoreBuild)linkedCore).noEffect = true;
                linkedCore.handleItem(source, item);
            }
        }

        @Override
        public void itemTaken(Item item){
            if(linkedCore != null){
                linkedCore.itemTaken(item);
            }
        }

        @Override
        public int removeStack(Item item, int amount){
            if(linkedCore != null){
                return linkedCore.removeStack(item, amount);
            }
            return 0;
        }

        @Override
        public int getMaximumAccepted(Item item){
            return linkedCore != null ? linkedCore.getMaximumAccepted(item) : 0;
        }

        @Override
        public void drawSelect(){
            if(linkedCore != null){
                linkedCore.drawSelect();
            }
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.itemCapacity && linkedCore != null) return linkedCore.sense(sensor);
            return super.sense(sensor);
        }

        @Override
        public boolean canPickup(){
            return linkedCore == null;
        }

        @Override
        public boolean allowDeposit(){
            return linkedCore != null && linkedCore.allowDeposit();
        }
    }
}
