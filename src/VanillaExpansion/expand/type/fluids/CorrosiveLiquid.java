package VanillaExpansion.expand.type.fluids;

import arc.struct.Seq;
import mindustry.type.Liquid;
import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.world.*;

import static mindustry.entities.Puddles.*;


public class CorrosiveLiquid extends Liquid {

    public Seq<Block> whitelistBlocks = new Seq<>();
    public float damageRate = 0.1f;

    public CorrosiveLiquid(String name){
        super(name);
    }
    @Override
    public void update(Puddle puddle){
        if(!Vars.state.rules.fire) return;

        for(Block b : whitelistBlocks){
            if(damageRate > 0 && puddle.tile.build != null && !whitelistBlocks.contains(b)){
                puddle.tile.build.damage(damageRate * Time.delta);
            }
        }
    }

    @Override
    public boolean canExtinguish(){
        return false;
    }


}
