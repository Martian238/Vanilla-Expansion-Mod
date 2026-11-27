package VanillaExpansion.effects;
import arc.Events;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.EventType;
public class OneBitRenderer {
    private static OneBitShader shader;
    private static boolean enabled = false;
    private final static boolean debug = true;
    public static void init() {
        if (!Vars.headless) {
            shader = new OneBitShader();
        }
        Events.run(EventType.Trigger.drawOver, () -> {
            if (!enabled) return;
            Vars.renderer.effectBuffer.blit(shader);
        });
    }
    public static void enable() { enabled = true; if(debug) Log.info("enable one bit shader"); }
    public static void disable() { enabled = false; if(debug) Log.info("disable one bit shader"); }
    public static boolean isEnabled() { return enabled; }
}
