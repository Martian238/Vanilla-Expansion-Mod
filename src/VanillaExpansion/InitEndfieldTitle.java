package VanillaExpansion;

import VanillaExpansion.content.VEJSUnitTypes;
import VanillaExpansion.content.VEPlanets;
import arc.Core;
import arc.Events;
import arc.audio.Sound;
import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.Texture;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.Planets;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.type.Planet;
import mindustry.world.blocks.defense.turrets.BaseTurret;
import mindustry.world.blocks.distribution.Router;
import mindustry.world.blocks.storage.CoreBlock;

import java.util.Objects;

import static mindustry.Vars.state;
import static mindustry.Vars.ui;

public class InitEndfieldTitle {
    protected static TextureRegion titleRegion = new TextureRegion();;
    protected static TextureRegion titleShadowRegion = new TextureRegion();
    protected static boolean drawTitle = false;
    protected static float titleAlpha = 1f;
    protected static Sound titleSound = Sounds.none;
    protected static float titleFadeTime = 0f;

    public static void init(){

        //雷霆大字检测
        Events.on(EventType.WorldLoadEndEvent.class, e -> {
            drawTitle = false;
            testTitle(Planets.erekir, "erekir", 160f, 120f);
            testTitle(Planets.serpulo, "serpulo", 160f, 120f);
            testTitle(VEPlanets.proxima, "proxima", 160f, 120f);
            testTitle("ve-cyclant", 170, "cyclant", 160f, 120f);
            testTitle("ve-maress", 43, "maress", 160f, 120f);
            testTitle("ve-sitrullus", 15, "sitrullus", 160f, 120f);
            testTitle("ve-thavina", 0, "thavina", 160f, 120f);
        });
        Events.on(EventType.UnitSpawnEvent.class, e -> {
            if(e.unit.type == VEJSUnitTypes.textTrigger){
                drawTitle = false;
                Log.info("Text trigger created");
                for(Unit u : Groups.unit){
                    if(Objects.equals(u.type.name, "ve-meta") && u.team == Team.crux){
                        displayTitle("hyper", 373f, 240f, 5f, false);
                        break;
                    }
                    if(Objects.equals(u.type.name, "ve-fungitron-mass1")){
                        displayTitle("fungitron", 555f, 115f, 0f, false);
                        break;
                    }
                    if(Objects.equals(u.type.name, "ve-zentack-body")){
                        displayTitle("zentack", 120f, 150f, 0f, false);
                        break;
                    }
                    if(Objects.equals(u.type.name, "ve-chiniun")){
                        displayTitle("chiniun", 495f, 140f, 5f, false);
                        break;
                    }
                }
            }
        });



        //雷霆大字绘制
        Events.run(EventType.Trigger.draw, () -> {
            if(drawTitle){
                float x = Core.camera.position.x;
                float y = Core.camera.position.y;
                float w = Core.camera.width;
                float scale = (float) titleRegion.height / titleRegion.width; // 1080f / 1920f
                Draw.z(Layer.max - 0.01f);
                Draw.color(Color.black);
                Draw.alpha(0.25f * titleAlpha);
                Draw.rect(titleShadowRegion, x, y, w * 0.75f, w * scale * 0.5f, 0f);
                Draw.color(Color.white);
                Draw.alpha(titleAlpha);
                Draw.rect(titleRegion, x, y, w, w * scale, 0f);
                if(titleFadeTime > 0 && titleAlpha < 1f){
                    titleAlpha += 1f / titleFadeTime;
                    if(titleAlpha > 1f) titleAlpha = 1f;
                }
                Draw.color();
            }
        });
    }

    public static void testTitle(Planet planet, String name, float delay, float duration){
        if(state.map != null && state.isCampaign()){
            if(state.getPlanet() == planet && state.getSector().id == planet.startSector){
                for(Building b : Groups.build) {
                    if(b instanceof CoreBlock.CoreBuild && b.team == Team.sharded){
                        Time.run(5f, () -> {
                            boolean hasPlayer = false;
                            for(Unit u : Groups.unit){
                                if((u.spawnedByCore() && u.team == Team.sharded) || u.isPlayer()){
                                    hasPlayer = true;
                                    break;
                                }
                            }
                            for(Building b2 : Groups.build){
                                if((b2.block instanceof BaseTurret || b2.block instanceof Router) && b2.team == Team.sharded){
                                    hasPlayer = true;
                                    break;
                                }
                            }
                            if(!hasPlayer) displayTitle(name, delay - 5f, duration, 0f, true);
                        });
                        break;
                    }
                }
            }
        }
    }

    public static void testTitle(String planetName, int startSector, String name, float delay, float duration){
        if(state.map != null && state.isCampaign()){
            Log.info(state.getPlanet() + " " + state.getPlanet().name);
            if(Objects.equals(state.getPlanet().name, planetName)
                    && state.getSector().id == startSector){
                for(Building b : Groups.build) {
                    if(b instanceof CoreBlock.CoreBuild && b.team == Team.sharded){
                        Time.run(5f, () -> {
                            boolean hasPlayer = false;
                            for(Unit u : Groups.unit){
                                if((u.spawnedByCore() && u.team == Team.sharded) || u.isPlayer()){
                                    hasPlayer = true;
                                    break;
                                }
                            }
                            for(Building b2 : Groups.build){
                                if((b2.block instanceof BaseTurret || b2.block instanceof Router) && b2.team == Team.sharded){
                                    hasPlayer = true;
                                    break;
                                }
                            }
                            if(!hasPlayer) displayTitle(name, delay - 5f, duration, 0f, true);
                        });
                        break;
                    }
                }
            }
        }
    }

    public static void displayTitle(String titleName, float delay, float duration, float fadeTime, boolean sound){
        String locale = Core.settings.getString("locale", "en");
        Log.info("Displaying title: '" + titleName.toUpperCase() + "' in locale: '" + locale + "'");
        drawTitle = false;
        boolean uiShown = ui.hudfrag.shown;
        Vars.ui.hudfrag.shown = false;
        if(locale.startsWith("zh_CN")){
            titleName = titleName + "-zh-cn";
        }else if(locale.startsWith("zh_TW")){
            titleName = titleName + "-zh-tw";
        }else{
            titleName = titleName + "-en";
        }
        if(sound) {
            findFiles(titleName, "titleSound", "circle-soft");
        }else{
            findFiles(titleName, "none", "circle-soft");
        }
        if(fadeTime <= 0){
            titleAlpha = 1f;
            titleFadeTime = 0f;
        }else{
            titleAlpha = 0f;
            titleFadeTime = fadeTime;
        }
        Time.run(delay, () -> {
            drawTitle = true;
            titleSound.at(Core.camera.position, 1f, 1f);
            Time.run(duration, () -> {
                drawTitle = false;
                Vars.ui.hudfrag.shown = uiShown;
            });
        });
    }

    private static void findFiles(String titleName, String soundName, String shadowName){
        Fi root = Vars.mods.getMod(VanillaExpansionMod.class).root;
        //Log.info("mod root: " + root.path() + " exists=" + root.exists());
        Fi fi = findPic(root, titleName + ".png");
        if(fi == null){
            fi = Core.files.internal("titles/" + titleName + ".png");
        }
        if(fi != null && fi.exists()) {
            Texture regionFi = new Texture(fi);
            titleRegion = new TextureRegion(regionFi);
            //Log.info("Title sprite loaded from " + fi.path());
        }else{
            //Log.info("Title sprite not found");
        }

        Fi fiB = findPic(root, shadowName + ".png");
        if(fi == null){
            fiB = Core.files.internal("titles/" + shadowName + ".png");
        }
        if(fiB != null && fiB.exists()) {
            Texture regionFiB = new Texture(fiB);
            titleShadowRegion = new TextureRegion(regionFiB);
        }

        if(soundName != "none") {
            Fi fi2 = findPic(root, soundName + ".ogg");
            if (fi2 == null) {
                fi2 = Core.files.internal("titles/" + soundName + ".ogg");
            }
            if (fi2 != null && fi2.exists()) {
                titleSound = new Sound(fi2);
            }
        }else{
            titleSound = Sounds.none;
        }
    }

    private static Fi findPic(Fi dir, String name){
        if(dir == null || !dir.exists() || !dir.isDirectory()) return null;
        for(Fi f : dir.list()){
            if(f.isDirectory()){
                Fi found = findPic(f, name);
                if(found != null) return found;
            }else if(f.name().equals(name)){
                return f;
            }
        }
        return null;
    }
}
