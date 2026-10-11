package VanillaExpansion;

import VanillaExpansion.content.*;
import VanillaExpansion.expand.graphics.VECacheLayer;
import VanillaExpansion.expand.graphics.VEShaders;
import VanillaExpansion.ui.VEFonts;
import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.graphics.Color;
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
import mindustry.mod.Mods;
import mindustry.ui.Fonts;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.mod.Mod;
import VanillaExpansion.expand.graphics.LensShockwaveFX;
import VanillaExpansion.expand.input.VEInputHandler;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.liquid.LiquidBlock;
import mindustry.world.blocks.liquid.LiquidBridge;
import mindustry.world.meta.Env;

import java.util.Arrays;
import java.util.Objects;

import static mindustry.Vars.*;


public class VanillaExpansionMod extends Mod {



    private final boolean textTest = false; //字体测试开关




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

        //全图腐蚀处理
        InitCorrosion.init();
        //雷霆大字系统
        InitEndfieldTitle.init();
        //铁堡垒生成
        InitFerricFortress.init();
        //简易反作弊
        InitAntiCheat.init();







        /*
        ContextFactory.initGlobal(new SecureContextFactory());

        Events.on(EventType.ClientLoadEvent.class, e -> {
            try{
                java.lang.reflect.Field scopeField = mindustry.mod.Mods.class.getDeclaredField("scope");
                scopeField.setAccessible(true);
                Scriptable scope = (Scriptable) scopeField.get(Vars.mods);
                if(scope != null){
                    scope.delete("Packages");
                    scope.delete("java");
                }
            }catch(Exception ignored){}
        });
        */
    }



    @Override
    public void loadContent(){

        EntityRegister.load();
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
        VEJSPlanets.load();
        VEPlanets.load();
        VETechTree.load();
        VEFonts.loadFonts();
        //VEModifiedUnitTypes.load();

        Fi root = Vars.mods.getMod(VanillaExpansionMod.class).root;
        Log.info("Mod assets: " + Arrays.toString(root.list()));
        String locale = Core.settings.getString("locale", "en");
        Log.info("Locale : " + locale);
    }



}