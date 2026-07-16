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
 * RBMK冷却器
 * 降低反应堆温度
 */
public class RBMKCooler extends RBMKBase{
    public float coolingPower = 1f;
    
    public RBMKCooler(String name){
        super(name);
        hasItems = false;
        solid = true;
        update = true;
        requirements(Category.power, ItemStack.with(
            Items.copper, 100,
            Items.lead, 80,
            Items.metaglass, 50
        ));
    }
    
    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.abilities, "Cools reactor temperature");
    }
    
    @Override
    public void setBars(){
        super.setBars();
        addBar("cooling", (RBMKCoolerBuild entity) -> new Bar(
            () -> "Cooling: " + (int)(entity.coolingEfficiency * 100) + "%",
            () -> Pal.accent,
            () -> entity.coolingEfficiency
        ));
    }
    
    public class RBMKCoolerBuild extends RBMKBaseBuild{
        public float coolingEfficiency = 1f;
        public float baseCoolingEfficiency = 1f;
        public boolean isCooling = false; // 是否正在冷却
        public float coolingEffectTime = 0f; // 冷却效果时间
        
        @Override
        public void updateTile(){
            super.updateTile();
            
            // 计算冷却效率（根据周围温度和自身状态）
            calculateCoolingEfficiency();
            
            // 基础冷却效果
            heat = Mathf.clamp(heat - coolingPower * coolingEfficiency * delta(), 25f, maxHeat);
            
            // 检测到结构内存在cryofluid液体时，如果温度大于800则以150/t的速度消耗热量，并以120/s消耗液体
            if(liquids != null && liquids.current() == Liquids.cryofluid && liquids.currentAmount() > 0.1f){
                if(heat > 800f){
                    float cryofluidCooling = 150f * delta();
                    heat = Mathf.clamp(heat - cryofluidCooling, 25f, maxHeat);
                    
                    // 消耗液体
                    float liquidConsumption = 120f * delta();
                    liquids.remove(Liquids.cryofluid, liquidConsumption);
                    
                    // 标记正在冷却
                    isCooling = true;
                    coolingEffectTime = 0.5f;
                }
            }
            
            // 更新冷却效果时间
            if(coolingEffectTime > 0f){
                coolingEffectTime -= delta();
            } else {
                isCooling = false;
            }
        }
        
        public void calculateCoolingEfficiency(){
            // 基础冷却效率
            coolingEfficiency = baseCoolingEfficiency;
            
            // 根据周围温度调整冷却效率
            // 周围温度越高，冷却效率越高
            float nearbyHeat = 0f;
            int count = 0;
            
            for(int dx = -1; dx <= 1; dx++){
                for(int dy = -1; dy <= 1; dy++){
                    if(dx == 0 && dy == 0) continue;
                    
                    Building build = world.build((int)(x + dx), (int)(y + dy));
                    if(build != null && build.block instanceof RBMKBase){
                        RBMKBaseBuild baseBuild = (RBMKBaseBuild)build;
                        nearbyHeat += baseBuild.heat;
                        count++;
                    }
                }
            }
            
            if(count > 0){
                float avgNearbyHeat = nearbyHeat / count;
                // 周围温度越高，冷却效率提升越多
                coolingEfficiency *= (1f + (avgNearbyHeat - 25f) / 1000f);
            }
            
            // 限制冷却效率范围
            coolingEfficiency = Mathf.clamp(coolingEfficiency, 0.5f, 2f);
        }
        
        @Override
        public void draw(){
            super.draw();
            
            // 绘制冷却效果
            if(heat > 50f){
                Draw.z(Layer.blockAdditive);
                Draw.blend(Blending.additive);
                float intensity = Mathf.clamp((heat - 50f) / (maxHeat - 50f), 0f, 1f);
                Draw.color(Pal.accent, intensity * 0.3f);
                Fill.square(x, y, size * tilesize / 2f - 2f);
                Draw.blend();
                Draw.color();
            }
            
            // 绘制热量消耗时的红色喷射效果
            if(isCooling && coolingEffectTime > 0f){
                Draw.z(Layer.effect);
                Draw.blend(Blending.additive);
                
                float alpha = Mathf.clamp(coolingEffectTime / 0.5f, 0f, 1f);
                float height = size * tilesize * 1.5f * alpha;
                float width = size * tilesize * 0.3f * alpha;
                
                Draw.color(Color.red, alpha * 0.8f);
                Fill.rect(x, y + height / 2f, width, height);
                
                // 绘制喷射粒子
                for(int i = 0; i < 3; i++){
                    float offset = (i - 1) * width * 0.4f;
                    float particleHeight = height * (0.8f + Mathf.random() * 0.4f);
                    float particleWidth = width * 0.3f;
                    
                    Draw.color(Color.red, alpha * 0.6f * Mathf.random());
                    Fill.rect(x + offset, y + particleHeight / 2f, particleWidth, particleHeight);
                }
                
                Draw.blend();
                Draw.color();
            }
        }
        
        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            // 只接受冷却液
            return liquid == Liquids.cryofluid && (liquids.current() == liquid || liquids.currentAmount() < 0.2f);
        }
        
        @Override
        public void write(Writes write){
            super.write(write);
            write.f(coolingEfficiency);
        }
        
        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            coolingEfficiency = read.f();
        }
        
        @Override
        public byte version(){
            return 0;
        }
    }
}