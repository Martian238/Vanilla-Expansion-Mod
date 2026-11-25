package VanillaExpansion.expand.type.unit;

import arc.Events;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Unit;

public class UnitEventUpdateManager {
    private static Seq<Unit> eventUnits = new Seq<>();
    private static boolean registered = false;

    protected static void add(Unit unit) {
        if (!eventUnits.contains(unit)) {
            eventUnits.add(unit);
            if (!registered) {
                registered = true;
                Events.run(EventType.Trigger.update, () -> {
                    updateUnits();
                });
                Events.run(EventType.Trigger.draw, () -> {
                    drawUnits();
                });
            }
        }
    }

    protected static void remove(Unit unit) {
        eventUnits.remove(unit);
    }

    public static void clear(){
        eventUnits.clear();
    }

    private static void updateUnits(){
        if(eventUnits.isEmpty() || !Vars.state.isGame() || Vars.state.isPaused()){
            return;
        }
        for(Unit unit : eventUnits){
            if(unit instanceof HyperUnit h){
                try{
                    h.actualUpdate();
                }catch(Exception e){}
            }
        }
    }

    private static void drawUnits(){
        if(eventUnits.isEmpty() || !Vars.state.isGame()){
            return;
        }
        for(Unit unit : eventUnits){
            if(unit instanceof HyperUnit h){
                try{
                    h.actualDraw();
                }catch(Exception e){}
            }
        }
    }
}

