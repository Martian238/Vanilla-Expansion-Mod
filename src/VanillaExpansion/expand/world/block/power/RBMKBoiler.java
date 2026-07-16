package VanillaExpansion.expand.world.block.power;

import VanillaExpansion.content.VELiquids;
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
import mindustry.world.modules.*;

import static mindustry.Vars.*;

/**
 * RBMK锅炉
 * 利用反应堆热量产生蒸汽
 */
public class RBMKBoiler extends RBMKBase{
    public float steamProduction = 1f;
    
    public RBMKBoiler(String name){
        super(name);
        hasItems = false;
        solid = true;
        update = true;
        requirements(Category.power, ItemStack.with(
            Items.copper, 150,
            Items.lead, 100,
            Items.metaglass, 80,
            Items.graphite, 50
        ));
    }
    
    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.abilities, "Produces steam from reactor heat");
    }
    
    @Override
    public void setBars(){
        super.setBars();
        // 添加蒸汽容量进度条
        addBar("steam", (RBMKBoilerBuild entity) -> new Bar(
            () -> entity.steamLiquids != null && entity.steamLiquids.current() != null ? 
                entity.steamLiquids.current().localizedName + " " + Strings.fixed(entity.steamLiquids.currentAmount(), 1) + "/100" : 
                "No Steam",
            () -> entity.steamLiquids != null && entity.steamLiquids.current() != null ? entity.steamLiquids.current().barColor() : Pal.gray,
            () -> entity.steamLiquids != null ? entity.steamLiquids.currentAmount() / 100f : 0f
        ));
    }
    
    public class RBMKBoilerBuild extends RBMKBaseBuild{
        private float steamProgress = 0f;
        private static final float craftTime = 12f; // 制作时间（提高50倍速度）
        private LiquidModule steamLiquids; // 第二个液体容量，用于存储蒸汽
        
        @Override
        public void created(){
            super.created();
            steamLiquids = new LiquidModule();
        }
        
        @Override
        public void updateTile(){
            // 不调用super.updateTile()，避免自动输出第一容量的液体
            
            // 热量传导
            conductHeat();
            // 中子通量传导
            conductNeutronFlux();
            // 基础热量散失
            heat = Mathf.clamp(heat - 0.01f * delta(), 25f, maxHeat);
            
            // 只有当温度足够高且有水时才进行蒸汽生产
            if(heat > 150f && liquids != null && liquids.get(Liquids.water) > 0.1f){
                // 检查第二容量液体，如果等于或大于容量则停止换热
                if(steamLiquids != null && VELiquids.steam != null && steamLiquids.get(VELiquids.steam) >= 100f){
                    steamProgress = 0f;
                } else {
                    // 计算生产速度，基于温度（线性效率，温度越高效率越高）
                    float efficiency = (heat - 150f) / 100f;
                    // 确保效率至少为0.1，避免过低效率
                    efficiency = Math.max(efficiency, 0.1f);
                    steamProgress += efficiency * delta() / craftTime;
                    
                    if(steamProgress >= 1f){
                        // 消耗水
                        float waterConsumed = 5f; // 消耗5单位水（提高50倍速度）
                        if(liquids.get(Liquids.water) >= waterConsumed){
                            liquids.remove(Liquids.water, waterConsumed);
                            
                            // 生产蒸汽到第二个液体容量
                            if(VELiquids.steam != null && steamLiquids != null){
                                steamLiquids.add(VELiquids.steam, waterConsumed * 10f); // 蒸汽体积膨胀10倍
                            }
                            
                            // 换热降温
                            heat = Mathf.clamp(heat - waterConsumed * 20f, 25f, maxHeat); // 提高2倍热量消耗
                            
                            steamProgress = 0f;
                        }
                    }
                }
            } else {
                steamProgress = 0f;
            }
            
            // 自动输出蒸汽
            if(steamLiquids != null && VELiquids.steam != null && steamLiquids.get(VELiquids.steam) > 0.1f){
                // 直接从第二个液体容量输出蒸汽
                // 使用dumpLiquid方法输出蒸汽
                dumpSteamLiquid();
            }
        }
        
        @Override
        public void write(Writes write){
            super.write(write);
            if(steamLiquids != null){
                steamLiquids.write(write);
            }
        }
        
        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            steamLiquids = new LiquidModule();
            steamLiquids.read(read);
        }
        
        // 输出蒸汽到相邻方块
        public void dumpSteamLiquid(){
            if(steamLiquids == null || VELiquids.steam == null) return;
            
            Liquid liquid = VELiquids.steam;
            float amount = steamLiquids.get(liquid);
            if(amount <= 0.01f) return;
            
            for(int i = 0; i < 4; i++){
                Building other = nearby(i);
                if(other != null && other.liquids != null && other.block.hasLiquids){
                    float transferAmount = Math.min(amount, 50f); // 每次传输50单位蒸汽
                    float currentAmount = other.liquids.get(liquid);
                    float canAccept = Math.min(transferAmount, other.block.liquidCapacity - currentAmount);
                    
                    if(canAccept > 0){
                        other.liquids.add(liquid, canAccept);
                        steamLiquids.remove(liquid, canAccept);
                        amount -= canAccept;
                        
                        if(amount <= 0.01f) break;
                    }
                }
            }
        }
        
        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
        // 只接受水到第一容量
        return liquid == Liquids.water;
        }

        // 只接受蒸汽到第二容量
        public boolean acceptSteamLiquid(Building source, Liquid liquid){
        return liquid == VELiquids.steam;
        }
        
        @Override
        public void draw(){
            super.draw();
            
            // 绘制蒸汽效果
            if(heat > 100f){
                Draw.z(Layer.blockAdditive);
                Draw.blend(Blending.additive);
                float intensity = Mathf.clamp((heat - 100f) / (maxHeat - 100f), 0f, 1f);
                Draw.color(Color.white, intensity * 0.3f);
                Fill.square(x, y, size * tilesize / 2f - 2f);
                Draw.blend();
                Draw.color();
            }
        }
    }
}