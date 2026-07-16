package VanillaExpansion.expand.world.block.power;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
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
 * RBMK控制棒
 * 调节反应堆功率输出
 */
public class RBMKControl extends RBMKBase{
    public boolean moderated = false;
    public float maxControlValue =1f;
    
    public RBMKControl(String name){
        super(name);
        hasItems = false;
        configurable = true;
        solid = true;
        update = true;
        requirements(Category.power, ItemStack.with(
            Items.copper, 150,
            Items.lead, 100,
            Items.titanium, 50,
            Items.graphite, 50
        ));
    }
    
    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.abilities, "Controls reactor power output");
    }
    
    @Override
    public void setBars(){
        super.setBars();
        addBar("control", (RBMKControlBuild entity) -> new Bar(
            () -> "Control: " + (int)(entity.controlValue * 100) + "%",
            () -> Pal.accent,
            () -> entity.controlValue
        ));
    }
    
    public class RBMKControlBuild extends RBMKBaseBuild{
        public float controlValue = 0.5f; // 0-1, 0=完全插入, 1=完全拔出
        public float targetControlValue = 0.5f; // 目标控制值，用于平滑过渡
        
        @Override
        public void updateTile(){            super.updateTile();
            
            // 平滑过渡到目标控制值
            controlValue = Mathf.lerp(controlValue, targetControlValue, 0.033f * delta());
            
            // 控制中子通过：滑轨划到多少百分比就通过多少中子
            float passRatio = controlValue; // 通过比例
            float absorbRatio = 1f - controlValue; // 吸收比例
            
            // 如果调整到0%，则清零所有中子
            if(controlValue <= 0.01f){
                // 清零自身的中子通量
                neutronFlux = 0f;
                neutronFluxFast = 0f;
                neutronFluxSlow = 0f;
                
                // 清零3x3范围内所有结构的中子
                for(int dx = -1; dx <= 1; dx++){
                    for(int dy = -1; dy <= 1; dy++){
                        Building build = world.build((int)(x + dx), (int)(y + dy));
                        if(build != null && build.block instanceof RBMKBase){
                            RBMKBaseBuild baseBuild = (RBMKBaseBuild)build;
                            baseBuild.neutronFlux = 0f;
                            baseBuild.neutronFluxFast = 0f;
                            baseBuild.neutronFluxSlow = 0f;
                        }
                    }
                }
            } else {
                // 计算需要吸收的中子量（考虑时间因子，确保平滑过渡）
                float absorbedFlux = neutronFlux * absorbRatio * 0.1f * delta();
                float absorbedFluxFast = neutronFluxFast * absorbRatio * 0.1f * delta();
                float absorbedFluxSlow = neutronFluxSlow * absorbRatio * 0.1f * delta();
                
                // 吸收中子
                neutronFlux = Math.max(neutronFlux - absorbedFlux, 0f);
                neutronFluxFast = Math.max(neutronFluxFast - absorbedFluxFast, 0f);
                neutronFluxSlow = Math.max(neutronFluxSlow - absorbedFluxSlow, 0f);
            }
            
            // 影响周围的反应堆组件
            affectNearbyComponents();
        }
        
        public void affectNearbyComponents() {
            // 现在燃料棒会自己扫描周围的控制棒，所以这里不需要再做任何操作
        }
        
        @Override
        public void conductNeutronFlux() {
            // 控制棒吸收中子，根据位置决定吸收比例
            float absorbRatio = 1f - controlValue; // 吸收比例：0=完全不吸收（完全拔出），1=完全吸收（完全插入）
            float passRatio = controlValue; // 通过比例：0=完全不通过（完全插入），1=完全通过（完全拔出）
            
            // 吸收中子
            float absorbedFlux = neutronFlux * absorbRatio * 0.5f * delta();
            neutronFlux -= absorbedFlux;
            
            float absorbedSlowFlux = neutronFluxSlow * absorbRatio * 0.5f * delta();
            neutronFluxSlow -= absorbedSlowFlux;
            
            float absorbedFastFlux = neutronFluxFast * absorbRatio * 0.5f * delta();
            neutronFluxFast -= absorbedFastFlux;
            
            // 传导剩余的中子到相邻组件
            for(Building other : proximity) {
                if(other instanceof RBMKBaseBuild) {
                    RBMKBaseBuild rbmk = (RBMKBaseBuild) other;
                    // 传导中子通量，考虑控制棒位置
                    float fluxTransfer = neutronFlux * 0.1f * passRatio * delta();
                    neutronFlux -= fluxTransfer;
                    rbmk.neutronFlux += fluxTransfer;
                    
                    // 传导慢中子通量，考虑控制棒位置
                    float slowFluxTransfer = neutronFluxSlow * 0.1f * passRatio * delta();
                    neutronFluxSlow -= slowFluxTransfer;
                    rbmk.neutronFluxSlow += slowFluxTransfer;
                    
                    // 传导快中子通量，考虑控制棒位置
                    float fastFluxTransfer = neutronFluxFast * 0.1f * passRatio * delta();
                    neutronFluxFast -= fastFluxTransfer;
                    rbmk.neutronFluxFast += fastFluxTransfer;
                }
            }
        }
        
        @Override
        public void draw(){
            super.draw();
            
            // 绘制控制棒位置
            Draw.z(Layer.blockOver);
            Draw.color(Pal.gray);
            float barWidth = size * tilesize - 8f;
            float barHeight = 4f;
            Fill.rect(x, y - size * tilesize / 2f + 6f, barWidth, barHeight);
            
            Draw.color(Pal.accent);
            float fillWidth = barWidth * controlValue;
            Fill.rect(x - barWidth / 2f + fillWidth / 2f, y - size * tilesize / 2f + 6f, fillWidth, barHeight);
            Draw.color();
        }
        
        @Override
        public void buildConfiguration(Table table){            super.buildConfiguration(table);
            
            table.label(() -> "Control Rod Position:");
            table.row();
            table.slider(0f, 1f, 0.01f, targetControlValue, value -> targetControlValue = value);
        }
        
        @Override
        public void configure(Object value){            if(value instanceof Float f){                targetControlValue = Mathf.clamp(f, 0f, 1f);            }
        }
        
        @Override
        public void write(Writes write){            super.write(write);            write.f(controlValue);            write.f(targetControlValue);
        }
        
        @Override
        public void read(Reads read, byte revision){            super.read(read, revision);            controlValue = read.f();            targetControlValue = read.f();
        }
        
        @Override
        public byte version(){
            return 0;
        }
    }
}