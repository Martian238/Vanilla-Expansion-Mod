package VanillaExpansion.ui.debug;

import VanillaExpansion.expand.world.block.debug.DebugBlock;
import VanillaExpansion.expand.world.block.debug.DebugModule;
import VanillaExpansion.expand.world.block.debug.DebugModuleRegistry;
import VanillaExpansion.ui.VEStyles;
import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.BorderImage;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

public class DebugModuleDialog extends BaseDialog {
    public static final float BUTTON_SIZE = 42f;
    public static final float PANE_WIDTH = 560f;
    public static final float PANE_HEIGHT = 440f;
    public static final float ROW_GAP = 4f;
    public static final float CARD_GAP = 6f;
    public static final float CARD_MARGIN = 8f;
    public static final float BORDER_THICKNESS = 2f;
    public static final Color BORDER_ENABLED = Pal.accent;
    public static final Color BORDER_DISABLED = Color.gray;
    public static final Color BORDER_UNATTACHED = Color.darkGray;
    public static final String TITLE_KEY = "debug.modules.title";
    public static final String TITLE_DEFAULT = "调试模块 / Debug Modules";
    public static final String EMPTY_KEY = "debug.modules.empty";
    public static final String EMPTY_DEFAULT = "无已注册模块 / No modules registered";
    public static final String ADD_KEY = "debug.modules.add";
    public static final String ADD_DEFAULT = "添加模块 / Add module";
    public static final String REMOVE_KEY = "debug.modules.remove";
    public static final String REMOVE_DEFAULT = "移除模块 / Remove module";
    public static final String ENABLE_KEY = "debug.modules.enable";
    public static final String ENABLE_DEFAULT = "启用或禁用模块 / Toggle module";
    public static final String CLEAR_KEY = "debug.modules.clear";
    public static final String CLEAR_DEFAULT = "清空模块 / Clear modules";
    public static final String REFRESH_KEY = "debug.modules.refresh";
    public static final String REFRESH_DEFAULT = "刷新 / Refresh";
    public static final String CLOSE_KEY = "debug.modules.close";
    public static final String CLOSE_DEFAULT = "关闭 / Close";
    public static final String NOT_ADDED_KEY = "debug.modules.notAdded";
    public static final String NOT_ADDED_DEFAULT = "未添加到方块 / Not attached";

    private final DebugBlock.DebugBuild build;
    private final Table list = new Table();
    private final ScrollPane pane = new ScrollPane(list);

    // 构造模块管理界面
    public DebugModuleDialog(DebugBlock.DebugBuild build) {
        super(Core.bundle.get(TITLE_KEY, TITLE_DEFAULT));
        this.build = build;
        VEStyles.load();
        setFillParent(false);
        closeOnBack();
        list.background(Styles.black3);
        list.margin(8f);
        pane.setFadeScrollBars(false);
        pane.setScrollingDisabled(true, false);
        cont.add(header()).growX().row();
        cont.add(pane).size(PANE_WIDTH, PANE_HEIGHT).row();
        cont.add(actionBar()).growX().padTop(6f).row();
        buttons.defaults().size(BUTTON_SIZE).pad(4f);
        buttons.button(Icon.cancel, VEStyles.square, this::hide).tooltip(Core.bundle.get(CLOSE_KEY, CLOSE_DEFAULT));
        update(() -> {
            if (build.tile == null || build.tile.build != build) hide();
        });
        rebuild();
    }

    // 构建标题栏
    private Table header() {
        Table table = new Table();
        table.left();
        table.add("[accent]" + build.block.localizedName + "[]").left().padRight(10f);
        table.add("[lightgray]" + build.moduleSlots.size + " / " + ((DebugBlock) build.block).maxModules + "[]").left();
        return table;
    }

    // 构建底部操作栏
    private Table actionBar() {
        Table table = new Table();
        table.left();
        table.defaults().size(BUTTON_SIZE).pad(ROW_GAP);
        table.button(Icon.refresh, VEStyles.square, this::rebuild).tooltip(Core.bundle.get(REFRESH_KEY, REFRESH_DEFAULT));
        table.button(Icon.trash, VEStyles.squareRemove, () -> configureAndRebuild(DebugBlock.CONFIG_CLEAR))
                .tooltip(Core.bundle.get(CLEAR_KEY, CLEAR_DEFAULT));
        return table;
    }

    // 重建模块卡片列表
    private void rebuild() {
        list.clear();
        Seq<String> ids = DebugModuleRegistry.ids();
        for (int i = 0; i < build.moduleSlots.size; i++) {
            String id = build.moduleSlots.get(i).id;
            if (!ids.contains(id)) ids.add(id);
        }
        if (ids.isEmpty()) {
            list.add(Core.bundle.get(EMPTY_KEY, EMPTY_DEFAULT)).wrap().color(Color.lightGray).pad(16f).row();
            return;
        }
        ids.sort();
        for (int i = 0; i < ids.size; i++) buildCard(ids.get(i));
    }

    // 构建单个模块卡片
    private void buildCard(String id) {
        DebugBlock.ModuleSlot slot = build.getSlot(id);
        Table body = new Table();
        body.top().left();
        body.margin(CARD_MARGIN);

        Table head = new Table();
        head.left();
        head.add("[lightgray]" + id + "[]").left().growX();
        head.add(moduleName(slot, id)).right().color(slot != null && slot.enabled ? Color.white : Color.lightGray);
        body.add(head).growX().row();

        if (slot != null) {
            slot.module.buildConfig(build, body, this::requestRebuild);
        } else {
            body.add(Core.bundle.get(NOT_ADDED_KEY, NOT_ADDED_DEFAULT)).left().color(Color.gray).padTop(4f).row();
        }

        body.table(actions -> {
            actions.right();
            actions.defaults().size(BUTTON_SIZE).pad(ROW_GAP);
            if (slot == null) {
                actions.button(Icon.add, VEStyles.squareAdd, () -> configureAndRebuild(DebugBlock.CONFIG_ADD_PREFIX + id))
                        .tooltip(Core.bundle.get(ADD_KEY, ADD_DEFAULT));
            } else {
                actions.button(slot.enabled ? Icon.pause : Icon.play, VEStyles.squareToggle,
                                () -> configureAndRebuild((slot.enabled ? DebugBlock.CONFIG_DISABLE_PREFIX : DebugBlock.CONFIG_ENABLE_PREFIX) + id))
                        .tooltip(Core.bundle.get(ENABLE_KEY, ENABLE_DEFAULT));
                actions.button(Icon.trash, VEStyles.squareRemove, () -> configureAndRebuild(DebugBlock.CONFIG_REMOVE_PREFIX + id))
                        .tooltip(Core.bundle.get(REMOVE_KEY, REMOVE_DEFAULT));
            }
        }).right().padTop(4f).row();

        Table card = new Table();
        card.background(Styles.black6);

        BorderImage border = new BorderImage(Tex.whiteui);
        border.thickness = BORDER_THICKNESS;
        border.color.a = 0f;
        border.borderColor = slot == null ? BORDER_UNATTACHED : (slot.enabled ? BORDER_ENABLED : BORDER_DISABLED);
        card.stack(border, body).growX();

        list.add(card).growX().padBottom(CARD_GAP).row();
    }

    // 返回卡片显示的模块名
    private String moduleName(DebugBlock.ModuleSlot slot, String id) {
        if (slot != null) return slot.module.displayName();
        DebugModule module = DebugModuleRegistry.create(id);
        return module == null ? id : module.displayName();
    }

    // 延迟重建以避开按钮事件
    private void requestRebuild() {
        Core.app.post(this::rebuild);
    }

    // 应用配置并刷新界面
    private void configureAndRebuild(String command) {
        build.configure(command);
        rebuild();
    }
}
