package VanillaExpansion;

import VanillaExpansion.content.*;
import VanillaExpansion.expand.graphics.VECacheLayer;
import VanillaExpansion.expand.graphics.VEShaders;
import VanillaExpansion.expand.type.unit.IronGolemType;
import VanillaExpansion.expand.type.unit.IronGolemUnit;
import VanillaExpansion.ui.VEFonts;
import arc.Core;
import arc.Events;
import arc.audio.Sound;
import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.Texture;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.graphics.g2d.Font;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.entities.Effect;
import mindustry.entities.Units;
import mindustry.game.Team;
import mindustry.gen.Sounds;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.mod.Mods;
import mindustry.type.Planet;
import mindustry.ui.Fonts;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.mod.Mod;
import VanillaExpansion.expand.graphics.LensShockwaveFX;
import VanillaExpansion.expand.input.VEInputHandler;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.turrets.BaseTurret;
import mindustry.world.blocks.distribution.Router;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.liquid.LiquidBlock;
import mindustry.world.blocks.liquid.LiquidBridge;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.meta.Env;

import java.util.Arrays;
import java.util.Objects;

import static mindustry.Vars.*;


public class VanillaExpansionMod extends Mod {

    private static float timer = 0f;
    private static float checkInterval;
    public static Seq<String> blockWhitelist1 = Seq.with(
            "liquid-source","liquid-void","incinerator"
    );
    public static Seq<String> blockWhitelist2 = Seq.with(
            "ve-silicide-fluid-source","ve-silicide-fluid-void",
            "ve-silver-conduit","ve-silver-conduit-armored","ve-valve-fluid-cross","ve-valve-fluid-distribute",
            "ve-silver-bridge","ve-chained-pump","ve-fluid-sorter"
    );
    public static Seq<String> erekirBlockWhitelist = Seq.with(
            "reinforced-conduit","reinforced-bridge-conduit","reinforced-liquid-junction",
            "reinforced-liquid-router","reinforced-liquid-container","reinforced-liquid-tank",
            "reinforced-pump","slag-incinerator"
    );
    public static boolean hasCorrosive;

    private final boolean textTest = false; //字体测试开关

    private TextureRegion titleRegion = new TextureRegion();;
    private TextureRegion titleShadowRegion = new TextureRegion();
    private boolean drawTitle = false;
    private float titleAlpha = 1f;
    private Sound titleSound = Sounds.none;
    private float titleFadeTime = 0f;
    private Seq<Mods.LoadedMod> otherMods = new Seq<>();
    private boolean hasCheat = false;
    private boolean goDie = false;



    public static MultiCrafterPayloadFragment payloadFragment;
    @Override
    public void init() {
        ContentOrderGuard.init();
        LensShockwaveFX.init();

        // 替换输入处理器并测试VEFonts（仅客户端，移动端除外）
        Events.on(EventType.ClientLoadEvent.class, e -> {
            if(!Vars.mobile){
                control.setInput(new VEInputHandler());
            }
            if(textTest) {
                // 延迟10秒后显示VEFonts测试UI
                Time.runTask(10f, () -> {
                    BaseDialog dialog = new BaseDialog("VEFonts测试");
                    Font veFont = VEFonts.novo != null ? VEFonts.novo : Fonts.def;
                    Label veFontLabel = new Label("VEFonts Test Text - Novo Custom Font", new Label.LabelStyle(veFont, Color.white));
                    dialog.cont.add(veFontLabel).row();
                    dialog.cont.button("关闭", dialog::hide).size(100f, 50f);
                    dialog.show();
                });
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


        //全图酸腐蚀处理

            Events.run(EventType.Trigger.update, () -> {
                if(state.isPaused() || !state.rules.fire) return;
                timer += Time.delta;
                if(checkInterval < 10f){
                    checkInterval = 60f;//防止卡毙掉
                }
                if(timer < checkInterval) return;
                timer = 0f;
                hasCorrosive = false;
                for(Building building : Groups.build){
                    if(building.block instanceof LiquidBlock || building.block instanceof LiquidBridge){
                        if(building.liquids.get(VEJSLiquids.acid) > 0.01f && !blockWhitelist1.contains(building.block.name) && !blockWhitelist2.contains(building.block.name) && !(erekirBlockWhitelist.contains(building.block.name) && !state.rules.hasEnv(Env.scorching))){
                            if(Mathf.chanceDelta(0.33f)) {
                                building.damagePierce(Math.abs(Mathf.range(0.01f, 0.1f)) * building.block.health * (building.liquids.get(VEJSLiquids.acid) / building.block.liquidCapacity));
                                building.liquids.remove(VEJSLiquids.acid, 0.1f);
                            }
                            hasCorrosive = true;
                        }
                    }
                }
                if(hasCorrosive){
                    checkInterval = 10f;
                }else{
                    checkInterval = 60f;
                }
            });

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

        //铁堡垒生成
        Events.on(EventType.BlockBuildEndEvent.class, e -> {
            //Log.info("build end");
            if(Objects.equals(e.tile.block().name, "ve-watermelon")){
                //Log.info("watermelon");
                int x = e.tile.x;
                int y = e.tile.y;
                if(checkFerrumWalls(x, y, -90, e.unit.team)){
                    createFerricFortress(x, y, -90, e.unit, e.unit.team);
                    return;
                }
                if(checkFerrumWalls(x, y, 0, e.unit.team)){
                    createFerricFortress(x, y, 0, e.unit, e.unit.team);
                    return;
                }
                if(checkFerrumWalls(x, y, 90, e.unit.team)){
                    createFerricFortress(x, y, 90, e.unit, e.unit.team);
                    return;
                }
                if(checkFerrumWalls(x, y, 180, e.unit.team)){
                    createFerricFortress(x, y, 180, e.unit, e.unit.team);
                }
            }
        });

    }

    public boolean checkFerrumWall(int x, int y, boolean mustNull, Team team){
        Tile tile = Vars.world.tile(x, y);
        if(tile == null || tile.block() instanceof Floor){
            return mustNull;
        }else{
            return (Objects.equals(tile.block().name, "ve-ferrum-wall")) && !mustNull && tile.team() == team;
        }
    }

    public boolean checkFerrumWalls(int x, int y, float rot, Team team){
        return checkFerrumWall(x + (int) Mathf.cosDeg(rot), y + (int) Mathf.sinDeg(rot), false, team) &&
        checkFerrumWall(x + 2 * (int) Mathf.cosDeg(rot), y + 2 * (int) Mathf.sinDeg(rot), false, team) &&
        checkFerrumWall(x + (int) Mathf.cosDeg(rot) + (int) Mathf.cosDeg(rot + 90f),
                y + (int) Mathf.sinDeg(rot) + (int) Mathf.sinDeg(rot + 90f), false, team) &&
        checkFerrumWall(x + (int) Mathf.cosDeg(rot) + (int) Mathf.cosDeg(rot - 90f),
                y + (int) Mathf.sinDeg(rot) + (int) Mathf.sinDeg(rot - 90f), false, team) &&
                checkFerrumWall(x + 2 * (int) Mathf.cosDeg(rot) + (int) Mathf.cosDeg(rot + 90f),
                        y + 2 * (int) Mathf.sinDeg(rot) + (int) Mathf.sinDeg(rot + 90f), true, team) &&
                checkFerrumWall(x + 2 * (int) Mathf.cosDeg(rot) + (int) Mathf.cosDeg(rot - 90f),
                        y + 2 * (int) Mathf.sinDeg(rot) + (int) Mathf.sinDeg(rot - 90f), true, team);
    }

    public void createFerricFortress(int x, int y, float rot, Unit owner, Team team){
        removeFerrumWall(x, y);
        removeFerrumWall(x + (int) Mathf.cosDeg(rot), y + (int) Mathf.sinDeg(rot));
        removeFerrumWall(x + 2 * (int) Mathf.cosDeg(rot), y + 2 * (int) Mathf.sinDeg(rot));
        removeFerrumWall(x + (int) Mathf.cosDeg(rot) + (int) Mathf.cosDeg(rot + 90f),
                y + (int) Mathf.sinDeg(rot) + (int) Mathf.sinDeg(rot + 90f));
        removeFerrumWall(x + (int) Mathf.cosDeg(rot) + (int) Mathf.cosDeg(rot - 90f),
                y + (int) Mathf.sinDeg(rot) + (int) Mathf.sinDeg(rot - 90f));
        Tile tile = Vars.world.tile(x, y);
        if(tile != null) {
            Unit fu = VEJSUnitTypes.ferricFortress.create(team);
            fu.set(tile.worldx() + 2 * tilesize * Mathf.cosDeg(rot), tile.worldy() + 2 * tilesize * Mathf.sinDeg(rot));
            Events.fire(new EventType.UnitCreateEvent(fu, null, owner));
            if (!Vars.net.client()) {
                fu.add();
                Units.notifyUnitSpawn(fu);
            }
            fu.rotation = rot;
            Sounds.unitCreateBig.at(tile.worldx(), tile.worldy(), 1f, 0.7f);
            Effect.shake(4, 12, tile.worldx(), tile.worldy());
            //Log.info("golem");
        }
    }

    public void removeFerrumWall(int x, int y){
        Tile tile = Vars.world.tile(x, y);
        if(tile != null && !(tile.block() instanceof Floor)){
            tile.setAir();
            Fx.dynamicExplosion.at(tile.worldx(), tile.worldy(), 1f);
        }
    }


    public void testTitle(Planet planet, String name, float delay, float duration){
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

    public void testTitle(String planetName, int startSector, String name, float delay, float duration){
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



    public void displayTitle(String titleName, float delay, float duration, float fadeTime, boolean sound){
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

    private void findFiles(String titleName, String soundName, String shadowName){
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


    @Override
    public void loadContent(){

        goDie = hasCheat = false;

        VEShaders.load();
        VECacheLayer.init();
        //VanillaExpansion.content.VEStuffTypes.load();
        //VanillaExpansion.effects.SpecialDeathEffects.load();
        //VanillaExpansion.expand.special.SpecialContent.load();
        VEItems.load();
        VEJSLiquids.load();
        VELiquids.load();
        //VanillaExpansion.content.VEUnitTypes.load();
        VEJSBlocks.load();
        VEBlocks.load();
        VEEnvironBlocks.load();
        VEJSUnitTypes.load();
        VEPlanets.load();
        VETechTree.load();
        VEFonts.loadFonts();

        Fi root = Vars.mods.getMod(VanillaExpansionMod.class).root;
        Log.info("Mod assets: " + Arrays.toString(root.list()));
        String locale = Core.settings.getString("locale", "en");
        Log.info("Locale : " + locale);
    }



}