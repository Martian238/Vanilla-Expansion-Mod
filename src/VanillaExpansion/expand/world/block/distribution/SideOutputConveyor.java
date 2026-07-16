package VanillaExpansion.expand.world.block.distribution;

import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import VanillaExpansion.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class SideOutputConveyor extends Block implements Autotiler{
    private static final float itemSpace = 0.4f;
    private static final int capacity = 3;

    public @Load(value = "@-#1-#2", lengths = {7, 4}) TextureRegion[][] regions;

    public float speed = 0f;
    public float displayedSpeed = 0f;
    public boolean pushUnits = true;

    public @Nullable Block junctionReplacement, bridgeReplacement;

    public SideOutputConveyor(String name){
        super(name);
        rotate = true;
        update = true;
        group = BlockGroup.transportation;
        hasItems = true;
        itemCapacity = capacity;
        priority = TargetPriority.transport;
        conveyorPlacement = true;
        underBullets = true;

        ambientSound = Sounds.loopConveyor;
        ambientSoundVolume = 0.0022f;
        unloadable = false;
        noUpdateDisabled = false;
    }

    @Override
    public void setStats(){
        super.setStats();

        //have to add a custom calculated speed, since the actual movement speed is apparently not linear
        stats.add(Stat.itemsMoved, displayedSpeed, StatUnit.itemsSecond);
    }

    @Override
    public void init(){
        super.init();

        if(junctionReplacement == null) junctionReplacement = Blocks.junction;
        if(bridgeReplacement == null || !(bridgeReplacement instanceof ItemBridge || bridgeReplacement instanceof DuctBridge)) bridgeReplacement = Blocks.itemBridge;
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        int[] bits = getTiling(plan, list);

        if(bits == null || regions == null) return;

        TextureRegion region = regions[bits[0]][0];
        Draw.rect(region, plan.drawx(), plan.drawy(), region.width * bits[1] * region.scl(), region.height * bits[2] * region.scl(), plan.rotation * 90);
    }

    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock){
        return (otherblock.outputsItems() || (lookingAt(tile, rotation, otherx, othery, otherblock) && otherblock.hasItems))
            && lookingAtEither(tile, rotation, otherx, othery, otherrot, otherblock);
    }

    //stack conveyors should be bridged over, not replaced
    @Override
    public boolean canReplace(Block other){
        return super.canReplace(other) && !(other instanceof StackConveyor);
    }

    @Override
    public void handlePlacementLine(Seq<BuildPlan> plans){
        if(bridgeReplacement == null) return;
        boolean hasJuntionReplacement = junctionReplacement != null;
        if(bridgeReplacement instanceof DuctBridge bridge) Placement.calculateBridges(plans, bridge, hasJuntionReplacement, b -> b instanceof Duct || b instanceof SideOutputConveyor);
        if(bridgeReplacement instanceof ItemBridge bridge) Placement.calculateBridges(plans, bridge, hasJuntionReplacement, b -> b instanceof SideOutputConveyor);
    }

    @Override
    public TextureRegion[] icons(){
        return regions == null ? super.icons() : new TextureRegion[]{regions[0][0]};
    }

    @Override
    public boolean isAccessible(){
        return true;
    }

    @Override
    public Block getReplacement(BuildPlan req, Seq<BuildPlan> plans){
        if(junctionReplacement == null) return this;

        Boolf<Point2> cont = p -> plans.contains(o -> o.x == req.x + p.x && o.y == req.y + p.y && (req.block instanceof SideOutputConveyor || req.block instanceof Junction));
        return cont.get(Geometry.d4(req.rotation)) &&
            cont.get(Geometry.d4(req.rotation - 2)) &&
            req.tile() != null &&
            req.tile().block() instanceof SideOutputConveyor &&
            Mathf.mod(req.tile().build.rotation - req.rotation, 2) == 1 ? junctionReplacement : this;
    }

    public class ConveyorBuild extends Building implements ChainedBuilding {
        //parallel array data
        public Item[] ids = new Item[capacity];
        public float[] xs = new float[capacity], ys = new float[capacity];
        //amount of items, always < capacity
        public int len = 0;
        //next entity
        public @Nullable Building next;
        public @Nullable ConveyorBuild nextc;
        //whether the next conveyor's rotation == tile rotation
        public boolean aligned;

        public int lastInserted, mid;
        public float minitem = 1;

        public int blendbits, blending;
        public int blendsclx = 1, blendscly = 1;

        public float clogHeat = 0f;

        //side output directions
        public @Nullable Building leftSide;
        public @Nullable Building rightSide;

        //side output animation data: -1 = no animation, 0 = animating left, 1 = animating right
        public int[] sideOutputAnim = new int[capacity];
        public float[] sideOutputProgress = new float[capacity];

        {
            for(int i = 0; i < capacity; i++){
                sideOutputAnim[i] = -1;
            }
        }

        @Override
        public void draw(){
            int frame = enabled && clogHeat <= 0.5f ? (int)(((Time.time * speed * 8f * timeScale * efficiency)) % 4) : 0;

            //draw extra conveyors facing this one for non-square tiling purposes
            if(regions != null){
                Draw.z(Layer.blockUnder);
                for(int i = 0; i < 4; i++){
                    if((blending & (1 << i)) != 0){
                        int dir = rotation - i;
                        float rot = i == 0 ? rotation * 90 : (dir)*90;

                        Draw.rect(sliced(regions[0][frame], i != 0 ? SliceMode.bottom : SliceMode.top), x + Geometry.d4x(dir) * tilesize*0.75f, y + Geometry.d4y(dir) * tilesize*0.75f, rot);
                    }
                }

                Draw.z(Layer.block - 0.2f);

                Draw.rect(regions[blendbits][frame], x, y, tilesize * blendsclx, tilesize * blendscly, rotation * 90);
            }

            Draw.z(Layer.block - 0.1f);
            float layer = Layer.block - 0.1f, wwidth = world.unitWidth(), wheight = world.unitHeight(), scaling = 0.01f;

            for(int i = 0; i < len; i++){
                Item item = ids[i];
                if(item == null || item.fullIcon == null) continue;

                Tmp.v1.trns(rotation * 90, tilesize, 0);
                Tmp.v2.trns(rotation * 90, -tilesize / 2f, xs[i] * tilesize / 2f);

                float ix, iy;

                //handle side output animation
                if(sideOutputAnim[i] != -1){
                    //item moves sideways during animation
                    float progress = sideOutputProgress[i];
                    float sideDir = sideOutputAnim[i] == 1 ? 1f : -1f;
                    Tmp.v3.trns(rotation * 90 + 90f * sideDir, progress * tilesize * 0.5f);
                    ix = (x + Tmp.v1.x * 0.5f + Tmp.v2.x + Tmp.v3.x);
                    iy = (y + Tmp.v1.y * 0.5f + Tmp.v2.y + Tmp.v3.y);
                }else{
                    ix = (x + Tmp.v1.x * ys[i] + Tmp.v2.x);
                    iy = (y + Tmp.v1.y * ys[i] + Tmp.v2.y);
                }

                //keep draw position deterministic.
                Draw.z(layer + (ix / wwidth + iy / wheight) * scaling);
                Draw.rect(item.fullIcon, ix, iy, itemSize, itemSize);
            }
        }

        @Override
        public void payloadDraw(){
            Draw.rect(block.fullIcon, x, y);
        }

        @Override
        public void drawCracks(){
            Draw.z(Layer.block - 0.15f);
            super.drawCracks();
        }

        @Override
        public void overwrote(Seq<Building> builds){
            if(builds.first() instanceof ConveyorBuild build){
                ids = build.ids.clone();
                xs = build.xs.clone();
                ys = build.ys.clone();
                len = build.len;
                clogHeat = build.clogHeat;
                lastInserted = build.lastInserted;
                mid = build.mid;
                minitem = build.minitem;
                items.add(build.items);
                sideOutputAnim = build.sideOutputAnim.clone();
                sideOutputProgress = build.sideOutputProgress.clone();
            }
        }

        @Override
        public boolean shouldAmbientSound(){
            return clogHeat <= 0.5f;
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();

            int[] bits = buildBlending(tile, rotation, null, true);
            blendbits = bits[0];
            blendsclx = bits[1];
            blendscly = bits[2];
            blending = bits[4];

            next = front();
            nextc = next instanceof ConveyorBuild && next.team == team ? (ConveyorBuild)next : null;
            aligned = nextc != null && rotation == next.rotation;
            
            //get side buildings (left and right relative to conveyor direction)
            int leftDir = Mathf.mod(rotation - 1, 4);
            int rightDir = Mathf.mod(rotation + 1, 4);
            
            leftSide = tile.nearbyBuild(leftDir);
            rightSide = tile.nearbyBuild(rightDir);
        }

        @Override
        public void unitOn(Unit unit){

            if(!pushUnits || clogHeat > 0.5f || !enabled) return;

            noSleep();

            float mspeed = speed * tilesize * 55f;
            float centerSpeed = 0.1f;
            float centerDstScl = 3f;
            float tx = Geometry.d4x(rotation), ty = Geometry.d4y(rotation);

            float centerx = 0f, centery = 0f;

            if(Math.abs(tx) > Math.abs(ty)){
                centery = Mathf.clamp((y - unit.y()) / centerDstScl, -centerSpeed, centerSpeed);
                if(Math.abs(y - unit.y()) < 1f) centery = 0f;
            }else{
                centerx = Mathf.clamp((x - unit.x()) / centerDstScl, -centerSpeed, centerSpeed);
                if(Math.abs(x - unit.x()) < 1f) centerx = 0f;
            }

            if(len * itemSpace < 0.9f){
                unit.impulse((tx * mspeed + centerx) * delta(), (ty * mspeed + centery) * delta());
            }
        }

        @Override
        public void updateTile(){
            minitem = 1f;
            mid = 0;

            //skip updates if possible
            if(len == 0 && Mathf.equal(timeScale, 1f)){
                clogHeat = 0f;
                sleep();
                return;
            }

            float nextMax = aligned ? 1f - Math.max(itemSpace - nextc.minitem, 0) : 1f;
            float moved = speed * edelta();

            for(int i = len - 1; i >= 0; i--){
                //handle side output animation
                if(sideOutputAnim[i] != -1){
                    sideOutputProgress[i] += moved * 3f; //animation speed
                    if(sideOutputProgress[i] >= 1f){
                        //animation complete, actually transfer the item
                        if(sideOutputAnim[i] == 0 && leftSide != null && leftSide.team == team){
                            leftSide.handleItem(this, ids[i]);
                        }else if(sideOutputAnim[i] == 1 && rightSide != null && rightSide.team == team){
                            rightSide.handleItem(this, ids[i]);
                        }
                        items.remove(ids[i], 1);
                        sideOutputAnim[i] = -1;
                        sideOutputProgress[i] = 0f;
                        remove(i);
                    }
                    continue;
                }

                float nextpos = (i == len - 1 ? 100f : ys[i + 1]) - itemSpace;
                float maxmove = Mathf.clamp(nextpos - ys[i], 0, moved);

                ys[i] += maxmove;

                if(ys[i] > nextMax) ys[i] = nextMax;
                if(ys[i] > 0.5 && i > 0) mid = i - 1;
                xs[i] = Mathf.approach(xs[i], 0, moved*2);

                //always try side output first (priority: right > left)
                if(ys[i] > 0){ //only after item has entered the conveyor a bit
                    int dir = trySideOutputDirection(ids[i]);
                    if(dir != -1){
                        sideOutputAnim[i] = dir;
                        sideOutputProgress[i] = 0f;
                        continue;
                    }
                }

                if(ys[i] >= 1f && pass(ids[i])){
                    //align X position if passing forwards
                    if(aligned){
                        nextc.xs[nextc.lastInserted] = xs[i];
                    }
                    //remove last item
                    items.remove(ids[i], len - i);
                    len = Math.min(i, len);
                }else if(ys[i] < minitem){
                    minitem = ys[i];
                }
            }

            if(minitem < itemSpace + (blendbits == 1 ? 0.3f : 0f)){
                clogHeat = Mathf.approachDelta(clogHeat, 1f, 1f / 60f);
            }else{
                clogHeat = 0f;
            }

            noSleep();
        }

        public boolean pass(Item item){
            if(item != null && next != null && next.team == team && next.acceptItem(this, item)){
                next.handleItem(this, item);
                return true;
            }
            return false;
        }

        //returns: -1 = no side output, 0 = left, 1 = right
        public int trySideOutputDirection(Item item){
            if(rightSide != null && rightSide.team == team && rightSide.block.group != BlockGroup.transportation && rightSide.acceptItem(this, item)){
                return 1;
            }
            if(leftSide != null && leftSide.team == team && leftSide.block.group != BlockGroup.transportation && leftSide.acceptItem(this, item)){
                return 0;
            }
            return -1;
        }

        public boolean trySideOutput(Item item){
            //try right side first (but not to transportation blocks)
            if(rightSide != null && rightSide.team == team && rightSide.block.group != BlockGroup.transportation && rightSide.acceptItem(this, item)){
                rightSide.handleItem(this, item);
                return true;
            }
            //then try left side (but not to transportation blocks)
            if(leftSide != null && leftSide.team == team && leftSide.block.group != BlockGroup.transportation && leftSide.acceptItem(this, item)){
                leftSide.handleItem(this, item);
                return true;
            }
            return false;
        }

        @Override
        public int removeStack(Item item, int amount){
            noSleep();
            int removed = 0;

            for(int j = 0; j < amount; j++){
                for(int i = 0; i < len; i++){
                    if(ids[i] == item){
                        remove(i);
                        removed ++;
                        break;
                    }
                }
            }

            items.remove(item, removed);
            return removed;
        }

        @Override
        public void getStackOffset(Item item, Vec2 trns){
            trns.trns(rotdeg() + 180f, tilesize / 2f);
        }

        @Override
        public int acceptStack(Item item, int amount, Teamc source){
            return Math.min((int)(minitem / itemSpace), amount);
        }

        @Override
        public void handleStack(Item item, int amount, Teamc source){
            amount = Math.min(amount, capacity - len);

            for(int i = amount - 1; i >= 0; i--){
                add(0);
                xs[0] = 0;
                ys[0] = i * itemSpace;
                ids[0] = item;
                items.add(item, 1);
            }

            noSleep();
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            if(len >= capacity) return false;
            Tile facing = Edges.getFacingEdge(source.tile, tile);
            if(facing == null) return false;
            int direction = Math.abs(facing.relativeTo(tile.x, tile.y) - rotation);
            return (((direction == 0) && minitem >= itemSpace) || ((direction % 2 == 1) && minitem > 0.7f)) && !(source.block.rotate && next == source);
        }

        @Override
        public void handleItem(Building source, Item item){
            if(len >= capacity) return;

            int r = rotation;
            Tile facing = Edges.getFacingEdge(source.tile, tile);
            int ang = ((facing.relativeTo(tile.x, tile.y) - r));
            float x = (ang == -1 || ang == 3) ? 1 : (ang == 1 || ang == -3) ? -1 : 0;

            noSleep();
            items.add(item, 1);

            if(Math.abs(facing.relativeTo(tile.x, tile.y) - r) == 0){ //idx = 0
                add(0);
                xs[0] = x;
                ys[0] = 0;
                ids[0] = item;
            }else{ //idx = mid
                add(mid);
                xs[mid] = x;
                ys[mid] = 0.5f;
                ids[mid] = item;
            }
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.i(len);

            for(int i = 0; i < len; i++){
                write.s(ids[i].id);
                write.b((byte)(xs[i] * 127));
                write.b((byte)(ys[i] * 255 - 128));
            }
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            int amount = read.i();
            len = Math.min(amount, capacity);

            for(int i = 0; i < amount; i++){
                short id;
                float x, y;

                if(revision == 0){
                    int val = read.i();
                    id = (short)(((byte)(val >> 24)) & 0xff);
                    x = (float)((byte)(val >> 16)) / 127f;
                    y = ((float)((byte)(val >> 8)) + 128f) / 255f;
                }else{
                    id = read.s();
                    x = (float)read.b() / 127f;
                    y = ((float)read.b() + 128f) / 255f;
                }

                if(i < capacity){
                    ids[i] = content.item(id);
                    xs[i] = x;
                    ys[i] = y;
                }
            }

            //this updates some state
            updateTile();
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.progress){
                if(len == 0) return 0;
                return ys[len - 1];
            }
            return super.sense(sensor);
        }

        @Override
        public Object senseObject(LAccess sensor){
            if(sensor == LAccess.firstItem && len > 0) return ids[len - 1];
            return super.senseObject(sensor);
        }

        @Override
        public void setProp(UnlockableContent content, double value){
            if(content instanceof Item item && items != null){
                int amount = Math.min((int)value, capacity);
                if(items.get(item) != amount){
                    if(items.get(item) < amount){
                        handleStack(item, amount - items.get(item), null);
                    }else if(amount >= 0){
                        removeStack(item, items.get(item) - amount);
                    }
                }
            }else super.setProp(content, value);
        }

        public final void add(int o){
            for(int i = Math.max(o + 1, len); i > o; i--){
                ids[i] = ids[i - 1];
                xs[i] = xs[i - 1];
                ys[i] = ys[i - 1];
                sideOutputAnim[i] = sideOutputAnim[i - 1];
                sideOutputProgress[i] = sideOutputProgress[i - 1];
            }

            sideOutputAnim[o] = -1;
            sideOutputProgress[o] = 0f;
            len++;
        }

        public final void remove(int o){
            for(int i = o; i < len - 1; i++){
                ids[i] = ids[i + 1];
                xs[i] = xs[i + 1];
                ys[i] = ys[i + 1];
                sideOutputAnim[i] = sideOutputAnim[i + 1];
                sideOutputProgress[i] = sideOutputProgress[i + 1];
            }

            len--;
        }

        @Nullable
        @Override
        public Building next(){
            return nextc;
        }
    }
}
