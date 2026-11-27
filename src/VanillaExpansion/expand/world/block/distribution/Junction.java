package VanillaExpansion.expand.world.block.distribution;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import static mindustry.Vars.*;
public class Junction extends Block{
    public float speed = 6;
    public int capacity = 6;
    public float displayedSpeed = 6f;
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
    public boolean canReplace(Block other){
        if(other.alwaysReplace) return true;
        if(other.privileged) return false;
        return other.replaceable && (other != this || (rotate && quickRotate)) && ((this.group != BlockGroup.none && other.group == this.group) || other == this || other.group == BlockGroup.liquids) &&
            (size == other.size || (size >= other.size && ((subclass != null && subclass == other.subclass) || group.anyReplace)));
    }
    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.itemsMoved, displayedSpeed, StatUnit.itemsSecond);
        stats.add(Stat.itemCapacity, table -> {
            table.add(Strings.autoFixed(capacity, 2) + " " + StatUnit.items.localized() + " " + StatUnit.perSide.localized());
        });
    }
    @Override
    public boolean outputsItems(){
        return true;
    }
    public class JunctionBuild extends Building{
        public DirectionalItemBuffer buffer = new DirectionalItemBuffer(capacity);
        @Override
        public boolean canBeReplaced(Block other){
            return super.canBeReplaced(other) || other.group == BlockGroup.liquids;
        }
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
        }
        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            buffer.read(read, revision == 0);
        }
    }
}