package VanillaExpansion.expand.ui;
import VanillaExpansion.expand.abilities.HotbarAbility;
import VanillaExpansion.expand.abilities.HotbarWeaponSlot;
import arc.Core;
import arc.graphics.Color;
import arc.scene.event.Touchable;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.util.Strings;
import mindustry.Vars;
import mindustry.gen.Unit;
import mindustry.type.Item;
public class HotbarDebugUI {
    private final String[] lastTexts = new String[7];
    public static HotbarDebugUI instance;
    //显示调试开关
    public static boolean alwaysVisible = true;
    private Table root;
    private Table wrapper;   // 外层容器，固定宽度 280
    private Label titleLabel, unitLabel, slotLabel, itemLabel,
            weaponLabel, healthLabel, speedLabel, statusLabel;
    private boolean initialized = false;
    private float refreshTimer = 0f;
    private static final float REFRESH_INTERVAL = 6f;
    public void init() {
        instance = this;
        if (initialized) return;
        initialized = true;
        root = new Table();
        root.setFillParent(false);
        root.top().right();
        root.margin(8f);
        root.touchable = Touchable.disabled;
        root.defaults().left().width(280f);
        titleLabel = root.add("[accent]Hotbar Debug[]").get();
        root.row();
        root.image().color(Color.darkGray).height(1f).width(280f)
                .padTop(2f).padBottom(4f).get();
        root.row();
        unitLabel    = root.add("").get(); root.row();
        slotLabel    = root.add("").get(); root.row();
        itemLabel    = root.add("").get(); root.row();
        weaponLabel  = root.add("").get(); root.row();
        healthLabel  = root.add("").get(); root.row();
        speedLabel   = root.add("").get(); root.row();
        statusLabel  = root.add("").get(); root.row();
        addToScene();
        root.visible = true;
        if (wrapper != null) wrapper.visible = true;
    }
    private void addToScene() {
        if (root == null) return;
        if (root.parent != null) {
            root.remove();
        }
        Core.scene.add(root);
        root.pack();
        root.setPosition(
                Core.graphics.getWidth() - root.getWidth() - 8f,
                Core.graphics.getHeight() - root.getHeight() - 8f
        );
    }
    public void dispose() {
        if (root != null) {
            root.remove();
            root = null;
        }
        initialized = false;
    }
    public void setVisible(boolean v) {
        alwaysVisible = v;
        if (root != null) root.visible = v;
    }
    public boolean isVisible() {
        return root != null && root.visible;
    }
    public void update() {
        if (alwaysVisible) {
            if (root == null) {
                init();
            } else if (root.parent == null) {
                addToScene();
            }
            if (!root.visible) root.visible = true;
        }
        if (root == null || !root.visible) return;
        // 节流
        if (++refreshTimer < REFRESH_INTERVAL) return;
        refreshTimer = 0f;
        refresh();
    }

    // 刷新
    private void refresh() {
        Unit unit = Vars.player != null ? Vars.player.unit() : null;
        if (unit == null) {
            setIfChanged(0, unitLabel, "[gray]单位:[] [red]无（未控制）[]");
            setIfChanged(1, slotLabel, "");
            setIfChanged(2, itemLabel, "");
            setIfChanged(3, weaponLabel, "");
            setIfChanged(4, healthLabel, "");
            setIfChanged(5, speedLabel, "");
            setIfChanged(6, statusLabel, "");
            return;
        }
        setIfChanged(0, unitLabel, "[gray]单位:[] [white]" + unit.type.name + "[]");

        HotbarUI ui = HotbarUI.instance;
        if (ui == null) {
            setIfChanged(2, itemLabel, "[gray]物品栏:[] [red]未初始化[]");
            return;
        }
        int selected = ui.getSelected();
        Item item = ui.getItem(selected);
        setIfChanged(1, slotLabel, "[gray]选中格:[] [yellow]" + (selected + 1)
                + "[] / " + HotbarUI.SLOT_COUNT);
        setIfChanged(2, itemLabel, item == null
                ? "[gray]物品:[] [red]空[]"
                : "[gray]物品:[] [white]" + item.name + "[]");
        // 找 HotbarAbility
        HotbarAbility hab = null;
        for (var ab : unit.abilities) {
            if (ab instanceof HotbarAbility h) { hab = h; break; }
        }
        if (hab == null) {
            setIfChanged(3, weaponLabel, "[gray]武器:[] [red]无 HotbarAbility[]");
            setIfChanged(4, healthLabel, "[gray]血量:[] [white]" + (int) unit.health
                    + "[] / " + (int) unit.maxHealth);
            setIfChanged(5, speedLabel, "[gray]速度乘数:[] [white]"
                    + Strings.autoFixed(unit.speedMultiplier(), 2));
            setIfChanged(6, statusLabel, "");
            return;
        }
        // 武器
        HotbarWeaponSlot slot = hab.getActiveSlot();
        if (slot == null) {
            setIfChanged(3, weaponLabel, "[gray]武器:[] [gray]无槽[]");
        } else if (slot.bullet == null) {
            setIfChanged(3, weaponLabel, "[gray]武器:[] [gray]空弹[] [gray]reload=[]"
                    + Strings.autoFixed(slot.reloadTimer, 1));
        } else {
            setIfChanged(3, weaponLabel, "[gray]武器:[] [green]"
                    + slot.bullet.getClass().getSimpleName()
                    + "[] [gray]reload=[]" + Strings.autoFixed(slot.reloadTimer, 1)
                    + "[]/" + Strings.autoFixed(slot.reload, 1));
        }
        // 血量
        setIfChanged(4, healthLabel, "[gray]血量:[] [white]" + (int) unit.health
                + "[] / [white]" + (int) unit.maxHealth
                + "[] [gray](+[]" + (int) hab.getBonusHealth() + "[])[gray]");
        // 速度
        setIfChanged(5, speedLabel, "[gray]速度乘数:[] [white]"
                + Strings.autoFixed(unit.speedMultiplier(), 2)
                + "[] [gray](+[]" + Strings.autoFixed(hab.getSpeedMul() - 1f, 2) + "[])[gray]");
        // 状态
        if (hab.getBonusStatus() != null) {
            setIfChanged(6, statusLabel, "[gray]状态:[] [orange]" + hab.getBonusStatus().name + "[]");
        } else {
            setIfChanged(6, statusLabel, "[gray]状态:[] [gray]无[]");
        }
    }
    private void setIfChanged(int idx, Label label, String text) {
        if (text == null) text = "";
        if (!text.equals(lastTexts[idx])) {
            label.setText(text);
            lastTexts[idx] = text;
        }
    }
}