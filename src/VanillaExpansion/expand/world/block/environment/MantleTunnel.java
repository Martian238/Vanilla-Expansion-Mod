package VanillaExpansion.expand.world.block.environment;

import arc.Core;
import mindustry.world.blocks.environment.Floor;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.graphics.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class MantleTunnel extends Floor {
    public static final Point2[] offsets = {
            new Point2(0, 0),
            new Point2(1, 0),
            new Point2(1, 1),
            new Point2(0, 1),
            new Point2(-1, 1),
            new Point2(-1, 0),
            new Point2(-1, -1),
            new Point2(0, -1),
            new Point2(1, -1),
            new Point2(-1, 2),
            new Point2(2, 1),
            new Point2(2, 0),
            new Point2(2, -1),
            new Point2(0, 2),
            new Point2(1, 2),
            new Point2(2, 2),
    };

    public Block parent = Blocks.air;
    public Effect effect = Fx.redgeneratespark;
    public Color effectColor = Pal.redSpark;
    public float effectSpacing = 15f;

    static{
        for(var p : offsets){
            p.sub(1, 1);
        }
    }

    public MantleTunnel(String name){
        super(name);
        variants = 1;
        emitLight = true;
        lightColor = Blocks.magmarock.lightColor;
        lightRadius = Blocks.magmarock.lightRadius;
        attributes.set(Attribute.heat, 2f);
        attributes.set(Attribute.water, -1f);
        attributes.set(Attribute.sand, 10f);
        parent = blendGroup = Blocks.darkPanel3;
    }

    @Override
    public void drawMain(Tile tile){
        if(parent instanceof Floor floor){
            floor.drawMain(tile);
        }
        if(checkAdjacent(tile)){
            variantRegions = new TextureRegion[1];
            variantRegions[0] = Core.atlas.find(name + "1");
            Draw.rect(variantRegions[0], tile.worldx() - tilesize * 2f, tile.worldy() - tilesize * 2f);
        }
    }

    @Override
    public boolean updateRender(Tile tile){
        return checkAdjacent(tile);
    }

    @Override
    public boolean shouldIndex(Tile tile){
        return isCenterVent(tile);
    }

    public boolean isCenterVent(Tile tile){
        for(int x = 0; x < 4; x++){
            for(int y = 0; y < 4; y++){
                Tile other = tile.nearby(x - 1, y - 1);
                if(other == null || other.floor() != this) return false;
            }
        } return true;
    }

    @Override
    public void renderUpdate(UpdateRenderState state){
        if(state.tile.nearby(0, 0) != null && state.tile.nearby(0, 0).block() == Blocks.air && (state.data += Time.delta) >= effectSpacing){
            effect.at(state.tile.x * tilesize - tilesize * 1.5f + Mathf.range(12), state.tile.y * tilesize - tilesize * 1.5f + Mathf.range(12), effectColor);
            state.data = 0f;
        }
    }

    public boolean checkAdjacent(Tile tile){
        for(int x = 0; x < 4; x++){
            for(int y = 0; y < 4; y++){
                Tile other = tile.nearby(- x, - y);
                if(other == null || other.floor() != this) return false;
            }
        } return true;
    }
}
