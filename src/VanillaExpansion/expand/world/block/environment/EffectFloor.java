package VanillaExpansion.expand.world.block.environment;

import arc.graphics.Color;
import arc.math.Mathf;
import arc.util.Time;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.graphics.Pal;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;

import static mindustry.Vars.tilesize;

public class EffectFloor extends Floor {
    public EffectFloor(String name){
        super(name);
    }

    public Effect effect = Fx.redgeneratespark;
    public Color effectColor = Pal.redSpark;
    public float effectSpacing = -1f;
    public float effectChance = 0.02f;
    public float xRand = 0, yRand = 0, pRand = 0;

    @Override
    public boolean updateRender(Tile tile){
        return true;
    }
    @Override
    public void renderUpdate(UpdateRenderState state){
        if(state.tile.nearby(0, 0) != null && state.tile.nearby(0, 0).block() == Blocks.air && (effectSpacing >= 0 ? ((state.data += Time.delta) >= effectSpacing) : Mathf.chanceDelta(effectChance))){
            float dx = 0, dy = 0;
            if(pRand > 0){
                float p = Mathf.range(pRand);
                float r = Mathf.range(180f);
                dx += p * Mathf.cosDeg(r);
                dy += p * Mathf.sinDeg(r);
            }
            dx += Mathf.range(xRand);
            dy += Mathf.range(yRand);
            effect.at(state.tile.worldx() + dx, state.tile.worldy() + dy, effectColor);
            state.data = 0f;
        }
    }
}
