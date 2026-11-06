package VanillaExpansion.expand;

import VanillaExpansion.expand.ui.HotbarDebugUI;
import arc.Events;
import mindustry.game.EventType;

public class VEDebugHook {
    public static void register() {
        Events.on(EventType.ClientLoadEvent.class, e -> {
            HotbarDebugUI.instance = new HotbarDebugUI();
            HotbarDebugUI.instance.init();
        });
        Events.run(EventType.Trigger.update, () -> {
            if (HotbarDebugUI.instance != null) {
                HotbarDebugUI.instance.update();
            }
        });
    }
}