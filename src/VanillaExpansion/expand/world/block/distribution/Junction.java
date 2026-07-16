package VanillaExpansion.expand.world.block.distribution;

import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.heat.*;
import mindustry.world.meta.*;

import java.util.Arrays;

import static mindustry.Vars.*;

public class Junction extends Block{
    public float speed = 26;
    public int capacity = 6;
    public float displayedSpeed = 13f;
    public float visualMaxHeat = 450f;

    public Junction(String name){
        super(name);
        update = true;
        solid = false;
        underBullets = true;
        group = BlockGroup.transportation;
        unloadable = false;
        noUpdateDisabled = true;
        hasLiquids = true;
        outputsLiquid = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.itemsMoved, displayedSpeed, StatUnit.itemsSecond);
        stats.add(Stat.itemCapacity, table -> {
            table.add(Strings.autoFixed(capacity, 2) + " " + StatUnit.items.localized() + " " + StatUnit.perSide.localized());
        });
        stats.add(Stat.heatCapacity, visualMaxHeat, StatUnit.heatUnits);
    }

    @Override
    public boolean outputsItems(){
        return true;
    }

    public class JunctionBuild extends Building implements HeatBlock, HeatConsumer{
        public DirectionalItemBuffer buffer = new DirectionalItemBuffer(capacity);
        public float heat = 0f;
        public float[] sideHeat = new float[4];
        public IntSet cameFrom = new IntSet();
        public long lastHeatUpdate = -1;

        @Override
        public int acceptStack(Item item, int amount, Teamc source){
            return 0;
        }

        @Override
        public void updateTile(){
            for(int i = 0; i < 4; i++){
                if(buffer.indexes[i] > 0){
                    if(buffer.indexes[i] > capacity) buffer.indexes[i] = capacity;
                    long l = buffer.buffers[i][0];
                    float time = BufferItem.time(l);

                    if(Time.time >= time + speed / timeScale || Time.time < time){

                        Item item = content.item(BufferItem.item(l));
                        Building dest = nearby(i);

                        if(item == null || dest == null || !dest.acceptItem(this, item) || dest.team != team){
                            continue;
                        }

                        dest.handleItem(this, item);
                        System.arraycopy(buffer.buffers[i], 1, buffer.buffers[i], 0, buffer.indexes[i] - 1);
                        buffer.indexes[i] --;
                    }
                }
            }

            updateHeat();
        }

        public void updateHeat(){
            if(lastHeatUpdate == state.updateId) return;

            lastHeatUpdate = state.updateId;

            Arrays.fill(sideHeat, 0f);
            cameFrom.clear();

            heat = 0f;

            for(var build : proximity){
                if(build == null || build.team != team || !(build instanceof HeatBlock heater)) continue;

                if(cameFrom.contains(build.id)) continue;
                cameFrom.add(build.id);

                if(build.block instanceof Junction){
                    continue;
                }

                float add = heater.heat();
                int dir = relativeTo(build);
                if(dir >= 0 && dir < 4){
                    sideHeat[dir] += add;
                }
                heat += add;
            }

            heat = Math.min(heat, visualMaxHeat);

            for(int i = 0; i < 4; i++){
                float inHeat = sideHeat[i];
                if(inHeat > 0){
                    int outDir = (i + 2) % 4;
                    sideHeat[i] = 0;
                    sideHeat[outDir] += inHeat;
                }
            }
        }

        @Override
        public float[] sideHeat(){
            return sideHeat;
        }

        @Override
        public float heatRequirement(){
            return visualMaxHeat;
        }

        @Override
        public float heat(){
            updateHeat();
            return heat;
        }

        @Override
        public float heatFrac(){
            return heat / visualMaxHeat;
        }

        @Override
        public void handleItem(Building source, Item item){
            int relative = source.relativeTo(tile);
            buffer.accept(relative, item);
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            int relative = source.relativeTo(tile);

            if(relative == -1 || !buffer.accepts(relative)) return false;
            Building to = nearby(relative);
            return to != null && to.team == team;
        }

        @Override
        public Building getLiquidDestination(Building source, Liquid liquid){
            if(!enabled) return this;

            int dir = (source.relativeTo(tile.x, tile.y) + 4) % 4;
            Building next = nearby(dir);
            if(next == null || (!next.acceptLiquid(this, liquid) && !(next.block instanceof Junction))){
                return this;
            }
            return next.getLiquidDestination(this, liquid);
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            buffer.write(write);
            write.f(heat);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            buffer.read(read, revision == 0);
            heat = read.f();
        }
    }
}