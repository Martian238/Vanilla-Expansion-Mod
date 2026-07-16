package VanillaExpansion.expand.world.block.power;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.power.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * RBMK空白结构
 * 填充反应堆结构
 */
public class RBMKBlank extends RBMKBase{
    public RBMKBlank(String name){
        super(name);
        hasItems = false;
        solid = true;
        update = true;
        requirements(Category.power, ItemStack.with(
            Items.copper, 50,
            Items.lead, 50
        ));
    }
    
    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.abilities, "Fills reactor structure");
    }
    
    public class RBMKBlankBuild extends RBMKBaseBuild{
        @Override
        public void updateTile(){
            super.updateTile();
        }
    }
}