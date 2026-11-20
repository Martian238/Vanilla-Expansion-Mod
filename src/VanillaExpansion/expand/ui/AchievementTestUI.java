package VanillaExpansion.expand.ui;

import arc.graphics.Color;
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;

//QNMD成就弹窗测试UI
public class AchievementTestUI {

    //单例
    public static AchievementTestUI instance;

    //根容器
    private Table root;

    //名称输入框
    private TextField nameField;

    //描述输入框
    private TextField descField;

    //稀有度输入框
    private TextField rarityField;

    //默认名称
    private String defaultName = "测试成就";

    //默认描述
    private String defaultDesc = "这是一个测试成就描述";

    //默认稀有度
    private String defaultRarity = "1";

    //面板宽度
    private float panelWidth = 300f;

    //面板内边距
    private float panelPad = 8f;

    //输入框高度
    private float fieldHeight = 36f;

    //按钮高度
    private float buttonHeight = 36f;

    //左边距
    private float marginLeft = 16f;

    //底边距
    private float marginBottom = 16f;

    //图标颜色提示
    private Color hintColor = Color.valueOf("aaaaaa");

    //初始化
    public static void init() {
        if (instance != null) return;
        instance = new AchievementTestUI();
        instance.build();
    }

    //销毁
    public static void dispose() {
        if (instance == null) return;
        if (instance.root != null) instance.root.remove();
        instance = null;
    }

    //构建面板
    private void build() {
        root = new Table();
        root.setFillParent(true);
        root.bottom().left();
        root.touchable = Touchable.childrenOnly;
        Vars.ui.hudGroup.addChild(root);

        Table panel = new Table();
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
                AchievementUI.add("批量成就 #" + i, "第 " + i + " 个批量测试", Icon.star, i % 4);
            }
        }).size(120f, buttonHeight).row();

        panel.add(buttons).colspan(2).left().row();
        root.add(panel).left().bottom().padLeft(marginLeft).padBottom(marginBottom);
    }

    //发送按钮逻辑
    private void send() {
        String name = nameField.getText();
        String desc = descField.getText();
        int rarity = parseRarity(rarityField.getText());
        trigger(name, desc, rarity);
    }

    //直接触发一个成就
    private void trigger(String name, String desc, int rarity) {
        AchievementUI.add(name, desc, Icon.star, rarity);
    }

    //解析稀有度
    private int parseRarity(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 1;
        }
    }
}