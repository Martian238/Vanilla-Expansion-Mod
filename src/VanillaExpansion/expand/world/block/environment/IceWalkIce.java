package VanillaExpansion.expand.world.block.environment;

import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.gen.Building;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;

import static mindustry.Vars.state;
import static mindustry.Vars.world;

public class IceWalkIce extends Floor {
    public IceWalkIce(String name) {
        super(name);
    }

    public float breakTime = 180f;
    public Block breakResult = Blocks.deepwater;
    public Effect breakEffect = Fx.none;

    @Override
    public void renderUpdate(UpdateRenderState state2) {
        state2.data++;
        if(state2.tile != null && state2.data >= breakTime){
            breakEffect.at(state2.tile.worldx(), state2.tile.worldy(), 0f, state2.tile.block().mapColor);
            if(breakResult instanceof Floor f) state2.tile.setFloor(f);
        }
    }

    @Override
    public boolean updateRender(Tile tile){
        return true;
    }
}
