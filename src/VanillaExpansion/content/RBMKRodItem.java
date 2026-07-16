package VanillaExpansion.content;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * RBMK燃料棒物品
 * 参考HBM's Nuclear Tech中的ItemRBMKRod
 */
public class RBMKRodItem extends Item{
    public float yield = 1f; // 燃料产出率/丰度
    public float heat = 10f; // 每tick产生的热量
    public float selfRate = 0f; // 自发裂变率
    public float diffusion = 1f; // 热扩散率
    public float meltingPoint = 2000f; // 熔点
    public boolean isNeutronSource = false; // 是否为中子源
    public float enrichment = 1f; // 燃料富集度
    
    public RBMKRodItem(String name, Color color){
        super(name, color);
        cost = 1000;
    }
    
    @Override
    public void setStats(){
        super.setStats();
        
        stats.add(Stat.abilities, "[gold]Enrichment: " + (int)(enrichment * 100) + "%");
        stats.add(Stat.abilities, "[gold]Heat: " + (int)heat + "°C/t");
        stats.add(Stat.abilities, "[gold]Diffusion: " + diffusion);
        stats.add(Stat.abilities, "[gold]Melting Point: " + (int)meltingPoint + "°C");
        
        if(isNeutronSource){
            stats.add(Stat.abilities, "[red]Neutron Source");
        }
    }
}
