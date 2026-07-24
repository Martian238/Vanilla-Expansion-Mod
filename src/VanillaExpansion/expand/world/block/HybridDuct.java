package VanillaExpansion.expand.world.block;

import arc.graphics.g2d.Draw;
import arc.math.Mathf;

import arc.math.geom.Geometry;
import arc.util.Tmp;
import mindustry.gen.Building;
import mindustry.graphics.Layer;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.Duct;

import static mindustry.Vars.itemSize;
import static mindustry.Vars.tilesize;


public class HybridDuct extends Duct {

    public final int timerFlow = timers++;
    public boolean leaks = true;

    public HybridDuct(String name) {
        super(name);
        speed = 0.5f;
        hasLiquids = true;
        liquidCapacity = 10f;

        buildType = HybridDuctBuild::new;

    }
    @Override
    public boolean blends(Tile tile, int rotation, int otherx, int othery, int otherrot, Block otherblock) {

        boolean itemBlend = (otherblock.outputsItems() ||
                (lookingAt(tile, rotation, otherx, othery, otherblock) && otherblock.hasItems))
                && lookingAtEither(tile, rotation, otherx, othery, otherrot, otherblock);

        boolean liquidBlend = otherblock.hasLiquids
                && (otherblock.outputsLiquid || lookingAt(tile, rotation, otherx, othery, otherblock))
                && lookingAtEither(tile, rotation, otherx, othery, otherrot, otherblock);


        return itemBlend || liquidBlend;
    }


    public class HybridDuctBuild extends DuctBuild {
        public float smoothLiquid;

        protected float counter = 0f;

        @Override
        public void updateTile() {

            float transportInterval = speed * 2f - 1f;


            counter += edelta();

            while (counter >= transportInterval) {
                if (current != null && next != null) {
                    if (moveForward(current)) {
                        items.remove(current, 1);
                        current = null;
                    }
                }
                if (current == null && items.total() > 0) {
                    current = items.first();
                }
                counter -= transportInterval;
            }


            if (liquids.currentAmount() > 0.0001f && timer(timerFlow, 1)) {
                moveLiquidForward(leaks, liquids.current());
            }


            if (current == null && items.total() == 0 && liquids.currentAmount() <= 0.0001f) {
                sleep();
            } else {
                noSleep();
            }
        }

        @Override
        public void draw() {
            float rotation = rotdeg();
            int r = this.rotation;


            for(int i = 0; i < 4; i++){
                if((blending & (1 << i)) != 0){
                    int dir = r - i;
                    float rot = i == 0 ? rotation : (dir)*90;
                    drawAt(x + Geometry.d4x(dir) * tilesize*0.75f,
                            y + Geometry.d4y(dir) * tilesize*0.75f,
                            0, rot, i != 0 ? SliceMode.bottom : SliceMode.top);
                }
            }

            if(current != null){
                Draw.z(Layer.blockUnder + 0.1f);
                Tmp.v1.set(Geometry.d4x(recDir) * tilesize / 2f, Geometry.d4y(recDir) * tilesize / 2f)
                        .lerp(Geometry.d4x(r) * tilesize / 2f, Geometry.d4y(r) * tilesize / 2f,
                                Mathf.clamp((progress + 1f) / (2f - 1f/speed)));
                Draw.rect(current.fullIcon, x + Tmp.v1.x, y + Tmp.v1.y, itemSize, itemSize);
            }


            Draw.scl(xscl, yscl);


            if(liquids.currentAmount() > 0.001f){
                Draw.z(Layer.blockUnder + 0.05f);
                Draw.color(liquids.current().color);
                Draw.alpha(smoothLiquid * 0.8f);
                Draw.rect(botRegions[blendbits], x, y, rotation);
                Draw.color();
            }

            Draw.z(Layer.blockUnder + 0.2f);
            Draw.rect(topRegions[blendbits], x, y, rotation);

            Draw.reset();
        }


        @Override
        protected void drawAt(float x, float y, int bits, float rotation, SliceMode slice){
            Draw.z(Layer.blockUnder + 0.2f);
            Draw.rect(sliced(topRegions[bits], slice), x, y, rotation);
        }


        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            noSleep();
            return (liquids.current() == liquid || liquids.currentAmount() < 0.2f)
                    && (tile == null || source == this || (source.relativeTo(tile.x, tile.y) + 2) % 4 != rotation);
        }

        @Override
        public void handleLiquid(Building source, Liquid liquid, float amount) {
            liquids.add(liquid, amount);
            noSleep();
        }


    }
}