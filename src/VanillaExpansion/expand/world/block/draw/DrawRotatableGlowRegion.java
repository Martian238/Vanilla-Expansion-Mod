package VanillaExpansion.expand.world.block.draw;

import arc.graphics.g2d.Draw;
import arc.math.Mathf;
import mindustry.gen.Building;
import mindustry.world.draw.DrawGlowRegion;
import mindustry.world.draw.DrawRegion;

public class DrawRotatableGlowRegion extends DrawGlowRegion {
    public float rotation;

    public DrawRotatableGlowRegion(){
    }

    public DrawRotatableGlowRegion(float layer){
        this.layer = layer;
    }

    public DrawRotatableGlowRegion(boolean rotate){
        this.rotate = rotate;
    }


    public DrawRotatableGlowRegion(String suffix){
        this.suffix = suffix;
    }


    @Override
    public void draw(Building build){
        if(build.warmup() <= 0.001f) return;

        float z = Draw.z();
        if(layer > 0) Draw.z(layer);
        Draw.blend(blending);
        Draw.color(color);
        Draw.alpha((Mathf.absin(build.totalProgress(), glowScale, alpha) * glowIntensity + 1f - glowIntensity) * build.warmup() * alpha);
        Draw.rect(region, build.x, build.y, build.totalProgress() * rotateSpeed + rotation + (rotate ? build.rotdeg() : 0f));
        Draw.reset();
        Draw.blend();
        Draw.z(z);
    }
}
