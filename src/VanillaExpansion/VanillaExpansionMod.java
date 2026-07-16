package VanillaExpansion;


import VanillaExpansion.expand.world.block.multicrafter.MultiCrafterPayloadFragment;
import arc.Core;
import arc.Events;
import arc.scene.ui.layout.Table;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.input.DesktopInput;
import mindustry.mod.Mod;
import mindustry.ui.dialogs.BaseDialog;

import static mindustry.Vars.state;
import static mindustry.Vars.ui;


public class VanillaExpansionMod extends Mod {
    public static MultiCrafterPayloadFragment payloadFragment;
    @Override
    public void init() {
        //listen for game load event
        Events.on(EventType.ClientLoadEvent.class, e -> {
            if (Vars.control != null && Vars.control.input instanceof DesktopInput) {
                VanillaExpansion.expand.input.VEInputHandler proximaInput = new VanillaExpansion.expand.input.VEInputHandler();
                proximaInput.block = Vars.control.input.block;
                Vars.control.input = proximaInput;
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
        //VanillaExpansion.content.VEStuffTypes.load();
        //VanillaExpansion.effects.SpecialDeathEffects.load();
        //VanillaExpansion.expand.special.SpecialContent.load();
        //VanillaExpansion.content.VEItems.load();
        //VanillaExpansion.content.VELiquids.load();
        //VanillaExpansion.content.VEUnitTypes.load();
        //VanillaExpansion.content.VEBlocks.load();
        //VanillaExpansion.content.VEPlanets.load();
    }
}