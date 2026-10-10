package VanillaExpansion;

import arc.Core;
import arc.Events;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.mod.Mods;

import static mindustry.Vars.state;

public class InitAntiCheat {
    private static Seq<Mods.LoadedMod> otherMods = new Seq<>();
    private static boolean hasCheat = false;
    private static boolean goDie = false;

    public static void init(){
        //战役反作弊，烦死了
        Events.run(EventType.WorldLoadEndEvent.class, () -> {
            hasCheat = goDie = false;
            String locale = Core.settings.getString("locale", "en");
            if(state.isCampaign() && (locale.startsWith("zh_CN") || locale.startsWith("zh_TW")) && !state.getSector().isCaptured() && state.getPlanet().name.startsWith("ve-")) {
                otherMods = Vars.mods.getMods();
                if (otherMods != null) {
                    for (Mods.LoadedMod mod : otherMods) {
                        if (mod.name.startsWith("invincible-cheat") && mod.enabled()) {
                            hasCheat = true;
                        }
                    }
                }
            }
        });
        Events.run(EventType.BlockBuildEndEvent.class, () -> {
            if(hasCheat) {
                if(goDie) Core.app.exit();
                for (Building b : Groups.build) {
                    if (b.block.name.startsWith("invincible-cheat")) {
                        goDie = true;
                        while (true) {}
                    }
                }
            }
        });
        Events.run(EventType.UnitSpawnEvent.class, () -> {
            if(hasCheat){
                if(goDie) Core.app.exit();
                for(Unit u : Groups.unit) {
                    if (u.type.name.startsWith("invincible-cheat")) {
                        goDie = true;
                        while (true) {}
                    }
                }
            }
        });
    }
}
