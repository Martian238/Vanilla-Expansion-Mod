package VanillaExpansion.expand.world.block.liquid;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.liquid.*;
import mindustry.world.meta.*;
public class LiquidOverflowGate extends LiquidBlock{
    public float speed = 1f;
    public boolean invert = false;
    public LiquidOverflowGate(String name){
        super(name);

        group = BlockGroup.liquids;
        update = true;
        hasItems = false;
        conveyorPlacement = true;
        unloadable = false;
        itemCapacity = 0;
        noUpdateDisabled = true;
        rotate = true;
        underBullets = true;
        regionRotated1 = 1;
    }
    @Override
    public boolean rotatedOutput(int x, int y){
        return false;
    }

    public class LiquidOverflowGateBuild extends LiquidBuild{
        public byte overflowTimer;

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            return liquids.currentAmount() < block.liquidCapacity - 0.001f &&
                Edges.getFacingEdge(source.tile, tile).relativeTo(tile) == rotation;
        }

        @Override
        public void handleLiquid(Building source, Liquid liquid, float amount){
            super.handleLiquid(source, liquid, amount);
            noSleep();
        }

        @Override
        public void updateTile(){
            if(liquids.currentAmount() <= 0.001f) return;

            Liquid liquid = liquids.current();
            float amount = liquids.get(liquid);
            Building a = left(), b = right(), front = front();

            if(!invert){
                if(front != null && front.liquids.get(liquid) < front.block.liquidCapacity * 0.9f){
                    overflowTimer = 0;
                    transferLiquid(front, amount, liquid);
                }else{
                    if(overflowTimer < 10){
                        overflowTimer++;
                    }else{
                        Building first = cdump == 0 ? a : b;
                        Building second = cdump == 0 ? b : a;
                        if(first != null && first.liquids.get(liquid) < first.block.liquidCapacity * 0.9f){
                            transferLiquid(first, liquids.get(liquid), liquid);
                        }
                        if(second != null && liquids.currentAmount() > 0.001f && second.liquids.get(liquid) < second.block.liquidCapacity * 0.9f){
                            transferLiquid(second, liquids.get(liquid), liquid);
                        }
                        cdump = (byte)(cdump == 0 ? 2 : 0);
                    }
                }
            }else{
                boolean bothSidesFull = (a == null || a.liquids.get(liquid) >= a.block.liquidCapacity * 0.9f) &&
                                        (b == null || b.liquids.get(liquid) >= b.block.liquidCapacity * 0.9f);

                if(!bothSidesFull){
                    overflowTimer = 0;
                    Building first = cdump == 0 ? a : b;
                    Building second = cdump == 0 ? b : a;
                    if(first != null && first.liquids.get(liquid) < first.block.liquidCapacity * 0.9f){
                        transferLiquid(first, amount, liquid);
                    }
                    if(second != null && liquids.currentAmount() > 0.001f && second.liquids.get(liquid) < second.block.liquidCapacity * 0.9f){
                        transferLiquid(second, liquids.get(liquid), liquid);
                    }
                    cdump = (byte)(cdump == 0 ? 2 : 0);
                }else{
                    if(overflowTimer < 10){
                        overflowTimer++;
                    }else if(front != null && front.liquids.get(liquid) < front.block.liquidCapacity * 0.9f){
                        transferLiquid(front, liquids.get(liquid), liquid);
                    }
                }
            }
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.b(overflowTimer);
        }

        @Override
        public byte version(){
            return 2;
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 2){
                overflowTimer = read.b();
            }
        }
    }
}
