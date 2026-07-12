package VanillaExpansion;


import arc.Events;
import arc.scene.ui.layout.Table;
import mindustry.game.EventType;
import mindustry.mod.Mod;

import static mindustry.Vars.state;
import static mindustry.Vars.ui;


public class VanillaExpansionMod extends Mod {
    public static MultiCrafterPayloadFragment payloadFragment;
    @Override
    public void init() {
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







}