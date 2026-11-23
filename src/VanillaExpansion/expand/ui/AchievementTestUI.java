package VanillaExpansion.expand.ui;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Group;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Tex;

public class AchievementTestUI {
    // 单例
    public static AchievementTestUI instance;
    // 根容器
    private Table root;
    // 主面板
    private Table panel;
    // 名称输入框
    private TextField nameField;
    // 描述输入框
    private TextField descField;
    // 稀有度输入框
    private TextField rarityField;
    // 默认名称
    private String defaultName = "测试成就";
    // 默认描述
    private String defaultDesc = "这是一个测试成就描述";
    // 默认稀有度
    private String defaultRarity = "1";
    // 面板宽度
    private float panelWidth = 300f;
    // 面板内边距
    private float panelPad = 8f;
    // 输入框高度
    private float fieldHeight = 36f;
    // 按钮高度
    private float buttonHeight = 36f;
    // 左边距
    private float marginLeft = 16f;
    // 底边距
    private float marginBottom = 16f;
    // 提示颜色
    private Color hintColor = Color.valueOf("aaaaaa");
    // 是否可见
    private boolean visible = true;
    // 挂载的父容器
    private Group hostGroup;
    // 是否正在拖拽
    private boolean dragging = false;
    // 拖拽偏移X
    private float dragOffsetX;
    // 拖拽偏移Y
    private float dragOffsetY;
    // 临时坐标
    private final Vec2 tmp = new Vec2();

    // 初始化
    public static void init() {
        if (instance != null) return;
        instance = new AchievementTestUI();
        instance.build();
    }

    // 销毁
    public static void dispose() {
        if (instance == null) return;
        if (instance.root != null) instance.root.remove();
        instance = null;
    }

    // 设置可见性
    public static void setVisible(boolean v) {
        if (instance == null) return;
        instance.visible = v;
        instance.root.visible = v;
    }

    // 切换可见性
    public static void toggle() {
        if (instance == null) return;
        setVisible(!instance.visible);
    }
    public static void update() {
        if (instance == null || instance.root == null) return;
        instance.ensureHost();
        if (instance.root.parent != null
                && instance.root.getZIndex() != instance.root.parent.getChildren().size - 1) {
            instance.root.toFront();
        }
    }
    private void ensureHost() {
        Group target = Core.scene != null ? Core.scene.root : null;
        if (target == null) target = Vars.ui.hudGroup;
        if (target == null) return;
        if (root.parent != target) {
            if (root.parent != null) root.remove();
            target.addChild(root);
            target.toFront();
        }
        hostGroup = target;
    }

    // 构建面板
    private void build() {
        root = new Table();
        root.setFillParent(true);
        root.touchable = Touchable.childrenOnly;
        ensureHost();
        root.toFront();

        panel = new Table();
        panel.setBackground(Tex.button);
        panel.margin(panelPad);
        panel.touchable = Touchable.enabled;

        panel.add("名称").left().padRight(4f);
        nameField = new TextField(defaultName);
        nameField.setMessageText("成就名称");
        panel.add(nameField).width(panelWidth).height(fieldHeight).left().row();

        panel.add("描述").left().padRight(4f);
        descField = new TextField(defaultDesc);
        descField.setMessageText("成就描述");
        panel.add(descField).width(panelWidth).height(fieldHeight).left().row();

        panel.add("稀有度").left().padRight(4f);
        rarityField = new TextField(defaultRarity);
        rarityField.setMessageText("0/1/2/3");
        panel.add(rarityField).width(panelWidth).height(fieldHeight).left().row();

        Table buttons = new Table();
        buttons.button("发送", Icon.ok, this::send).size(120f, buttonHeight).padRight(4f);
        buttons.button("清空队列", Icon.cancel, () -> {
            if (AchievementUI.instance != null) {
                AchievementUI.instance.clearQueue();
            }
        }).size(120f, buttonHeight).row();
        buttons.button("测试蓝色", Icon.star, () -> trigger("蓝色成就", "默认稀有度0", 0)).size(120f, buttonHeight).padRight(4f);
        buttons.button("测试紫色", Icon.star, () -> trigger("紫色成就", "默认稀有度2", 2)).size(120f, buttonHeight).row();
        buttons.button("测试金色", Icon.star, () -> trigger("金色成就", "默认稀有度3", 3)).size(120f, buttonHeight).padRight(4f);
        buttons.button("批量5连", Icon.list, () -> {
            for (int i = 1; i <= 5; i++) {
                // TODO 暂时先这样
                AchievementUI.add("批量成就 #" + i, "批量成就 #" + i, Icon.star, i % 4, 1);
            }
        }).size(120f, buttonHeight).row();
        buttons.button("隐藏面板", Icon.eyeOff, AchievementTestUI::toggle).size(120f, buttonHeight).padRight(4f);
        buttons.button("清除成就存档", Icon.trash, () -> {
            AchievementRegistry.clearAll();
            if (AchievementUI.instance != null) AchievementUI.instance.clearQueue();
        }).size(120f, buttonHeight).row();

        panel.add(buttons).colspan(2).left().row();

        root.addChild(panel);
        panel.pack();
        panel.setPosition(marginLeft, marginBottom);
        installDrag(panel);
    }
    private void installDrag(Table target) {
        target.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                if (button != KeyCode.mouseLeft) return false;
                dragging = true;
                dragOffsetX = x;
                dragOffsetY = y;
                event.stop();
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if (!dragging) return;
                arc.scene.Element e = event.listenerActor;
                if (e == null) return;
                e.localToStageCoordinates(tmp.set(x, y));
                panel.setPosition(tmp.x - dragOffsetX, tmp.y - dragOffsetY);
                clampToScreen();
                event.stop();
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                dragging = false;
            }
        });
    }
    private void clampToScreen() {
        if (panel == null) return;
        float w = panel.getWidth(), h = panel.getHeight();
        float x = Mathf.clamp(panel.x, 0, Math.max(0, Core.graphics.getWidth() - w));
        float y = Mathf.clamp(panel.y, 0, Math.max(0, Core.graphics.getHeight() - h));
        panel.setPosition(x, y);
    }
    private void send() {
        String name = nameField.getText();
        String desc = descField.getText();
        int rarity = parseRarity(rarityField.getText());
        trigger(name, desc, rarity);
    }
    private void trigger(String name, String desc, int rarity) {
        // TODO
        AchievementUI.add(name, desc, Icon.star, rarity, 1, 0);
    }
    private int parseRarity(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 1;
        }
    }
}