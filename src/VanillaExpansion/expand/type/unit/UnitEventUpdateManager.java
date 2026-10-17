package VanillaExpansion.expand.type.unit;

import arc.Events;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Unit;

public class UnitEventUpdateManager {
    private static final Seq<Unit> eventUnits = new Seq<>();
    private static boolean registered = false;

    public static void add(Unit unit) {
        if (!eventUnits.contains(unit)) {
            eventUnits.add(unit);
            if (!registered) {
                registered = true;
                Events.run(EventType.Trigger.update, () -> {
                    updateUnits();
                });
            }
        }
    }

    public static void remove(Unit unit) {
        eventUnits.remove(unit);
    }

    public static void clear(){
        eventUnits.clear();
    }

    private static void updateUnits(){
        if(eventUnits.isEmpty() || !Vars.state.isGame() || Vars.state.isPaused()) return;
        for(Unit unit : eventUnits){
            if(unit instanceof HyperUnit h){
                h.actualUpdate();
            }
        }
    }
}

