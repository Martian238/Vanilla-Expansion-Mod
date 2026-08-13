package VanillaExpansion;

import VanillaExpansion.content.*;
import VanillaExpansion.expand.graphics.VECacheLayer;
import VanillaExpansion.expand.graphics.VEShaders;
import arc.Events;
import arc.math.Mathf;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Liquids;
import mindustry.content.Planets;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.mod.Mod;
import VanillaExpansion.expand.graphics.LensShockwaveFX;
import VanillaExpansion.expand.input.VEInputHandler;
import mindustry.world.Block;
import mindustry.world.blocks.liquid.LiquidBlock;
import mindustry.world.blocks.liquid.LiquidBridge;
import mindustry.world.meta.Env;

import static mindustry.Vars.control;
import static mindustry.Vars.state;
import static mindustry.Vars.ui;


public class VanillaExpansionMod extends Mod {

    private static float timer = 0f;
    private static float checkInterval;
    public static Seq<String> blockWhitelist1 = Seq.with(
            "liquid-source","liquid-void","incinerator"
    );
    public static Seq<String> blockWhitelist2 = Seq.with(

            "ve-silicide-fluid-source","ve-silicide-fluid-void",
            "ve-silver-conduit","ve-silver-conduit-armored","ve-valve-fluid-cross","ve-valve-fluid-distribute",
            "ve-silver-bridge"
    );
    public static Seq<String> erekirBlockWhitelist = Seq.with(
            "reinforced-conduit","reinforced-bridge-conduit","reinforced-liquid-junction",
            "reinforced-liquid-router","reinforced-liquid-container","reinforced-liquid-tank",
            "reinforced-pump","slag-incinerator"
    );
    public static boolean hasCorrosive;

    public static MultiCrafterPayloadFragment payloadFragment;
    @Override
    public void init() {
        ContentOrderGuard.init();
        LensShockwaveFX.init();

        // 替换输入处理器（仅客户端，移动端除外）
        Events.on(EventType.ClientLoadEvent.class, e -> {
            if(!Vars.mobile){
                control.setInput(new VEInputHandler());
            }
        });

        // 等待 UI 就绪
        Events.run(EventType.Trigger.uiDrawBegin, () -> {
            if (payloadFragment == null) {
                Table itemInv = ui.hudGroup.find("inventory");
                if (itemInv != null) {
                    payloadFragment = new MultiCrafterPayloadFragment();
                    payloadFragment.build(itemInv.parent);
                }
            }
        });

        // 每帧更新
        Events.run(EventType.Trigger.update, () -> {
            if (payloadFragment != null) {
                Table itemInv = ui.hudGroup.find("inventory");
                payloadFragment.table.visible = itemInv != null && itemInv.visible && !state.isMenu();
                payloadFragment.rebuild();
            }
        });


        //全图酸腐蚀处理

            Events.run(EventType.Trigger.update, () -> {
                if(state.isPaused() || !state.rules.fire) return;
                timer += Time.delta;
                if(checkInterval < 10f){
                    checkInterval = 60f;//防止卡毙掉
                }
                if(timer < checkInterval) return;
                timer = 0f;
                hasCorrosive = false;
                for(Building building : Groups.build){
                    if(building.block instanceof LiquidBlock || building.block instanceof LiquidBridge){
                        if(building.liquids.get(VEJSLiquids.acid) > 0.01f && !blockWhitelist1.contains(building.block.name) && !blockWhitelist2.contains(building.block.name) && !(erekirBlockWhitelist.contains(building.block.name) && !state.rules.hasEnv(Env.scorching))){
                            if(Mathf.chanceDelta(0.33f)) {
                                building.damagePierce(Math.abs(Mathf.range(0.01f, 0.1f)) * building.block.health * (building.liquids.get(VEJSLiquids.acid) / building.block.liquidCapacity));
                                building.liquids.remove(VEJSLiquids.acid, 0.1f);
                            }
                            hasCorrosive = true;
                        }
                    }
                }
                if(hasCorrosive){
                    checkInterval = 10f;
                }else{
                    checkInterval = 60f;
                }
            });

    }




    @Override
    public void loadContent(){
        VEShaders.load();
        VECacheLayer.init();
        //VanillaExpansion.content.VEStuffTypes.load();
        //VanillaExpansion.effects.SpecialDeathEffects.load();
        //VanillaExpansion.expand.special.SpecialContent.load();
        VEItems.load();
        VEJSLiquids.load();
        VELiquids.load();
        //VanillaExpansion.content.VEUnitTypes.load();
        VEJSBlocks.load();
        VEBlocks.load();
        VEPlanets.load();
        CustomFx.load();
        VETechTree.load();
    }


    public class CorrosiveLiquidListener{





    }
}