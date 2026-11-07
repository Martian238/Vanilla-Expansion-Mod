package VanillaExpansion.expand.world.block;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.Vars;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Sounds;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.meta.BuildVisibility;

import static mindustry.Vars.*;


//代码来源：https://github.com/Hollow-YK/Endfield_Industry_Lib/blob/master/src/endfieldindustrylib/EFworld/blocks/AICBasicFacility/RectGenericAICBasicFacility.java

public class RectangleBlock extends Block {
    public RectangleBlock(String name){
        super(name);
        rotate = true;
        rotateDraw = true;
        rotateDrawEditor = true;
        solid = true;
        update = true;
    }

    /** 0角度x方向边长 */
    public int width = 1;
    /** 0角度y方向边长 */
    public int height = 1;
    /** 特殊输出方式 */
    public boolean specialDump = false;

    /** 子方块 */
    public static class RectangleTile extends Block {
        public RectangleTile(String name){
            super(name);
            update = true;
            solid = true;
            configurable = false;
            buildVisibility = BuildVisibility.hidden;
            ambientSound = Sounds.none;
            placeablePlayer = false;
            health = -1;
            targetable = false;
            buildType = RectangleTileBuild::new;
            destructible = false;
            breakable = false;
            createRubble = false;
        }

        @Override
        public void load(){
            super.load();
            region = Core.atlas.find("clear");
        }

        public static class RectangleTileBuild extends Building {
            public Building owner;

            @Override
            public void update(){
                super.update();
                if(owner == null){
                    tile.setAir();
                }else if(!owner.isAdded()){
                    tile.setAir();
                }else if(world.tile(owner.pos()) != owner.tile){
                    tile.setAir();
                }
            }

            public void setOwner(Building owner){
                this.owner = owner;
            }

            public boolean shouldHide(){
                return true;
            }

            @Override
            public void drawSelect(){
                owner.drawSelect();
            }

            @Override
            public void draw(){

            }

            //传递方法
            @Override
            public boolean acceptItem(Building source, Item item) {
                return owner != null && owner.acceptItem(source, item);
            }
            @Override
            public void handleItem(Building source, Item item) {
                if (owner != null)
                    owner.handleItem(source, item);
            }
            @Override
            public boolean acceptLiquid(Building source, Liquid liquid) {
                return owner != null && owner.acceptLiquid(source, liquid);
            }
            @Override
            public void handleLiquid(Building source, Liquid liquid, float amount) {
                if (owner != null)
                    owner.handleLiquid(source, liquid, amount);
            }
            @Override
            public float handleDamage(float amount) {
                return owner != null ? owner.handleDamage(amount) : amount;
            }
            @Override
            public void damage(float damage) {
                if (owner != null)
                    owner.damage(damage);
            }
            @Override
            public void tapped() {
                if (owner != null) {
                    if (Vars.control.input.config.getSelected() == owner) {
                        Vars.control.input.config.hideConfig();
                    } else {
                        Vars.control.input.config.showConfig(owner);
                    }
                }
            }
            @Override
            public void buildConfiguration(Table table) {
                if (owner != null)
                    owner.buildConfiguration(table);
            }
            @Override
            public boolean inFogTo(Team viewer) {
                if(owner != null)
                    return owner.inFogTo(viewer);
                else return true;
            }

            @Override
            public void write(Writes write) {
                super.write(write);
                write.i(owner == null ? -1 : owner.pos());
            }

            @Override
            public void read(Reads read, byte revision) {
                super.read(read, revision);
                int pos = read.i();
                if (pos != -1) {
                    owner = world.build(pos);
                }
            }
        }
    }

    public static RectangleTile rectTile;

    public static void registerRectangleTile(){
        rectTile = new RectangleTile("rect-tile");
        rectTile.load();
    }

    public class RectangleBuild extends Building {
        public Seq<Building> tiles = new Seq<>();

        @Override
        public void placed(){
            super.placed();
            createTiles();
        }

        public void createTiles(){
            int w = rotation % 2 == 0 ? width : height;
            int h = rotation % 2 == 0 ? height : width;
            int minX = tileX() - (w % 2 == 0 ? w / 2 -1 : w / 2);
            int maxX = tileX() + (w / 2);
            int minY = tileY() - (h % 2 == 0 ? h / 2 -1 : h / 2);
            int maxY = tileY() + (h / 2);
            for(int x= minX;x<=maxX;x++){
                for(int y = minY; y<=maxY;y++){
                    // 跳过主方块占据的区域
                    if (this==world.tile(x,y).build) continue;

                    Tile childTile = world.tile(x,y);
                    if (childTile == null)
                        continue;
                    // 放置子方块
                    childTile.setBlock(rectTile, team, rotation);
                    // 确保建筑类型正确，防止存档加载时的类型不匹配
                    if (childTile.build instanceof RectangleTile.RectangleTileBuild tb) {
                        tb.setOwner(this);
                        tiles.add(tb);
                    } else {
                        // 如果类型不正确，可能是旧存档的建筑，强制替换
                        if (childTile.build != null) {
                            childTile.setAir();
                        }
                        childTile.setBlock(rectTile, team, rotation);
                        if (childTile.build instanceof RectangleTile.RectangleTileBuild tb) {
                            tb.setOwner(this);
                            tiles.add(tb);
                        } else {
                            // 如果仍然不是正确类型，记录错误但继续
                            Log.info("RectangleBlock err");
                        }
                    }
                }
            }
        }

        @Override
        public void onRemoved(){
            for(Building b : tiles){
                if(b.isAdded()) b.tile.setAir();
            }
            super.onRemoved();
        }

        private void ensureChildren() {
            boolean rotated = rotation % 2 != 0;
            int w = rotated ? height : width;
            int h = rotated ? width : height;

            int minX = tileX() - (w % 2 == 0 ? w / 2 - 1 : w / 2);
            int maxX = tileX() + (w / 2);
            int minY = tileY() - (h % 2 == 0 ? h / 2 - 1 : h / 2);
            int maxY = tileY() + (h / 2);

            Seq<Building> newTiles = new Seq<>();

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    // 跳过主方块自身
                    if (x == tileX() && y == tileY()) continue;

                    Tile t = world.tile(x, y);
                    if (t == null) continue;

                    Building b = t.build;
                    // 如果当前建筑已经是正确的子方块，直接保留
                    if (b instanceof RectangleTile.RectangleTileBuild && ((RectangleTile.RectangleTileBuild) b).owner == this) {
                        newTiles.add(b);
                    } else {
                        createTiles(); // 重新创建子方块
                        break; // 放置后跳出内层循环，避免重复放置
                    }
                }
            }

            tiles = newTiles;
        }

        // 可选：在绘制时添加整个矩形的边框效果
        @Override
        public void drawConfigure() {
            int w = rotation % 2 == 0 ? width : height;
            int h = rotation % 2 == 0 ? height : width;
            float offX = w * tilesize / 2f;
            float offY = h * tilesize / 2f;
            Draw.color(Pal.accent);
            Lines.stroke(1.0F);
            Lines.rect(x - offX, y - offY, w * tilesize, h * tilesize);
            Draw.reset();
        }

        @Override
        public void drawSelect() {
            super.drawSelect();
            Draw.color(Pal.accent);
            Lines.stroke(1.0F);
            int w = rotation % 2 == 0 ? width : height;
            int h = rotation % 2 == 0 ? height : width;
            float offX = w * tilesize / 2f;
            float offY = h * tilesize / 2f;
            Lines.rect(x - offX, y - offY, w * tilesize, h * tilesize);
        }
        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            createTiles(); // 修复子块引用
        }

        //获取主方块和子方块的所有相邻方块，排除其中的自身成分并输出序列
        public Seq<Building> rectProximity(){
            Seq<Building> result = proximity;
            if(!tiles.isEmpty()){
                for(Building rt : tiles){
                    for(Building p : rt.proximity){
                        if(p.isAdded() && !result.contains(p)) result.add(p);
                    }
                }
            }
            for(Building r : result){
                if(tiles.contains(r) || r == this){
                    result.remove(r);
                }
            }
            return result;
        }

        //用新的相邻方块序列重写
        @Override
        public boolean dump(Item todump) {
            Seq<Building> proximity = rectProximity();
            if (!block.hasItems || items.total() == 0 || proximity.size == 0 || (todump != null && !items.has(todump))) return false;
            int dump = this.cdump;
            var allItems = content.items();
            int itemSize = allItems.size;
            Object[] itemArray = allItems.items;
            if (todump == null) {
                for (int i = 0; i < proximity.size; i++) {
                    Building other = proximity.get((i + dump) % proximity.size);
                    for (int ii = 0; ii < itemSize; ii++) {
                        if (!items.has(ii)) continue;
                        Item item = (Item)itemArray[ii];
                        if (other.acceptItem(this, item) && canDump(other, item)) {
                            other.handleItem(this, item);
                            items.remove(item, 1);
                            incrementDump(proximity.size);
                            return true;
                        }
                    }
                    incrementDump(proximity.size);
                }
            } else {
                for (int i = 0; i < proximity.size; i++) {
                    Building other = proximity.get((i + dump) % proximity.size);
                    if (other.acceptItem(this, todump) && canDump(other, todump)) {
                        other.handleItem(this, todump);
                        items.remove(todump, 1);
                        incrementDump(proximity.size);
                        return true;
                    }
                    incrementDump(proximity.size);
                }
            }
            return false;
        }

    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation) {

        int w = rotation % 2 == 0 ? width  : height ;
        int h = rotation % 2 == 0 ? height  : width ;
        int minX = tile.x - (w % 2 == 0 ? w / 2 -1 : w / 2);
        int maxX = tile.x + (w / 2);
        int minY = tile.y - (h % 2 == 0 ? h / 2 -1 : h / 2);
        int maxY = tile.y + (h / 2);
        for (int x =minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                Tile other = world.tile(x, y);
                if (other == null
                        || other.block().solid /* || !other.team().data().canPlace(other.x, other.y, team) */ ) {
                    return false;
                }
            }
        }
        return super.canPlaceOn(tile, team, rotation);

    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        int w = rotation % 2 == 0 ? width : height;
        int h = rotation % 2 == 0 ? height : width;
        float wx = size%2==0?x * tilesize+4:x*tilesize;
        float wy = size%2==0?y * tilesize+4:x*tilesize;
        float offX = w * tilesize / 2f;
        float offY = h * tilesize / 2f;
        Drawf.dashRect(valid ? Pal.accent : Pal.remove, wx - offX, wy - offY, w * tilesize,
                h * tilesize);
    }
}
