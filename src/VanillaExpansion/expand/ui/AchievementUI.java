package VanillaExpansion.expand.ui;

import arc.Core;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.Group;
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.gen.Tex;
import mindustry.ui.Fonts;
import java.util.ArrayDeque;
import java.util.Deque;

public class AchievementUI {
    // 单例
    public static AchievementUI instance;
    // 待显示队列
    private final Deque<Achievement> queue = new ArrayDeque<>();
    // 根容器
    private Table root;
    // 当前弹窗
    private Table current;
    // 是否正在显示
    private boolean showing = false;
    // 显示时长(秒)
    private final float showDuration = 4f;
    // 动画时长(秒)
    private final float animDuration = 0.3f;
    // 面板宽度
    private final float panelWidth = 250f;
    // 面板高度
    private final float panelHeight = 58f;
    // 右边距
    private final float marginRight = 16f;
    // 顶边距
    private final float marginTop = 16f;
    // 滑入偏移
    private final float slideOffset = 40f;
    // 背景颜色
    private final Color bgColor = Color.valueOf("1b1b1bee");
    // 稀有度颜色数组
    private final Color[] rarityColors = {
            Color.valueOf("ffffff"),
            Color.valueOf("4dff65"),
            Color.valueOf("4ea1ff"),
            Color.valueOf("a335ee"),
            Color.valueOf("ffd700"),
            Color.valueOf("ff4200")
    };
    // 当前目标X
    private float targetX = 0f;
    // 当前目标透明度
    private float targetAlpha = 1f;
    // 显示位置
    private Position position = Position.topRight;
    // 挂载的父容器
    private Group hostGroup;

    // 显示位置枚举
    public enum Position {
        // 右上角
        topRight,
        // 左上角
        topLeft,
        // 右下角
        bottomRight,
        // 左下角
        bottomLeft
    }

    // 初始化
    public static void init() {
        if (instance != null) return;
        instance = new AchievementUI();
        instance.root = new Table();
        instance.root.setFillParent(true);
        instance.root.touchable = Touchable.disabled;
        instance.ensureHost();
        instance.root.toFront();
        instance.applyPosition();
    }

    // 清空队列
    public void clearQueue() {
        queue.clear();
    }

    // 销毁
    public static void dispose() {
        if (instance == null) return;
        if (instance.root != null) instance.root.remove();
        instance = null;
    }

    // 设置显示位置
    public static void setPosition(Position pos) {
        if (instance == null) return;
        instance.position = pos;
        instance.applyPosition();
    }

    // 每帧维持层级并确保挂载在可见容器
    public static void update() {
        if (instance == null) return;
        instance.ensureHost();
        if (instance.root != null && instance.root.parent != null
                && instance.root.getZIndex() != instance.root.parent.getChildren().size - 1) {
            instance.root.toFront();
        }
        if (instance.current == null) return;
        instance.current.translation.x = Mathf.lerp(instance.current.translation.x, instance.targetX, 0.15f);
        instance.current.color.a = Mathf.lerp(instance.current.color.a, instance.targetAlpha, 0.15f);
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

    // 应用显示位置到根容器
    private void applyPosition() {
        if (root == null) return;
        root.clear();
        root.setFillParent(true);
        root.touchable = Touchable.disabled;
        switch (position) {
            case topLeft -> root.top().left();
            case bottomRight -> root.bottom().right();
            case bottomLeft -> root.bottom().left();
            default -> root.top().right();
        }
    }

    // 添加成就
    public static void add(String id, String name, Drawable icon, int maxLevel, int current) {
        add(id, name, icon, maxLevel, current);
    }

    // 添加成就
    public static void add(String id, String name, Drawable icon, int rarity, int maxLevel, int current) {
        if (instance == null) return;
        instance.addAchievement(id, name, icon, rarity, maxLevel, current);
    }

    // 内部添加
    private void addAchievement(String id, String name, Drawable icon, int rarity, int maxLevel, int current) {
        queue.addLast(new Achievement(id, name, icon, rarity, maxLevel, current));
        if (!showing) next();
    }

    // 显示下一个
    private void next() {
        Achievement a = queue.pollFirst();
        if (a == null) {
            showing = false;
            current = null;
            return;
        }
        showing = true;
        buildPanel(a);
    }

    // 构建弹窗
    private void buildPanel(Achievement a) {
        if (current != null) {
            current.remove();
            current = null;
        }
        ensureHost();
        current = new Table();
        current.setBackground(Tex.whiteui);
        current.setColor(rarityColor(a.rarity));
        current.touchable = Touchable.disabled;
        current.margin(3f);

        Table inner = new Table();
        inner.setBackground(Tex.whiteui);
        inner.setColor(bgColor);
        inner.margin(8f);

        Table content = new Table();
        content.left();

        Image icon = new Image(a.icon);
        content.add(icon).size(40f).padRight(10f);

        Table textTable = new Table();
        textTable.left();

        Label title = new Label(a.name, new Label.LabelStyle(Fonts.def, rarityColor(a.rarity)));
        title.setFontScale(1f);
        textTable.add(title).left().row();


        Label desc = new Label("", new Label.LabelStyle(Fonts.def, Color.white));
        desc.setFontScale(0.75f);
        desc.setWrap(true);
        textTable.add(desc).left().growX().row();

        content.add(textTable).left().growX();
        inner.add(content).size(panelWidth, panelHeight);
        current.add(inner);

        current.pack();
        addToRoot();
        current.toFront();

        current.translation.set(slideOffset, 0f);
        current.color.a = 0f;
        targetX = 0f;
        targetAlpha = 1f;

        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                if (current != null) {
                    targetX = 0f;
                    targetAlpha = 1f;
                }
            }
        }, 0.05f);

        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                if (current != null) {
                    targetX = slideOffset;
                    targetAlpha = 0f;
                }
            }
        }, showDuration);

        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                if (current != null) {
                    current.remove();
                    current = null;
                }
                next();
            }
        }, showDuration + animDuration);
    }

    // 根据位置把当前弹窗加入根容器
    private void addToRoot() {
        switch (position) {
            case topLeft -> root.add(current).top().left().padLeft(marginRight).padTop(marginTop);
            case bottomRight -> root.add(current).bottom().right().padRight(marginRight).padBottom(marginTop);
            case bottomLeft -> root.add(current).bottom().left().padLeft(marginRight).padBottom(marginTop);
            default -> root.add(current).top().right().padRight(marginRight).padTop(marginTop);
        }
    }

    // 稀有度颜色
    private Color rarityColor(int rarity) {
        if (rarity < 0) rarity = 0;
        if (rarity >= rarityColors.length) rarity = rarityColors.length - 1;
        return rarityColors[rarity];
    }

    // 成就数据类
    public static class Achievement {

        public String id;
        // 名称
        public String name;
        // 图标
        public Drawable icon;
        // 稀有度
        public int rarity;

        public int maxLevel;

        public int current;

        // 构造
        public Achievement(String id, String name, Drawable icon, int rarity, int maxLevel, int current) {
            this.name = name;
            this.icon = icon;
            this.rarity = rarity;
            this.maxLevel = maxLevel;
            this.current = current;
        }
    }
}