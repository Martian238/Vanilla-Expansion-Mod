package VanillaExpansion.expand.ui;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.layout.Table;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.ui.Fonts;
import mindustry.gen.Tex;

import java.util.ArrayDeque;
import java.util.Deque;
public class AchievementUI {
    public static AchievementUI instance;
    //待显示队列
    private final Deque<Achievement> queue = new ArrayDeque<>();
    //根容器
    private Table root;
    //当前弹窗
    private Table current;
    //是否正在显示
    private boolean showing = false;
    //显示时长(秒)
    private final float showDuration = 4f;
    //动画时长(秒)
    private final float animDuration = 0.3f;
    //面板宽度
    private final float panelWidth = 250f;
    //面板高度
    private final float panelHeight = 58f;
    //右边距
    private final float marginRight = 16f;
    //顶边距
    private final float marginTop = 16f;
    //滑入偏移
    private final float slideOffset = 40f;
    //背景颜色
    private final Color bgColor = Color.valueOf("1b1b1bee");
    //稀有度颜色数组
    private final Color[] rarityColors = {
        Color.valueOf("4ea1ff"),
        Color.valueOf("4ea1ff"),
        Color.valueOf("a335ee"),
        Color.valueOf("ffd700")
    };
    //当前目标X
    private float targetX = 0f;
    //当前目标透明度
    private float targetAlpha = 1f;
    //初始化
    public static void init() {
        if (instance != null) return;
        instance = new AchievementUI();
        instance.root = new Table();
        instance.root.setFillParent(true);
        instance.root.top().right();
        instance.root.touchable = Touchable.disabled;
        Vars.ui.hudGroup.addChild(instance.root);
        instance.root.toFront();
    }
    //清空队列
    public void clearQueue() {
        queue.clear();
    }
    //销毁
    public static void dispose() {
        if (instance == null) return;
        if (instance.root != null) instance.root.remove();
        instance = null;
    }
    //添加成就(名称, 描述, 图标)
    public static void add(String name, String desc, Drawable icon) {
        add(name, desc, icon, 1);
    }
    //添加成就(名称, 描述, 图标, 稀有度)
    public static void add(String name, String desc, Drawable icon, int rarity) {
        if (instance == null) return;
        instance.addAchievement(name, desc, icon, rarity);
    }
    //内部添加
    private void addAchievement(String name, String desc, Drawable icon, int rarity) {
        queue.addLast(new Achievement(name, desc, icon, rarity));
        if (!showing) next();
    }
    //显示下一个
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
    //构建弹窗
    private void buildPanel(Achievement a) {
        if (current != null) {
            current.remove();
            current = null;
        }
        current = new Table();
        current.setBackground(Tex.whiteui);
        current.setColor(rarityColor(a.rarity));
        current.touchable = Touchable.disabled;
        current.margin(3f);//边框宽

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
        title.setFontScale(0.9f);
        textTable.add(title).left().row();

        Label desc = new Label(a.desc, new Label.LabelStyle(Fonts.def, Color.white));
        desc.setFontScale(0.75f);
        desc.setWrap(true);
        textTable.add(desc).left().growX().row();

        content.add(textTable).left().growX();
        inner.add(content).size(panelWidth, panelHeight);
        current.add(inner);

        current.pack();
        root.add(current).top().right().padRight(marginRight).padTop(marginTop);
        current.toFront();

        current.translation.set(slideOffset, 0f);
        current.color.a = 0f;
        targetX = 0f;
        targetAlpha = 1f;

        //入场
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                if (current != null) {
                    targetX = 0f;
                    targetAlpha = 1f;
                }
            }
        }, 0.05f);

        //开始退场
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                if (current != null) {
                    targetX = slideOffset;
                    targetAlpha = 0f;
                }
            }
        }, showDuration);

        //清理并显示下一个
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
    //每帧更新(平滑插值)
    public static void update() {
        if (instance == null || instance.current == null) return;
        instance.current.translation.x = Mathf.lerp(instance.current.translation.x, instance.targetX, 0.15f);
        instance.current.color.a = Mathf.lerp(instance.current.color.a, instance.targetAlpha, 0.15f);
    }
    //稀有度颜色
    private Color rarityColor(int rarity) {
        if (rarity < 0) rarity = 0;
        if (rarity >= rarityColors.length) rarity = rarityColors.length - 1;
        return rarityColors[rarity];
    }
    //成就数据类
    public static class Achievement {
        //名称
        public String name;
        //描述
        public String desc;
        //图标
        public Drawable icon;
        //稀有度
        public int rarity;
        //构造
        public Achievement(String name, String desc, Drawable icon, int rarity) {
            this.name = name;
            this.desc = desc;
            this.icon = icon;
            this.rarity = rarity;
        }
    }
}