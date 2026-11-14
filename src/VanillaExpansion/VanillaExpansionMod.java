package VanillaExpansion;

import VanillaExpansion.content.*;
import VanillaExpansion.effects.OneBitRenderer;
import VanillaExpansion.expand.graphics.VECacheLayer;
import VanillaExpansion.expand.graphics.VEShaders;
import VanillaExpansion.expand.type.unit.UnitEventUpdateManager;
import VanillaExpansion.expand.ui.HotbarUI;
import VanillaExpansion.expand.world.block.RectangleBlock;
import VanillaExpansion.ui.VEFonts;
import VanillaExpansion.ui.bossrush.BossRushRegistry;
import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.graphics.Color;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.graphics.g2d.Font;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.ui.Fonts;
import mindustry.game.EventType;
import mindustry.mod.Mod;
import VanillaExpansion.expand.graphics.LensShockwaveFX;
import VanillaExpansion.expand.input.VEInputHandler;
import mindustry.ui.dialogs.BaseDialog;
import java.util.Arrays;
import static mindustry.Vars.*;
public class VanillaExpansionMod extends Mod {
    private final boolean textTest = false; //字体测试开关
    public static HotbarUI HotbarUI = new HotbarUI();
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
            HotbarUI.init();
            //new HotbarDebugUI().init();//狗屎调试UI,会导致内存疯涨
            BossRushRegistry.register();
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
            //VEDebugHook.register();//狗屎调试UI,会导致内存疯涨
            if (payloadFragment != null) {
                Table itemInv = ui.hudGroup.find("inventory");
                payloadFragment.table.visible = itemInv != null && itemInv.visible && !state.isMenu();
                payloadFragment.rebuild();
            }
            if (VanillaExpansion.expand.ui.HotbarUI.instance != null) {
                VanillaExpansion.expand.ui.HotbarUI.instance.updateVisibility();
            }
        });

        //全图腐蚀处理
        InitCorrosion.init();
        //雷霆大字系统
        if (!Vars.headless) {
            InitEndfieldTitle.init();
        }
        //铁堡垒生成
        InitFerricFortress.init();
        //简易反作弊
        InitAntiCheat.init();
        //黑白闪屏渲染
        OneBitRenderer.init();

        //加载设置
        if(!headless){
            Events.on(EventType.ClientLoadEvent.class, e -> {
                VESettings.loadUI();
            });
        }
        //单位事件更新重置
        Events.on(EventType.WorldLoadBeginEvent.class, e -> {
            UnitEventUpdateManager.clear();
        });
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
        RectangleBlock.registerRectangleTile();
        VEShaders.load();
        VECacheLayer.init();
        //VanillaExpansion.effects.SpecialDeathEffects.load();
        //VanillaExpansion.expand.special.SpecialContent.load();
        VEItems.load();
        VEJSLiquids.load();
        VELiquids.load();
        VEJSStatuses.load();
        VanillaExpansion.content.VEUnitTypes.load();
        VEJSBlocks.load();
        VEBlocks.load();
        VEEnvironBlocks.load();
        VEJSUnitTypes.load();
        VEJSPlanets.load();
        VEPlanets.load();
        VETechTree.load();
        VEFonts.loadFonts();
        //VEModifiedUnitTypes.load();
        VESettings.load();
        //VEFontIcons.load();

        Fi root = Vars.mods.getMod(VanillaExpansionMod.class).root;
        Log.info("Mod assets: " + Arrays.toString(root.list()));
        String locale = Core.settings.getString("locale", "en");
        Log.info("Locale : " + locale);
    }
}