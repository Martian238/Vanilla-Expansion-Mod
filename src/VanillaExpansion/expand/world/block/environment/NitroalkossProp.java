package VanillaExpansion.expand.world.block.environment;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.entities.Effect;
import mindustry.entities.effect.ParticleEffect;
import mindustry.gen.ContentRegions;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.WobbleProp;

public class NitroalkossProp extends WobbleProp {


    public float podShineInterval = 60f;
    public float podShineFrameInterval = 7f;


    public NitroalkossProp(String name){
        super(name);
        update = true;
        lightColor = Color.valueOf("ff8a67");
        emitLight = true;
    }


    public TextureRegion[] variantRegionsAnimation0;
    public TextureRegion[] variantRegionsAnimation1;
    public TextureRegion[] variantRegionsAnimation2;
    public TextureRegion[] variantRegionsAnimation3;
    public TextureRegion[] variantRegionsAnimation4;
    public TextureRegion[] variantRegionsAnimation5;

    @Override
    public void load(){
        super.load();

        region = Core.atlas.find(name);

        ContentRegions.loadRegions(this);


        variantRegionsAnimation0 = new TextureRegion[variants];
        variantRegionsAnimation1 = new TextureRegion[variants];
        variantRegionsAnimation2 = new TextureRegion[variants];
        variantRegionsAnimation3 = new TextureRegion[variants];
        variantRegionsAnimation4 = new TextureRegion[variants];
        variantRegionsAnimation5 = new TextureRegion[variants];

            for(int i = 0; i < variants; i++){
                variantRegionsAnimation0[i] = Core.atlas.find(name + (i + 1) + "-0");
                variantRegionsAnimation1[i] = Core.atlas.find(name + (i + 1) + "-1");
                variantRegionsAnimation2[i] = Core.atlas.find(name + (i + 1) + "-2");
                variantRegionsAnimation3[i] = Core.atlas.find(name + (i + 1) + "-3");
                variantRegionsAnimation4[i] = Core.atlas.find(name + (i + 1) + "-4");
                variantRegionsAnimation5[i] = Core.atlas.find(name + (i + 1) + "-5");
            }
            region = variantRegionsAnimation0[0];

            if(customShadow){
                variantShadowRegions = new TextureRegion[variants];
                for(int i = 0; i < variants; i++){
                    variantShadowRegions[i] = Core.atlas.find(name + "-shadow" + (i + 1));
                }
            }

    }

    @Override
    public void drawBase(Tile tile){
        float ti = podShineFrameInterval / podShineInterval;
        float t = (Time.time % podShineInterval) / podShineInterval;
        if(t > ti * 5) {
            var region = variants > 0 ? variantRegionsAnimation0[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegionsAnimation0.length - 1))] : this.region;
            Draw.rectv(region, tile.worldx(), tile.worldy(), region.width * region.scl(), region.height * region.scl(), 0, vec -> vec.add(
                    Mathf.sin(vec.y * 3 + Time.time, wscl, wmag) + Mathf.sin(vec.x * 3 - Time.time, 70 * wtscl, 0.8f * wmag2),
                    Mathf.cos(vec.x * 3 + Time.time + 8, wscl + 6f, wmag * 1.1f) + Mathf.sin(vec.y * 3 - Time.time, 50 * wtscl, 0.2f * wmag2)
            ));
            this.lightRadius = 24f;
        }else if(t > ti * 4){
            var region = variants > 0 ? variantRegionsAnimation1[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegionsAnimation1.length - 1))] : this.region;
            Draw.rectv(region, tile.worldx(), tile.worldy(), region.width * region.scl(), region.height * region.scl(), 0, vec -> vec.add(
                    Mathf.sin(vec.y * 3 + Time.time, wscl, wmag) + Mathf.sin(vec.x * 3 - Time.time, 70 * wtscl, 0.8f * wmag2),
                    Mathf.cos(vec.x * 3 + Time.time + 8, wscl + 6f, wmag * 1.1f) + Mathf.sin(vec.y * 3 - Time.time, 50 * wtscl, 0.2f * wmag2)
            ));
            this.lightRadius = 16f;
        }else if(t > ti * 3){
            var region = variants > 0 ? variantRegionsAnimation2[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegionsAnimation2.length - 1))] : this.region;
            Draw.rectv(region, tile.worldx(), tile.worldy(), region.width * region.scl(), region.height * region.scl(), 0, vec -> vec.add(
                    Mathf.sin(vec.y * 3 + Time.time, wscl, wmag) + Mathf.sin(vec.x * 3 - Time.time, 70 * wtscl, 0.8f * wmag2),
                    Mathf.cos(vec.x * 3 + Time.time + 8, wscl + 6f, wmag * 1.1f) + Mathf.sin(vec.y * 3 - Time.time, 50 * wtscl, 0.2f * wmag2)
            ));
            this.lightRadius = 12f;
        }else if(t > ti * 2){
            var region = variants > 0 ? variantRegionsAnimation3[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegionsAnimation3.length - 1))] : this.region;
            Draw.rectv(region, tile.worldx(), tile.worldy(), region.width * region.scl(), region.height * region.scl(), 0, vec -> vec.add(
                    Mathf.sin(vec.y * 3 + Time.time, wscl, wmag) + Mathf.sin(vec.x * 3 - Time.time, 70 * wtscl, 0.8f * wmag2),
                    Mathf.cos(vec.x * 3 + Time.time + 8, wscl + 6f, wmag * 1.1f) + Mathf.sin(vec.y * 3 - Time.time, 50 * wtscl, 0.2f * wmag2)
            ));
            this.lightRadius = 8f;
        }else if(t > ti){
            var region = variants > 0 ? variantRegionsAnimation4[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegionsAnimation4.length - 1))] : this.region;
            Draw.rectv(region, tile.worldx(), tile.worldy(), region.width * region.scl(), region.height * region.scl(), 0, vec -> vec.add(
                    Mathf.sin(vec.y * 3 + Time.time, wscl, wmag) + Mathf.sin(vec.x * 3 - Time.time, 70 * wtscl, 0.8f * wmag2),
                    Mathf.cos(vec.x * 3 + Time.time + 8, wscl + 6f, wmag * 1.1f) + Mathf.sin(vec.y * 3 - Time.time, 50 * wtscl, 0.2f * wmag2)
            ));
            this.lightRadius = 4f;
        }else{
            var region = variants > 0 ? variantRegionsAnimation5[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegionsAnimation5.length - 1))] : this.region;
            Draw.rectv(region, tile.worldx(), tile.worldy(), region.width * region.scl(), region.height * region.scl(), 0, vec -> vec.add(
                    Mathf.sin(vec.y * 3 + Time.time, wscl, wmag) + Mathf.sin(vec.x * 3 - Time.time, 70 * wtscl, 0.8f * wmag2),
                    Mathf.cos(vec.x * 3 + Time.time + 8, wscl + 6f, wmag * 1.1f) + Mathf.sin(vec.y * 3 - Time.time, 50 * wtscl, 0.2f * wmag2)
            ));
            this.lightRadius = 2f;
        }
    }
}
