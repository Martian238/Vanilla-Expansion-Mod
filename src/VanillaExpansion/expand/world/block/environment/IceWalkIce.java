package VanillaExpansion.expand.world.block.environment;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
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
