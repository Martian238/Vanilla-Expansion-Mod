package VanillaExpansion.expand.world.block.draw;

import arc.Core;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.struct.Seq;
import arc.util.Eachable;
import mindustry.entities.part.DrawPart;
import mindustry.entities.units.BuildPlan;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.draw.DrawBlock;
import mindustry.world.draw.DrawBlockParts;

public class DrawStaticBlockParts extends DrawBlock {
    public Seq<DrawPart> parts = new Seq<>();
    public TextureRegion preview;

    @Override
    public void getRegionsToOutline(Block block, Seq<TextureRegion> out){
        for(var part : parts){
            part.getOutlines(out);
        }
    }

    @Override
    public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list){
        if(preview.found()){
            Draw.rect(preview, plan.drawx(), plan.drawy(), 0f);
        }
    }

    @Override
    public void draw(Building build){
        if(parts.size > 0){
            float progress = build.progress();

            var params = DrawPart.params.set(build.warmup(), 1f - progress, 1f - progress, 0f, 0f, 0f, build.x, build.y, 0f);

            for(var part : parts){
                part.draw(params);
            }
        }
    }

    @Override
    public void load(Block block){
        preview = Core.atlas.find(block.name + "-preview");

        for(var part : parts){
            part.load(block.name);
        }
    }

    @Override
    public TextureRegion[] icons(Block block){
        return preview.found() ? new TextureRegion[]{preview} : super.icons(block);
    }
}
