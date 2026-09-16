package VanillaExpansion.expand.world.block.defense;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.math.geom.Vec2;
import arc.struct.EnumSet;
import arc.struct.IntFloatMap;
import arc.struct.IntSet;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import arc.util.Tmp;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Sounds;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.RegenProjector;
import mindustry.world.blocks.power.PowerNode;
import mindustry.world.draw.DrawBlock;
import mindustry.world.draw.DrawDefault;
import mindustry.world.meta.BlockFlag;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.BlockStatus;
import mindustry.world.meta.Stat;

import static arc.graphics.Gl.points;
import static mindustry.Vars.*;
import static mindustry.gen.Iconc.link;

public class HealthTransferBlock extends Block {
    private static final IntSet taken = new IntSet();
    private static final IntFloatMap mendMap = new IntFloatMap();
    private static long lastUpdateFrame = -1;

    public HealthTransferBlock(String name) {
        super(name);
        update = true;
        schematicPriority = -10;
        configurable = true;
        ignoreResizeConfig = true;
        destructible = true;
        suppressable = true;
        saveConfig = true;
        copyConfig = true;
        sync = true;
        breakable = true;
        group = BlockGroup.projectors;
        solid = true;
        flags = EnumSet.of(BlockFlag.blockRepair);
        ambientSound = Sounds.loopRegen;
        ambientSoundVolume = 0.45f;
        config(Integer.class, (entity, value) -> {});
        config(Point2[].class, (tile, value) -> {});
    }

    public float linkRange = 20f;
    public int healRange = 24;
    public Color healColor = Pal.heal.cpy();
    public Color sacrificeColor = Color.valueOf("f25555");
    public DrawBlock drawer = new DrawDefault();
    public float transferMultiplier = 1f;
    public float transferSpeed = 5f;
    public Effect effect = Fx.mine;
    public float effectChance = 0.01f;

    private final boolean debug = false;

    @Override
    public void setBars(){
        super.setBars();

    }

    @Override
    public void setStats(){
        super.setStats();

    }

    @Override
    public void init(){
        super.init();
        clipSize = Math.max(clipSize, linkRange * tilesize);
    }

    @Override
    public void load(){
        super.load();
        drawer.load(this);
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        Tile tile = world.tile(x, y);
        Lines.stroke(1f);
        Draw.color(Pal.placing);
        Drawf.circles(x * tilesize + offset, y * tilesize + offset, linkRange * tilesize);
        if(tile != null) {
            float sx1 = tile.x * tilesize;
            float sy1 = tile.y * tilesize;
            sx1 += offset;
            sy1 += offset;
            Drawf.dashSquare(healColor, sx1, sy1, healRange * tilesize);
            indexer.eachBlock(player.team(), Tmp.r1.setCentered(sx1, sy1, healRange * tilesize), b -> true, t -> {
                Drawf.selected(t, Tmp.c1.set(healColor).a(Mathf.absin(4f, 1f)));
            });
        }
    }

    @Override
    public float planConfigClipSize(){
        return linkRange * tilesize * 2f + size * tilesize;
    }

    public class HealthTransferBuild extends Building {

        private Building linked = null;
        private float linkShineTime = 0f;

        private Seq<Vec2> healingPos = new Seq<>();
        private float linkWidth = 0f;

        private float linkX = 0f;
        private float linkY = 0f;

        @Override
        public void draw(){
            drawer.draw(this);
            if(linked != null && !linked.dead()) {
                Draw.z(Layer.bullet - 0.01f);
                Draw.color(sacrificeColor);
                Lines.stroke(0.75f + 2.25f * Mathf.clamp(linkShineTime / 15f));
                Lines.line(x, y, linked.x, linked.y);
            }
            if(healingPos != null) {
                Draw.z(Layer.bullet - 0.01f);
                Draw.color(healColor);
                for(Vec2 pos : healingPos) {
                    Lines.stroke(2f);
                    Lines.line(x, y, pos.x, pos.y);
                }
            }
        }

        @Override
        public void drawSelect(){
            super.drawSelect();
            Lines.stroke(1f);
            Draw.color(Pal.accent);
            Drawf.circles(x, y, linkRange * tilesize);
            float sx1 = tile.x * tilesize;
            float sy1 = tile.y * tilesize;
            sx1 += offset;
            sy1 += offset;
            Drawf.dashSquare(healColor, sx1, sy1, healRange * tilesize);
            indexer.eachBlock(player.team(), Tmp.r1.setCentered(sx1, sy1, healRange * tilesize), b -> b != linked, t -> {
                Drawf.selected(t, Tmp.c1.set(healColor).a(Mathf.absin(4f, 1f)));
            });
            Draw.reset();
        }
        @Override
        public void drawConfigure(){

            Drawf.circles(x, y, tile.block().size * tilesize / 2f + 1f + Mathf.absin(Time.time, 4f, 1f));

                Drawf.circles(x, y, linkRange * tilesize);

                if(linked != null) {
                    Drawf.square(linked.x, linked.y, linked.block.size * tilesize / 2f + 1f, sacrificeColor);
                }

            float sx1 = tile.x * tilesize;
            float sy1 = tile.y * tilesize;
            sx1 += offset;
            sy1 += offset;
            Drawf.dashSquare(healColor, sx1, sy1, healRange * tilesize);
            indexer.eachBlock(player.team(), Tmp.r1.setCentered(sx1, sy1, healRange * tilesize), b -> b != linked, t -> {
                Drawf.selected(t, Tmp.c1.set(healColor).a(Mathf.absin(4f, 1f)));
            });

                Draw.reset();
        }

        public Seq<Building> targets = new Seq<>();
        public int lastChange = -2;
        public float warmup, totalTime, optionalTimer;
        public boolean anyTargets = false;
        public boolean didRegen = false;

        public void updateTargets(){
            targets.clear();
            taken.clear();
            indexer.eachBlock(team, Tmp.r1.setCentered(x, y, healRange * tilesize), b -> b != linked, targets::add);
        }

        @Override
        public void updateTile() {
            if(linkShineTime > 0) linkShineTime -= delta();
            if (lastChange != world.tileChanges) {
                lastChange = world.tileChanges;
                updateTargets();
            }
            totalTime += warmup * Time.delta;
            didRegen = false;
            anyTargets = false;

            healingPos.clear();

            if(linked != null && !linkValid(tile, linked.tile)) {
                linked = null;
                linkX = 0;
                linkY = 0;
                if(debug)Log.info("clear link pos");
            }
            if(linked != null && (linkX != linked.x - x || linkY != linked.y - y)){
                linkX = linked.x - x;
                linkY = linked.y - y;
                if(debug)Log.info("link pos reset to (" + linkX + "," + linkY + ")");
            }
            if(linked == null && !(linkX == 0 && linkY == 0)){
                Tile tile = world.tileWorld(linkX + x, linkY + y);
                if(tile != null && tile.build != null && !tile.build.dead() && linkValid(this.tile, tile)){
                    linked = tile.build;
                    if(debug)Log.info("link recovered from pos (" + linkX + "," + linkY + ")");
                }
            }

            if(checkSuppression() || linked == null || linked.dead()){
                return;
            }

            anyTargets = targets.contains(b -> b.damaged());

            if(transferMultiplier <= 0) transferMultiplier = 1;

            if(efficiency > 0 && linked != null){
                for(var build : targets){
                    if(!build.damaged() || build.isHealSuppressed() || build == linked || linked == null || linked.dead()) continue;
                    float amount = transferSpeed * delta();
                    amount = Math.min((build.maxHealth() - build.health()) / transferMultiplier, amount);
                    amount = Math.min(linked.health(), amount);
                    if(Mathf.chanceDelta(effectChance * build.block.size * build.block.size)){
                        effect.at(build.x + Mathf.range(build.block.size * tilesize/2f - 1f), build.y + Mathf.range(build.block.size * tilesize/2f - 1f), healColor);
                    }
                    build.heal(amount * transferMultiplier);
                    linked.damagePierce(amount);
                    linkShineTime = 15f;
                    healingPos.add(new Vec2(build.x, build.y));
                }
            }
        }

        @Override
        public BlockStatus status() {
            if (!enabled || checkSuppression()) {
                return BlockStatus.logicDisable;
            }
            if (!anyTargets && linked != null) {
                return BlockStatus.noOutput;
            }
            if (efficiency <= 0 || linked == null) {
                return BlockStatus.noInput;
            }
            return ((state.tick / 30.0F) % 1.0F) < efficiency ? BlockStatus.active : BlockStatus.noInput;
        }

        @Override
        public boolean shouldAmbientSound() {
            return anyTargets && linked != null;
        }

        @Override
        public boolean onConfigureBuildTapped(Building other){
            //reverse connection
            if(other == linked){
                Point2[] out = new Point2[1];
                out[0] = Point2.unpack(other.pos());
                configure(out);
                linked = null;
                linkX = linkY = 0;
                if(debug)Log.info("cancel link");
                return true;
            }

            if(this != other && other != null && linkValid(tile, other.tile)){
                Point2[] out = new Point2[1];
                out[0] = Point2.unpack(other.pos());
                configure(out);
                linked = other;
                if(debug)Log.info("link building");
                return false;
            }
            return true;
        }

        @Override
        public Point2[] config(){
            Point2[] out = new Point2[1];
            out[0] = Point2.unpack(link).sub(tile.x, tile.y);
            return out;
        }

        public boolean linkValid(Tile tile, Tile other){
            if(tile == null || other == null || other.build == null || other.build.dead()
                    || (other.build.team != tile.build.team && other.build.team != Team.derelict)) return false;
            return ((other.x - tile.x) * (other.x - tile.x) + (other.y - tile.y) * (other.y - tile.y)) <= linkRange * linkRange;
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            if(linked == null || linked.dead()){
                write.f(0);
                write.d(0);
                if(debug)Log.info("link pos saved as none");
            }else{
                write.f(linkX);
                write.d(linkY);
                if(debug)Log.info("link pos saved as (" + linkX + "," + linkY + ")");
            }
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            float px = read.f();
            float py = (float) read.d();
            if(debug)Log.info("data read as (" + px + "," + py + ")");
            Time.run(1f, () -> {
                Tile tile = world.tileWorld(x + px, y + py);
                if (tile.build != null && !tile.build.dead() && tile != this.tile && linkValid(this.tile, tile)) {
                    linked = tile.build;
                    linkX = px;
                    linkY = py;
                    if(debug)Log.info("link pos set to (" + linkX + "," + linkY + ") by data");
                } else {
                    linked = null;
                    linkX = 0;
                    linkY = 0;
                    if(debug)Log.info("link pos set to none by data");
                }
            });
        }
    }
}
