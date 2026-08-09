package VanillaExpansion;

import VanillaExpansion.expand.graphics.VECacheLayer;
import VanillaExpansion.expand.graphics.VEShaders;
import arc.Events;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.input.DesktopInput;
import mindustry.mod.Mod;
import VanillaExpansion.expand.graphics.LensShockwaveFX;
import VanillaExpansion.expand.input.VEInputHandler;

import static mindustry.Vars.control;
import static mindustry.Vars.state;
import static mindustry.Vars.ui;


public class VanillaExpansionMod extends Mod {
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
    }
    @Override
    public void loadContent(){
        VEShaders.load();
        VECacheLayer.init();
        //VanillaExpansion.content.VEStuffTypes.load();
        //VanillaExpansion.effects.SpecialDeathEffects.load();
        //VanillaExpansion.expand.special.SpecialContent.load();
        VanillaExpansion.content.VEItems.load();
        VanillaExpansion.content.VELiquids.load();
        //VanillaExpansion.content.VEUnitTypes.load();
        VanillaExpansion.content.VEJSBlocks.load();
        VanillaExpansion.content.VEBlocks.load();
        VanillaExpansion.content.VEPlanets.load();
        VanillaExpansion.content.CustomFx.load();
        VanillaExpansion.content.VETechTree.load();
    }
}