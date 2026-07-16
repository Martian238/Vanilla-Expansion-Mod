package VanillaExpansion.expand.world.block.multicrafter;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;

import mindustry.gen.Building;

import mindustry.type.Liquid;
import mindustry.world.blocks.distribution.OverflowGate;
import mindustry.world.meta.BlockGroup;

import static mindustry.world.blocks.liquid.LiquidBlock.drawTiledFrames;


public class HybridGate extends OverflowGate {

    public TextureRegion bottomRegion;
    public float liquidPadding = 0f;


    public HybridGate(String name){
        super(name);
        hasLiquids = true;
        outputsLiquid = true;
        liquidCapacity = 10f;
        noUpdateDisabled = true;
        canOverdrive = false;
        floating = true;
        update = true;
        group = BlockGroup.transportation;
        buildType = HybridGateBuild::new;
    }

    @Override
    public void load() {
        super.load();
        bottomRegion = Core.atlas.find(name + "-bottom");
    }


    public class HybridGateBuild extends OverflowGateBuild {
        @Override
        public void updateTile(){
            dumpLiquid(liquids.current());
        }

        @Override
        public void draw(){
            Draw.rect(bottomRegion, x, y);

            if(liquids.currentAmount() > 0.001f){
                drawTiledFrames(size, x, y, liquidPadding, liquids.current(), liquids.currentAmount() / liquidCapacity);
            }

            Draw.rect(region, x, y);
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            return (liquids.current() == liquid || liquids.currentAmount() < 0.2f);
        }
    }





}
