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
 * RBMK吸收器
 * 吸收中子，降低反应堆功率
 */
public class RBMKAbsorber extends RBMKBase{
    public float absorptionFactor = 0.8f;
    
    public RBMKAbsorber(String name){
        super(name);
        hasItems = false;
        solid = true;
        update = true;
        requirements(Category.power, ItemStack.with(
            Items.copper, 100,
            Items.lead, 200,
            Items.graphite, 50
        ));
    }
    
    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.abilities, "Absorbs neutrons, decreases reactor power");
    }
    
    public class RBMKAbsorberBuild extends RBMKBaseBuild{
        @Override
        public void updateTile(){
            super.updateTile();
            
            // 吸收中子，每秒吸收25f中子
            float absorptionRate = 15f * delta();
            neutronFlux = Math.max(neutronFlux - absorptionRate, 0f);
            neutronFluxFast = Math.max(neutronFluxFast - absorptionRate, 0f);
            neutronFluxSlow = Math.max(neutronFluxSlow - absorptionRate, 0f);
        }
        
        @Override
        public void draw(){
            super.draw();
            
            // 绘制吸收效果
            Draw.z(Layer.blockOver);
            Draw.color(Color.purple, 0.2f);
            Fill.square(x, y, size * tilesize / 2f - 2f);
            Draw.color();
        }
    }
}