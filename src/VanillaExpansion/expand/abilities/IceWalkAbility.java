package VanillaExpansion.expand.abilities;

import arc.struct.ObjectMap;
import arc.struct.OrderedMap;
import arc.util.Time;
import mindustry.Vars;
import mindustry.entities.abilities.Ability;
import mindustry.gen.Unit;
import mindustry.graphics.BlockRenderer;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;

import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;

public class IceWalkAbility extends Ability {
    public IceWalkAbility() {}

    public ObjectMap<Block, Block> replaces = new OrderedMap<>();
    public int replaceRange = 5;
    public boolean flyAllow = false;

    @Override
    public void update(Unit unit){
        if(unit.moving() && !replaces.isEmpty() && (!unit.isFlying() || flyAllow)){
            float startX = unit.x - 0.5f * replaceRange * tilesize;
            float startY = unit.y - 0.5f * replaceRange * tilesize;
            for(int i=0; i<replaceRange; i++){
                for(int j=0; j<replaceRange; j++){
                    Tile tile = world.tileWorld(startX + i * tilesize, startY + j * tilesize);
                    if(tile != null) {
                        Floor floor = tile.floor();
                        if(floor != null) {
                            replaces.each((floor1, floor2) -> {
                                int x = tile.x;
                                int y = tile.y;
                                if(floor == floor2){
                                    Vars.renderer.blocks.floor.recacheTile(x, y);
                                }else if(floor == floor1 && floor2 instanceof Floor f) {
                                    tile.setFloor(f);
                                }
                            });
                        }
                    }
                }
            }
        }
    }
}
