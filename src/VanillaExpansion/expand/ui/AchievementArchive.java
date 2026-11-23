package VanillaExpansion.expand.ui;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.util.Scaling;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Fonts;

public class AchievementArchive {
    // 存档前缀
    private static final String SAVE_PREFIX = "ve-achievement-";
    // 稀有度颜色
    private static final Color[] RARITY_COLORS = {
            Color.valueOf("ffffff"),
            Color.valueOf("4dff65"),
            Color.valueOf("4ea1ff"),
            Color.valueOf("a335ee"),
            Color.valueOf("ffd700"),
            Color.valueOf("ff4200")
    };
    // 面板宽度
    private static final float CARD_WIDTH = 900f;
    // 卡片高度
    private static final float CARD_HEIGHT = 90f;
    // 图标尺寸
    private static final float ICON_SIZE = 64f;

    // 注册（转发到 AchievementRegistry）
    public static void register(String id, String nameKey, String descKey, arc.scene.style.Drawable icon, int rarity, int maxLevel, String levelKey) {
        // 由 AchievementRegistry 统一管理
    }

    // 是否已解锁
    public static boolean isUnlocked(String id) {
        return getLevel(id) > 0;
    }

    public static int getLevel(String id) {
        return Core.settings.getInt(SAVE_PREFIX + id, 0);
    }

    // 已解锁数量
    public static int unlockedCount() {
        int count = 0;
        for (AchievementRegistry.Entry e : AchievementRegistry.all()) if (isUnlocked(e.id)) count++;
        return count;
    }

    // 总数
    public static int totalCount() {
        return AchievementRegistry.all().size();
    }

    // 构建档案馆内容到指定表
    public static void buildInto(Table target) {
        target.clear();
        target.top();

        Table header = new Table();
        header.left();
        Label title = new Label(Core.bundle.get("ve-archive-title", "[stat]Achievement Archive[]"));
        title.setColor(Color.white);
        title.setFontScale(1.2f);
        header.add(title).left().row();
        Label progress = new Label(Core.bundle.format("ve-archive-progress", unlockedCount(), totalCount()));
        progress.setColor(Color.lightGray);
        progress.setFontScale(0.9f);
        header.add(progress).left().padTop(6f).row();
        target.add(header).growX().padBottom(16f).row();

        ScrollPane pane = new ScrollPane(buildList());
        pane.setFadeScrollBars(false);
        pane.setScrollingDisabled(true, false);
        pane.setOverscroll(false, false);
        target.add(pane).grow().row();
    }

    // 构建成就列表
    private static Table buildList() {
        Table list = new Table();
        list.top();
        list.defaults().growX().padBottom(8f);

        String levelFin = Core.bundle.get("ve-archive-complete", "(COMPLETE)");

        for (AchievementRegistry.Entry e : AchievementRegistry.all()) {
            boolean unlocked = isUnlocked(e.id);
            Table card = new Table();
            card.setBackground(Tex.whiteui);
            Color colorUsed = unlocked ? RARITY_COLORS[Math.max(0, Math.min(e.rarity, RARITY_COLORS.length - 1))] : Color.valueOf("333333");
            card.setColor(colorUsed);
            card.margin(4f);

            Table inner = new Table();
            inner.setBackground(Tex.whiteui);
            inner.setColor(Color.valueOf("1b1b1bee"));
            inner.margin(8f);
            inner.left();

            Image icon = new Image(e.icon);
            icon.setScaling(Scaling.fit);
            if (!unlocked) icon.setColor(Color.valueOf("000000"));
            inner.add(icon).size(ICON_SIZE).padRight(10f);

            Table text = new Table();
            text.left();
            Label name = new Label(Core.bundle.get(e.nameKey, e.nameKey),
                    new Label.LabelStyle(Fonts.def, unlocked ? colorUsed : Color.lightGray));
            name.setFontScale(1f);
            text.add(name).left().row();
            Label desc = new Label(Core.bundle.get(e.descKey, e.descKey),
                    new Label.LabelStyle(Fonts.def, Color.lightGray));
            desc.setFontScale(1f);
            desc.setWrap(true);
            text.add(desc).left().growX().row();

            if(e.maxLevel > 1){
                int current = getLevel(e.id);
                for(int i = 0; i < e.maxLevel; i++){
                    int num = i + 1;
                    String suf = current >= num ? " " + levelFin : "";
                    String pre = Core.bundle.format("ve-archive-level", num) + " ";
                    Label level = new Label(pre + Core.bundle.get(e.levelKey + num, e.levelKey + num) + suf,
                            new Label.LabelStyle(Fonts.def, current >= num ? Pal.accent : Color.lightGray));
                    desc.setFontScale(1f);
                    desc.setWrap(true);
                    text.add(level).left().growX().row();
                }
            }else{
                String suf = unlocked ? " " + levelFin : "";
                Label level = new Label(Core.bundle.get(e.levelKey , e.levelKey) + suf,
                        new Label.LabelStyle(Fonts.def, unlocked ? Pal.accent : Color.lightGray));
                desc.setFontScale(1f);
                desc.setWrap(true);
                text.add(level).left().growX().row();
            }

            inner.add(text).left().growX();

            card.add(inner).growX().height(CARD_HEIGHT * (1 + 0.2f * (e.maxLevel - 1)));
            list.add(card).width(CARD_WIDTH).row();
        }
        return list;
    }
}