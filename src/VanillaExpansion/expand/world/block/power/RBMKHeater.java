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
 * RBMK加热器
 * 为反应堆提供初始热量
 */
public class RBMKHeater extends RBMKBase{
    public float heatProduction = 0.5f;
    
    public RBMKHeater(String name){
        super(name);
        hasItems = false;
        solid = true;
        update = true;
        requirements(Category.power, ItemStack.with(
            Items.copper, 120,
            Items.lead, 100,
            Items.titanium, 80,
            Items.graphite, 50
        ));
    }
    
    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.abilities, "Provides initial heat for reactor");
    }
    
    public class RBMKHeaterBuild extends RBMKBaseBuild{
        @Override
        public void updateTile(){
            super.updateTile();
            
            // 产生初始热量
            if(heat < 100f){
                heat = Mathf.clamp(heat + heatProduction * delta(), 25f, 100f);
            }
        }
        
        @Override
        public void draw(){
            super.draw();
            
            // 绘制加热效果
            if(heat < 100f){
                Draw.z(Layer.blockAdditive);
                Draw.blend(Blending.additive);
                float intensity = Mathf.clamp((100f - heat) / 75f, 0f, 1f);
                Draw.color(Color.orange, intensity * 0.3f);
                Fill.square(x, y, size * tilesize / 2f - 2f);
                Draw.blend();
                Draw.color();
            }
        }
    }
}