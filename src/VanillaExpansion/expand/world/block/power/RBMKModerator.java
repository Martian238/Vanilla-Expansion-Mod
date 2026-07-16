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
 * RBMK慢化剂
 * 慢化中子，提高反应堆稳定性
 */
public class RBMKModerator extends RBMKBase{
    public float moderationBonus = 1.3f;
    
    public RBMKModerator(String name){
        super(name);
        hasItems = false;
        solid = true;
        update = true;
        requirements(Category.power, ItemStack.with(
            Items.copper, 100,
            Items.lead, 100,
            Items.graphite, 150
        ));
    }
    
    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.abilities, "Slows neutrons, increases reactor stability");
    }
    
    public class RBMKModeratorBuild extends RBMKBaseBuild{
        @Override
        public void updateTile(){
            super.updateTile();
            
            // 检测到结构内有总中子时，将1总中子转化为0.8慢中子
            if(neutronFlux > 0f){
                float conversionAmount = Math.min(neutronFlux, 1f * delta());
                neutronFlux -= conversionAmount;
                neutronFluxSlow += conversionAmount * 0.8f;
            }
        }
        
        @Override
        public void draw(){
            super.draw();
            
            // 绘制慢化效果
            Draw.z(Layer.blockOver);
            Draw.color(Color.green, 0.2f);
            Fill.square(x, y, size * tilesize / 2f - 2f);
            Draw.color();
        }
    }
}