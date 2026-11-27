package VanillaExpansion.expand.world.block.distribution;
import arc.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.Seq;
import arc.struct.ObjectSet;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.entities.TargetPriority;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import static arc.math.Mathf.pi;
import static mindustry.Vars.*;
public class CableCar extends Block{
    protected final static Seq<CableCarBuild> tempBuilds = new Seq<>();
    public final static BasketNetwork network = new BasketNetwork();
    public float wheelSize = 4f;
    public float maxRange = 5f;
    public int maxConnections = 4;
    public TextureRegion basket;
    public TextureRegion rotator;
    public float itemSpeed = 60f;
    public int basketCount = 1;
    public int itemsPerTrip = 4;
    public CableCar(String name){
        super(name);
        configurable = true;
        update = true;
        solid = true;
        underBullets = true;
        hasItems = true;
        itemCapacity = 20;
        unloadable = false;
        group = BlockGroup.transportation;
        allowDiagonal = false;
        copyConfig = false;
        ignoreResizeConfig = true;
        diagonalConfigInventory = true;
        priority = TargetPriority.transport;
        config(Integer.class, (CableCarBuild build, Integer point) -> {
            Building other = world.build(point);
            if(other instanceof CableCarBuild ocb){
                boolean contains = build.isConnected(ocb);
                if(contains){
                    build.disconnectTo(ocb);
                }else if(linkValid(build, other) && build.validConnections() < build.maxLinks()){
                    build.connectTo(ocb);
                }
            }
        });
        config(Point2[].class, (CableCarBuild build, Point2[] value) -> {
            build.disconnectAll();
            for(Point2 p : value){
                int newPos = Point2.pack(p.x + build.tileX(), p.y + build.tileY());
                configurations.get(Integer.class).get(build, newPos);
            }
        });
    }
    @Override
    public void load(){
        super.load();
        basket = loadTex("basket");
        rotator = loadTex("rotator");
    }
    @Override
    public void init(){
        super.init();
        clipSize = Math.max(clipSize, maxRange * tilesize + wheelSize);
        updateClipRadius((maxRange + 0.5f) * tilesize);
    }
    protected TextureRegion loadTex(String suffix){
        return Core.atlas.find(name + "-" + suffix);
    }
    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        Tile tile = Vars.world.tile(x, y);
        if(tile != null){
            Lines.stroke(1.0F);
            Draw.color(Pal.placing);
            Drawf.circles((float)(x * 8) + offset, (float)(y * 8) + offset, maxRange * 8.0F);
        }
    }
    @Override
    public void handlePlacementLine(Seq<BuildPlan> plans){
        for(int i = 0; i < plans.size - 1; i++){
            var cur = plans.get(i);
            var next = plans.get(i + 1);
            if(Mathf.dst(cur.x - next.x, cur.y - next.y) <= maxRange){
                cur.config = new Point2(next.x - cur.x, next.y - cur.y);
            }
        }
    }
    public void getPotentialLinks(Tile tile, Team team, Cons<Building> others){
        tempBuilds.clear();
        ObjectSet<Building> used = new ObjectSet<>();
        if(tile.build instanceof CableCarBuild self){
            for(Link l : self.links){
                Building b = world.build(l.dx + tile.x, l.dy + tile.y);
                if(b != null) used.add(b);
            }
        }
        Boolf<Building> valid =
                other -> other != null &&
                        other.tile != tile &&
                        other.team == team &&
                        other instanceof CableCarBuild cb &&
                        !used.contains(other) &&
                        overlaps(tile.x * tilesize + offset, tile.y * tilesize + offset, other.tile, maxRange * tilesize) &&
                        cb.validConnections() < cb.maxLinks();
        Geometry.circle(tile.x, tile.y, (int)(maxRange + 2), (x, y) -> {
            Building other = world.build(x, y);
            if(valid.get(other) && !tempBuilds.contains((CableCarBuild)other)){
                tempBuilds.add((CableCarBuild)other);
            }
        });
        tempBuilds.sort((a, b) -> Float.compare(a.dst2(tile), b.dst2(tile)));
        for(CableCarBuild b : tempBuilds){
            others.get(b);
        }
    }
    protected boolean overlaps(float srcx, float srcy, Tile other, float range){
        return Intersector.overlaps(Tmp.cr1.set(srcx, srcy, range), other.getHitbox(Tmp.r1));
    }
    protected boolean overlaps(Building src, Building other, float range){
        return overlaps(src.x, src.y, other.tile, range);
    }
    protected boolean overlaps(Tile src, Tile other, float range){
        return overlaps(src.drawx(), src.drawy(), other, range);
    }
    public boolean overlaps(@Nullable Tile src, @Nullable Tile other){
        if(src == null || other == null) return true;
        return Intersector.overlaps(Tmp.cr1.set(src.worldx() + offset, src.worldy() + offset, maxRange * tilesize),
                Tmp.r1.setSize(size * tilesize).setCenter(other.worldx() + offset, other.worldy() + offset));
    }
    public boolean linkValid(Building tile, Building link){
        return linkValid(tile, link, true);
    }
    public boolean linkValid(Building tile, Building link, boolean checkMaxNodes){
        if(tile == link || !(link instanceof CableCarBuild cbb) || tile.team != link.team || !(tile instanceof CableCarBuild cbb2)) return false;
        if(overlaps(tile, link, maxRange * tilesize) || (link.block instanceof CableCar node && overlaps(link, tile, node.maxRange * tilesize))){
            if(checkMaxNodes && link.block instanceof CableCar node){
                return cbb.validConnections() < cbb.maxLinks() || cbb.isConnected(cbb2);
            }
            return true;
        }
        return false;
    }
    public void drawCable(Team team, float x1, float y1, float x2, float y2, float size1, float size2){
        final float d = Mathf.dst(x2 - x1, y2 - y1);
        final float f = Mathf.sqrt(d * d - Mathf.sqr(size2 - size1));
        final float a = size1 > size2 ? Mathf.atan2(size1 - size2, f) : (size1 < size2 ? pi - Mathf.atan2(size2 - size1, f) : Mathf.halfPi);
        final float a2 = pi - a;
        final float na = Mathf.atan2(x2 - x1, y2 - y1);
        final float ux = (x2 - x1) / d, uy = (y2 - y1) / d;
        final float t2x = rotateX(ux, uy, a) * size1 + x1;
        final float t2y = rotateY(ux, uy, a) * size1 + y1;
        final float t3x = rotateX(ux, uy, -a) * size1 + x1;
        final float t3y = rotateY(ux, uy, -a) * size1 + y1;
        final float t4x = rotateX(ux, uy, a - pi) * (-size2) + x2;
        final float t4y = rotateY(ux, uy, a - pi) * (-size2) + y2;
        final float t5x = rotateX(ux, uy, pi - a) * (-size2) + x2;
        final float t5y = rotateY(ux, uy, pi - a) * (-size2) + y2;
        Lines.stroke(2.5f, team.color.cpy().lerp(Pal.gray, 0.6f));
        Lines.line(t2x, t2y, t4x, t4y);
        Lines.line(t3x, t3y, t5x, t5y);
        ArcCompat.arc(x1, y1, size1, na - a, na + a - (2 * pi));
        ArcCompat.arc(x2, y2, size2, na - a2 + pi, na + a2 + pi - (2 * pi));
        Lines.stroke(1f, team.color.cpy().lerp(Pal.gray, 0.2f));
        Lines.line(t2x, t2y, t4x, t4y);
        Lines.line(t3x, t3y, t5x, t5y);
        ArcCompat.arc(x1, y1, size1, na - a, na + a - (2 * pi));
        ArcCompat.arc(x2, y2, size2, na - a2 + pi, na + a2 + pi - (2 * pi));
    }
    public CablePath pathOf(float x1, float y1, float x2, float y2, float size1, float size2){
        final float d = Mathf.dst(x2 - x1, y2 - y1);
        final float f = Mathf.sqrt(d * d - Mathf.sqr(size2 - size1));
        final float a = size1 > size2 ? Mathf.atan2(size1 - size2, f) : (size1 < size2 ? pi - Mathf.atan2(size2 - size1, f) : Mathf.halfPi);
        final float a2 = pi - a;
        final float na = Mathf.atan2(x2 - x1, y2 - y1);
        final float ux = (x2 - x1) / d, uy = (y2 - y1) / d;
        final float t2x = rotateX(ux, uy, a) * size1 + x1;
        final float t2y = rotateY(ux, uy, a) * size1 + y1;
        final float t3x = rotateX(ux, uy, -a) * size1 + x1;
        final float t3y = rotateY(ux, uy, -a) * size1 + y1;
        final float t4x = rotateX(ux, uy, a - pi) * (-size2) + x2;
        final float t4y = rotateY(ux, uy, a - pi) * (-size2) + y2;
        final float t5x = rotateX(ux, uy, pi - a) * (-size2) + x2;
        final float t5y = rotateY(ux, uy, pi - a) * (-size2) + y2;
        final float l1 = Math.abs(2 * a - (2 * pi)) * size1;
        final float l2 = Math.abs(2 * a2 - (2 * pi)) * size2;
        final float total = l1 + l2 + f * 2;
        Cons<Float> getPt = len -> {
            float l = Mathf.mod(len, total);
            if(l < l1){
                float ang = Mathf.lerp(na - a, na + a - (2 * pi), l / l1);
                Tmp.v6.set(Mathf.cos(ang) * size1 + x1, Mathf.sin(ang) * size1 + y1);
            }else if(l < l1 + f){
                float t = Mathf.curve(l - l1, 0, f);
                Tmp.v6.set(t2x + (t4x - t2x) * t, t2y + (t4y - t2y) * t);
            }else if(l < l1 + f + l2){
                float t = Mathf.curve(l - l1 - f, 0, l2);
                float ang = Mathf.lerp(na - a2 + pi, na + a2 + pi - (2 * pi), t);
                Tmp.v6.set(Mathf.cos(ang) * size2 + x2, Mathf.sin(ang) * size2 + y2);
            }else{
                float t = Mathf.curve(l - (l1 + f + l2), 0, f);
                Tmp.v6.set(t5x + (t3x - t5x) * t, t5y + (t3y - t5y) * t);
            }
        };
        return new CablePath(getPt, total);
    }
    private static float rotateX(float ux, float uy, float rad){
        return ux * Mathf.cos(rad) - uy * Mathf.sin(rad);
    }
    private static float rotateY(float ux, float uy, float rad){
        return ux * Mathf.sin(rad) + uy * Mathf.cos(rad);
    }
    public static class CablePath{
        public final Cons<Float> getPt;
        public final float total;
        public CablePath(Cons<Float> getPt, float total){
            this.getPt = getPt;
            this.total = total;
        }
    }
    public static class ArcCompat{
        public static void arc(float x, float y, float radius, float from, float to){
            int seg = Math.max(8, (int)(Math.abs(to - from) * radius / 4f));
            Lines.beginLine();
            for(int i = 0; i <= seg; i++){
                float a = Mathf.lerp(from, to, i / (float)seg);
                Lines.linePoint(x + Mathf.cos(a) * radius, y + Mathf.sin(a) * radius);
            }
            Lines.endLine();
        }
    }
    @Override
    public void setBars(){
        super.setBars();
        addBar("connections", entity -> {
            if(entity instanceof CableCarBuild ccb){
                return new Bar(
                        () -> Core.bundle.format("bar.powerlines", ccb.validConnections(), ccb.maxLinks()),
                        () -> Pal.items,
                        () -> (float)ccb.validConnections() / ccb.maxLinks()
                );
            }
            return null;
        });
    }
    public static class Link{
        public final int dx, dy;
        public int dir;
        public Link(int dx, int dy, int dir){
            this.dx = dx;
            this.dy = dy;
            this.dir = dir;
        }
    }
    public static class CableBasket{
        public CableCarBuild current;
        public CableCarBuild target;
        public float progress;
        public CablePath path;
        public ItemSeq items = new ItemSeq();
        public boolean moving;
        public CableBasket(CableCarBuild start){
            this.current = start;
            this.progress = 0f;
            this.moving = false;
        }
        public void update(float delta){
            if(current == null || current.tile == null || current.dead) return;
            if(target == null || target.dead){
                target = findNextTarget();
                if(target == null){
                    moving = false;
                    return;
                }
                CableCar curBlock = (CableCar)current.block;
                CableCar tgtBlock = (CableCar)target.block;
                path = curBlock.pathOf(current.x, current.y, target.x, target.y,
                        curBlock.wheelSize, tgtBlock.wheelSize);
                progress = 0f;
                moving = true;
            }

            if(path == null || path.total <= 0){
                moving = false;
                return;
            }

            float step = (((CableCar)current.block).itemSpeed * delta) / path.total;
            progress += step;

            if(progress >= 1f){
                progress = 1f;
                arrive();
            }
        }
        void arrive(){
            if(target == null) return;
            current = target;
            target = null;
            progress = 0f;
            moving = false;
            tryTrade(current);
        }
        void tryTrade(CableCarBuild node){
            for(Link l : node.links){
                if(l.dir < 0){
                    if(node.items.total() < node.block.itemCapacity){
                        int space = node.block.itemCapacity - node.items.total();
                        for(ItemStack st : items.toSeq().copy()){
                            if(space <= 0) break;
                            int amt = Math.min(st.amount, space);
                            if(amt <= 0) continue;
                            node.items.add(st.item, amt);
                            items.remove(st.item, amt);
                            space -= amt;
                        }
                    }
                    break;
                }
            }
            if(items.total < itemsPerTrip()){
                for(Link l : node.links){
                    if(l.dir > 0 && node.items.total() > 0){
                        int want = Math.min(itemsPerTrip() - items.total, node.items.total());
                        for(Item item : content.items()){
                            int have = node.items.get(item);
                            if(have <= 0) continue;
                            int take = Math.min(want, have);
                            node.items.remove(item, take);
                            items.add(item, take);
                            want -= take;
                            if(want <= 0) break;
                        }
                        break;
                    }
                }
            }
        }
        int itemsPerTrip(){
            return current == null ? 4 : ((CableCar)current.block).itemsPerTrip;
        }
        CableCarBuild findNextTarget(){
            Seq<CableCarBuild> senders = new Seq<>();
            for(Link l : current.links){
                Building other = world.build(l.dx + current.tile.x, l.dy + current.tile.y);
                if(!(other instanceof CableCarBuild ob)) continue;
                if(l.dir > 0) senders.add(ob);
            }
            if(!senders.isEmpty()) return senders.random();
            Seq<CableCarBuild> candidates = new Seq<>();
            for(Link l : current.links){
                Building other = world.build(l.dx + current.tile.x, l.dy + current.tile.y);
                if(other instanceof CableCarBuild ob) candidates.add(ob);
            }
            return candidates.isEmpty() ? null : candidates.random();
        }
        public void draw(){
            if(current == null || path == null) return;
            path.getPt.get(progress * path.total);
            float px = Tmp.v6.x;
            float py = Tmp.v6.y;
            Draw.z(Layer.power - 1f);
            TextureRegion tex = ((CableCar)current.block).basket;
            if(tex.found()){
                Draw.rect(tex, px, py, 0);
            }else{
                Draw.color(current.team.color);
                Fill.square(px, py, 3f, 0f);
                Draw.color();
            }
        }
    }
    public static class BasketNetwork{
        public final Seq<CableBasket> baskets = new Seq<>();
        private int updateId = -1;
        private int drawId = -1;
        public void add(CableBasket basket){
            baskets.add(basket);
        }
        public void remove(CableBasket basket){
            baskets.remove(basket);
        }
        public void update(float delta){
            for(int i = baskets.size - 1; i >= 0; i--){
                CableBasket b = baskets.get(i);
                if(b.current == null || b.current.dead){
                    baskets.remove(i);
                    continue;
                }
                b.update(delta);
            }
        }
        public void draw(){
            for(CableBasket b : baskets){
                b.draw();
            }
        }
        public boolean tryBeginUpdate(int id){
            if(updateId == id) return false;
            updateId = id;
            return true;
        }
        public boolean tryBeginDraw(int id){
            if(drawId == id) return false;
            drawId = id;
            return true;
        }
    }
    public class CableCarBuild extends Building{
        public Seq<Link> links = new Seq<>();
        public Seq<CableBasket> ownBaskets = new Seq<>();
        public boolean basketsInited = false;
        public int maxLinks(){
            return ((CableCar)block).maxConnections;
        }
        public int validConnections(){
            int n = 0;
            for(Link l : links){
                Building b = world.build(l.dx + tile.x, l.dy + tile.y);
                if(b instanceof CableCarBuild) n++;
            }
            return n;
        }
        public boolean isConnected(CableCarBuild other){
            int dx = other.tile.x - tile.x;
            int dy = other.tile.y - tile.y;
            for(Link l : links){
                if(l.dx == dx && l.dy == dy) return true;
            }
            return false;
        }
        public void connectTo(CableCarBuild other){
            if(isConnected(other)) return;
            links.add(new Link(other.tile.x - tile.x, other.tile.y - tile.y, 1));
            if(!other.isConnected(this)){
                other.links.add(new Link(tile.x - other.tile.x, tile.y - other.tile.y, -1));
            }
        }
        public void disconnectTo(CableCarBuild other){
            int dx = other.tile.x - tile.x;
            int dy = other.tile.y - tile.y;
            links.remove(l -> l.dx == dx && l.dy == dy);
            other.links.remove(l -> l.dx == -dx && l.dy == -dy);
        }
        public void disconnectAll(){
            for(Link l : links){
                Building b = world.build(l.dx + tile.x, l.dy + tile.y);
                if(b instanceof CableCarBuild ob){
                    ob.links.remove(x -> x.dx == -l.dx && x.dy == -l.dy);
                }
            }
            links.clear();
        }
        @Override
        public void created(){
            super.created();
            if(basketsInited) return;
            basketsInited = true;
            int count = ((CableCar)block).basketCount;
            for(int i = 0; i < count; i++){
                CableBasket b = new CableBasket(this);
                ownBaskets.add(b);
                network.add(b);
            }
        }
        @Override
        public void onRemoved(){
            cleanupBaskets();
            disconnectAll();
            super.onRemoved();
        }
        @Override
        public void onDestroyed(){
            cleanupBaskets();
            disconnectAll();
            super.onDestroyed();
        }
        void cleanupBaskets(){
            for(CableBasket b : ownBaskets){
                network.remove(b);
            }
            ownBaskets.clear();
            for(int i = network.baskets.size - 1; i >= 0; i--){
                CableBasket b = network.baskets.get(i);
                if(b.current == this || b.target == this){
                    network.baskets.remove(i);
                }
            }
        }
        @Override
        public void playerPlaced(Object config){
            super.playerPlaced(config);
            int[] total = {0};
            getPotentialLinks(tile, team, link -> {
                if(total[0]++ < 1 && link instanceof CableCarBuild cb && !isConnected(cb)){
                    connectTo(cb);
                }
            });
        }
        @Override
        public void updateTile(){
            super.updateTile();
            if(network.tryBeginUpdate(id)){
                network.update(delta());
            }
        }
        @Override
        public boolean acceptItem(Building source, Item item){
            if(!hasItems || items.total() >= itemCapacity) return false;
            if(!enabled) return false;
            if(source == null) return true;
            if(source.team != team) return false;
            if(source instanceof CableCarBuild sc){
                for(Link l : links){
                    Building b = world.build(l.dx + tile.x, l.dy + tile.y);
                    if(b == sc){
                        return l.dir < 0;
                    }
                }
            }
            return true;
        }
        @Override
        public boolean canDump(Building to, Item item){
            if(to instanceof CableCarBuild tc){
                for(Link l : links){
                    Building b = world.build(l.dx + tile.x, l.dy + tile.y);
                    if(b == tc){
                        return l.dir > 0;
                    }
                }
            }
            return true;
        }
        @Override
        public void drawSelect(){
            super.drawSelect();
            Lines.stroke(1f);
            Draw.color(Pal.accent);
            Drawf.circles(x, y, maxRange * tilesize);
            for(Link l : links){
                Building b = world.build(l.dx + tile.x, l.dy + tile.y);
                if(b != null){
                    Drawf.square(b.x, b.y, b.block.size * tilesize / 2f + 1f, l.dir > 0 ? Pal.place : Pal.accent);
                }else{
                    Drawf.square((l.dx + tile.x) * 8, (l.dy + tile.y) * 8, tilesize / 2f + 1f, l.dir > 0 ? Pal.place : Pal.accent);
                }
            }
            Draw.reset();
        }
        @Override
        public void draw(){
            Draw.rect(region, x, y, 0);
            Draw.z(Layer.blockOver);
            if(rotator.found()){
                Drawf.shadow(rotator, x - 1, y - 1, 0);
                Drawf.spinSprite(rotator, x, y, 0f);
            }
            Draw.z(Layer.power);
            for(Link l : links){
                Building b = world.build(l.dx + tile.x, l.dy + tile.y);
                if(b instanceof CableCarBuild db){
                    if(((CableCar)db.block).maxRange < maxRange || db.id < id){
                        drawCable(team, x, y, db.x, db.y, wheelSize, ((CableCar)db.block).wheelSize);
                    }
                }
            }
            Draw.z(Layer.power - 1f);
            if(network.tryBeginDraw(id)){
                network.draw();
            }
            Draw.reset();
            drawTeamTop();
        }
        @Override
        public boolean onConfigureBuildTapped(Building other){
            if(linkValid(this, other)){
                configure(other.pos());
                return false;
            }
            if(this == other){
                if(validConnections() == 0){
                    int[] total = {0};
                    getPotentialLinks(tile, team, link -> {
                        if(total[0]++ < maxLinks()){
                            configure(link.pos());
                        }
                    });
                }else{
                    Seq<Link> copy = new Seq<>(links);
                    for(Link l : copy){
                        Building b = world.build(l.dx + tile.x, l.dy + tile.y);
                        if(b != null) configure(b.pos());
                    }
                }
                deselect();
                return false;
            }

            return true;
        }
        @Override
        public void write(Writes write){
            super.write(write);
            write.i(links.size);
            for(Link l : links){
                write.i(l.dx);
                write.i(l.dy);
                write.b(l.dir);
            }
        }
        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            int n = read.i();
            links.clear();
            for(int i = 0; i < n; i++){
                links.add(new Link(read.i(), read.i(), read.b()));
            }
        }
    }
}