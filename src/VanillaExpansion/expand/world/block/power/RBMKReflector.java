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
 * RBMK反射器
 * 反射中子，提高反应堆效率
 */
public class RBMKReflector extends RBMKBase{
    public float reflectionBonus = 1.2f;
    
    public RBMKReflector(String name){
        super(name);
        hasItems = false;
        solid = true;
        update = true;
        requirements(Category.power, ItemStack.with(
            Items.copper, 100,
            Items.lead, 150,
            Items.graphite, 100
        ));
    }
    
    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.abilities, "Reflects neutrons, increases reactor efficiency");
    }
    
    public class RBMKReflectorBuild extends RBMKBaseBuild{
        @Override
        public void updateTile(){
            super.updateTile();
            
            // 检测到结构内有中子时，以2/t的速度增加中子
            if(neutronFlux > 0f){
                float neutronIncrease = 2f * delta();
                neutronFlux += neutronIncrease;
                neutronFluxFast += neutronIncrease * 0.6f;
                neutronFluxSlow += neutronIncrease * 0.4f;
            }
        }
        
        @Override
        public void draw(){
            super.draw();
            
            // 绘制反射效果
            Draw.z(Layer.blockOver);
            Draw.color(Color.yellow, 0.2f);
            Fill.square(x, y, size * tilesize / 2f - 2f);
            Draw.color();
        }
    }
}